package kmpinterop

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Fails when Swift code constructs a Kotlin state class without passing every constructor
 * parameter.
 *
 * WHY THIS EXISTS
 *
 * Kotlin/Native exposes a data class as a single Objective-C *designated initializer* taking all
 * parameters. Kotlin default arguments do NOT cross the boundary: adding a defaulted parameter to
 *
 *     data class Success(val items: List<Item>)                      // init(items:)
 *     data class Success(val items: List<Item>, val d: D = D.EXPANDED) // init(items:densityOption:)
 *
 * is source-compatible for Kotlin and a COMPILE ERROR for every existing Swift call site. SKIE's
 * DefaultArgumentInterop does not rescue this: verified against the generated framework, these
 * constructors get exactly one initializer requiring all parameters and no defaulted overloads.
 *
 * That break is invisible to pull-request CI, which skips the iOS jobs in development mode, so it
 * lands on `main`. It happened twice in one day while making the list-density preference app-wide
 * (`DeviceTypeListScreenState.Success`, then `HistoryListScreenState.Success`).
 *
 * This check reproduces the failure on a plain JVM runner in milliseconds rather than waiting on a
 * macOS runner and a full Xcode build, so it runs on every PR.
 *
 * SCOPE / LIMITS
 *
 * Deliberately conservative -- it only reports a call site when it can see BOTH the Kotlin
 * declaration and the Swift construction, and only for classes whose Swift name it can resolve
 * unambiguously. It is a safety net for the common case, not a Swift parser. It will not catch
 * every interop break; `validation_ios_ui` remains the ground truth.
 */
open class SwiftConstructorCheckTask : DefaultTask() {
    private data class KotlinClass(
        val swiftName: String,
        val parameters: List<String>,
        val declaredIn: String,
    )

    private data class Violation(
        val swiftFile: String,
        val line: Int,
        val swiftName: String,
        val missing: List<String>,
        val declaredIn: String,
    )

    // Modules whose types are exported through the Kotlin/Native framework and constructed from
    // Swift. Add a module here when Swift starts building its types directly.
    private val kotlinSourceDirs = listOf("presentation-model/src/commonMain/kotlin")

    private val swiftSourceDirs = listOf("ios-app-swift-ui")

    @TaskAction
    fun check() {
        val rootDir = project.rootDir

        val classes = kotlinSourceDirs
            .map { File(rootDir, it) }
            .filter { it.isDirectory }
            .flatMap { dir -> dir.walkTopDown().filter { it.extension == "kt" }.toList() }
            .flatMap { parseKotlinFile(it, rootDir) }
            .associateBy { it.swiftName }

        if (classes.isEmpty()) {
            logger.lifecycle("Swift constructor check: no exported Kotlin classes found; nothing to verify.")
            return
        }

        val swiftFiles = swiftSourceDirs
            .map { File(rootDir, it) }
            .filter { it.isDirectory }
            .flatMap { dir -> dir.walkTopDown().filter { it.extension == "swift" }.toList() }

        val violations = swiftFiles.flatMap { checkSwiftFile(it, classes, rootDir) }

        if (violations.isNotEmpty()) {
            val report = buildString {
                appendLine("Swift constructs Kotlin types without passing every parameter.")
                appendLine()
                appendLine("Kotlin/Native generates ONE initializer per class taking all parameters.")
                appendLine("Kotlin default arguments do not cross into Swift, so adding a defaulted")
                appendLine("parameter breaks existing Swift call sites even though Kotlin still compiles.")
                appendLine()
                violations.forEach { v ->
                    appendLine("  ${v.swiftFile}:${v.line}")
                    appendLine("    ${v.swiftName}(...) is missing: ${v.missing.joinToString(", ")}")
                    appendLine("    declared in ${v.declaredIn}")
                    appendLine()
                }
                appendLine("Fix: pass the missing argument(s) at each Swift call site.")
            }
            throw GradleException(report)
        }

        logger.lifecycle(
            "Swift constructor check: ${classes.size} exported class(es), " +
                "${swiftFiles.size} Swift file(s), no violations.",
        )
    }

