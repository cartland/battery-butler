package com.chriscartland.batterybutler.viewmodel

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Convention test: every EXPOSED (non-private) StateFlow/MutableStateFlow property on a
 * ViewModel must be backed by the OBSERVABLE flow factory from KMP-ObservableViewModel
 * (`com.rickclephas.kmp.observableviewmodel.MutableStateFlow(viewModelScope, …)` or the
 * project `safeStateIn`/`retryableStateIn`/observable `stateIn` helpers), NOT the plain
 * `kotlinx.coroutines.flow.MutableStateFlow(…)`.
 *
 * WHY: state exposed to the native SwiftUI app via `@StateViewModel`/`@ObservedViewModel`
 * only triggers SwiftUI re-renders when the StateFlow was created through the observable
 * factory (which registers it with the ViewModel's `objectWillChange` publisher). A plain
 * `MutableStateFlow` exposed to Swift compiles fine but SILENTLY never updates the UI — and
 * dev-mode PR CI skips the iOS build, so the bug only surfaces post-merge. This test turns
 * that silent footgun into a build failure.
 *
 * Private plain `MutableStateFlow` is ALLOWED (e.g. internal "funnel" flows that combine
 * into an observable `uiState`); only EXPOSED state must be observable.
 *
 * Scope/limitation: this targets the plain-`MutableStateFlow` footgun specifically. A flow
 * forwarded straight from a use case (e.g. `val x = someUseCase()`) is out of scope — that
 * is a deliberate, separate decision (see CounterViewModel.appCounterRunning). To
 * intentionally exempt a property declaration, append `// observable-exempt` to its line.
 */
class ExposedStateObservabilityConventionTest {
    @Test
    fun `exposed ViewModel StateFlow properties use the observable factory`() {
        val srcDirs = listOf(
            File(repoRoot(), "viewmodel/src/commonMain/kotlin"),
            File(repoRoot(), "experimental/viewmodel/src/commonMain/kotlin"),
        )
        srcDirs.forEach { dir ->
            assertTrue(dir.isDirectory, "Expected ViewModel source directory at ${dir.absolutePath}")
        }

        val viewModelFiles = srcDirs.flatMap { dir ->
            dir.walkTopDown().filter { it.isFile && it.name.endsWith("ViewModel.kt") }.toList()
        }
        assertTrue(
            viewModelFiles.isNotEmpty(),
            "Found no *ViewModel.kt sources to scan under $srcDirs.",
        )

        val violations = viewModelFiles.flatMap { scanFile(it) }
        if (violations.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("Exposed ViewModel state is built with the PLAIN kotlinx MutableStateFlow.")
                    appendLine("SwiftUI (@StateViewModel) will SILENTLY NOT re-render when it changes.")
                    appendLine("Fix: build it with the observable factory")
                    appendLine("  com.rickclephas.kmp.observableviewmodel.MutableStateFlow(viewModelScope, initial)")
                    appendLine("or expose it via safeStateIn/retryableStateIn(viewModelScope, …).")
                    appendLine("(Private funnel flows may stay plain; only exposed state must be observable.)")
                    appendLine()
                    appendLine("Violations:")
                    violations.forEach { appendLine("  - $it") }
                },
            )
        }
    }

    private data class Violation(
        val file: String,
        val line: Int,
        val property: String,
        val detail: String,
    ) {
        override fun toString(): String = "$file:$line  `$property`  ($detail)"
    }

    private fun scanFile(file: File): List<Violation> {
        val lines = file.readLines()

        // Pass 1: classify every MutableStateFlow-backed property as observable or plain.
        val plainBackings = mutableSetOf<String>()
        val observableBackings = mutableSetOf<String>()
        lines.forEach { line ->
            val decl = parseDecl(line) ?: return@forEach
            when (mutableStateFlowIsObservable(decl.initializer ?: "")) {
                true -> observableBackings += decl.name
                false -> plainBackings += decl.name
                null -> Unit // not a MutableStateFlow construction
            }
        }

        // Pass 2: flag exposed properties that resolve to a plain MutableStateFlow.
        val violations = mutableListOf<Violation>()
        lines.forEachIndexed { index, line ->
            if (line.contains("// observable-exempt")) return@forEachIndexed
            val decl = parseDecl(line) ?: return@forEachIndexed
            if (decl.isPrivate) return@forEachIndexed
            val initializer = decl.initializer ?: return@forEachIndexed

            // (a) exposed property constructs a plain MutableStateFlow directly
            if (mutableStateFlowIsObservable(initializer) == false) {
                violations += Violation(
                    file.relativeName(),
                    index + 1,
                    decl.name,
                    "exposed plain MutableStateFlow(...)",
                )
                return@forEachIndexed
            }
            // (b) exposed property forwards a plain backing field
            val ref = simpleReference(initializer)
            if (ref != null && ref in plainBackings && ref !in observableBackings) {
                violations += Violation(
                    file.relativeName(),
                    index + 1,
                    decl.name,
                    "exposes plain backing `$ref`",
                )
            }
        }
        return violations
    }

    private data class Decl(
        val name: String,
        val isPrivate: Boolean,
        val initializer: String?,
    )

    /**
     * Parses a member property declaration (`[modifiers] val|var name[: Type] [= initializer]`).
     * Returns null for lines that are not member declarations (comments, `when (val x = …)`,
     * string literals, etc.) by requiring everything before the `val`/`var` keyword to be
     * only modifiers/annotations/whitespace.
     */
    private fun parseDecl(line: String): Decl? {
        val keyword = DECL_KEYWORD.find(line) ?: return null
        val prefix = line.substring(0, keyword.range.first)
        if (!prefix.matches(MODIFIER_PREFIX)) return null
        val name = keyword.groupValues[2]
        val rest = line.substring(keyword.range.last + 1)
        val initializer = ASSIGNMENT
            .find(rest)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.ifBlank { null }
        return Decl(name = name, isPrivate = PRIVATE.containsMatchIn(prefix), initializer = initializer)
    }

    /**
     * Returns true if [text] contains a `MutableStateFlow(...)` construction (including an
     * aliased name like `ObservableMutableStateFlow(...)`) whose first argument is
     * `viewModelScope` (the observable factory), false if it is a plain construction, and
     * null if there is no MutableStateFlow construction.
     */
    private fun mutableStateFlowIsObservable(text: String): Boolean? {
        val match = MSF_CONSTRUCTION.find(text) ?: return null
        return match.groupValues[1] == "viewModelScope"
    }

    private fun simpleReference(text: String): String? = SIMPLE_REF.matchEntire(text)?.groupValues?.get(1)

    private fun File.relativeName(): String = repoRoot().toPath().relativize(toPath()).toString()

    /**
     * Resolves the repository root (the directory containing settings.gradle.kts) starting
     * from the test's working directory, so the scan works regardless of which module Gradle
     * runs it from.
     */
    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        return dir ?: error(
            "Could not locate repo root (settings.gradle.kts) from ${System.getProperty("user.dir")}",
        )
    }

    private companion object {
        val DECL_KEYWORD = Regex("""\b(val|var)\s+(\w+)""")
        val MODIFIER_PREFIX = Regex(
            """\s*(?:@\w+(?:\([^)]*\))?\s*|""" +
                """(?:private|internal|protected|public|override|open|final|lateinit|const|abstract)\s+)*""",
        )
        val PRIVATE = Regex("""\bprivate\b""")
        val ASSIGNMENT = Regex("""^\s*(?::\s*[^=]+?)?\s*=\s*(.*)$""")

        // Greedy `<.*>` skips the (possibly nested) generic; first constructor argument captured.
        val MSF_CONSTRUCTION = Regex("""\w*MutableStateFlow\s*(?:<.*>)?\s*\(\s*([A-Za-z_][\w.]*)""")
        val SIMPLE_REF = Regex("""(\w+)(?:\.asStateFlow\(\))?""")
    }
}