    /**
     * Extract constructor parameter names for data classes.
     *
     * Nested declarations are flattened the way Kotlin/Native names them: `HistoryListScreenState`
     * containing `data class Success` is exported to Swift as `HistoryListScreenStateSuccess`.
     */
    private fun parseKotlinFile(
        file: File,
        rootDir: File,
    ): List<KotlinClass> {
        val text = file.readText()
        val relativePath = file.relativeTo(rootDir).path
        val result = mutableListOf<KotlinClass>()

        // Outer declaration: the sealed interface / class the file's nested types hang off.
        val outerName = Regex("""^(?:sealed\s+)?(?:interface|class)\s+(\w+)""", RegexOption.MULTILINE)
            .find(text)
            ?.groupValues
            ?.get(1)

        Regex("""(\s*)data class\s+(\w+)\s*\(""").findAll(text).forEach { match ->
            val indent = match.groupValues[1].substringAfterLast('\n')
            val simpleName = match.groupValues[2]
            val params = parseParameterNames(text, match.range.last) ?: return@forEach

            // Indented => nested inside the outer declaration => flattened name in Swift.
            val swiftName = if (indent.isNotEmpty() && outerName != null && simpleName != outerName) {
                "$outerName$simpleName"
            } else {
                simpleName
            }
            result += KotlinClass(swiftName, params, relativePath)
        }
        return result
    }

    /** Read the balanced parameter list starting at the opening paren and return parameter names. */
    private fun parseParameterNames(
        text: String,
        openParenIndex: Int,
    ): List<String>? {
        var depth = 0
        var i = openParenIndex
        val body = StringBuilder()
        while (i < text.length) {
            when (val c = text[i]) {
                '(' -> {
                    depth++
                    if (depth > 1) body.append(c)
                }

                ')' -> {
                    depth--
                    if (depth == 0) {
                        return splitTopLevel(body.toString())
                            .mapNotNull { param ->
                                Regex("""\bval\s+(\w+)\s*:""").find(param)?.groupValues?.get(1)
                            }.takeIf { it.isNotEmpty() }
                    }
                    body.append(c)
                }

                else -> {
                    if (depth >= 1) body.append(c)
                }
            }
            i++
        }
        return null
    }

    /** Split on commas that are not nested inside parentheses, angle brackets, or generics. */
    private fun splitTopLevel(s: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        s.forEach { c ->
            when (c) {
                '(', '<', '[' -> {
                    depth++
                    current.append(c)
                }

                ')', '>', ']' -> {
                    depth--
                    current.append(c)
                }

                ',' -> {
                    if (depth == 0) {
                        parts += current.toString()
                        current.clear()
                    } else {
                        current.append(c)
                    }
                }

                else -> {
                    current.append(c)
                }
            }
        }
        if (current.isNotBlank()) parts += current.toString()
        return parts
    }

    private fun checkSwiftFile(
        file: File,
        classes: Map<String, KotlinClass>,
        rootDir: File,
    ): List<Violation> {
        val text = file.readText()
        val relativePath = file.relativeTo(rootDir).path
        val violations = mutableListOf<Violation>()

        classes.values.forEach { kotlinClass ->
            Regex("""\b${Regex.escape(kotlinClass.swiftName)}\s*\(""").findAll(text).forEach { match ->
                val args = readSwiftCallArguments(text, match.range.last) ?: return@forEach
                // A bare `Type()` is a legitimate no-arg construction only when there are no
                // parameters; otherwise every parameter must appear as a Swift argument label.
                val missing = kotlinClass.parameters.filterNot { it in args }
                if (missing.isNotEmpty()) {
                    violations += Violation(
                        swiftFile = relativePath,
                        line = text.take(match.range.first).count { it == '\n' } + 1,
                        swiftName = kotlinClass.swiftName,
                        missing = missing,
                        declaredIn = kotlinClass.declaredIn,
                    )
                }
            }
        }
        return violations
    }

    /** Collect the argument labels of a Swift call, e.g. `f(a: 1, b: 2)` -> ["a", "b"]. */
    private fun readSwiftCallArguments(
        text: String,
        openParenIndex: Int,
    ): List<String>? {
        var depth = 0
        var i = openParenIndex
        val body = StringBuilder()
        while (i < text.length) {
            when (val c = text[i]) {
                '(', '[' -> {
                    depth++
                    if (depth > 1) body.append(c)
                }

                ')', ']' -> {
                    depth--
                    if (depth == 0) {
                        return splitTopLevel(body.toString())
                            .mapNotNull { Regex("""^\s*(\w+)\s*:""").find(it)?.groupValues?.get(1) }
                    }
                    body.append(c)
                }

                else -> {
                    if (depth >= 1) body.append(c)
                }
            }
            i++
        }
        return null
    }
}
