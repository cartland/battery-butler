package stringresource

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File

open class HardcodedStringCheckTask : DefaultTask() {
    private data class Violation(
        val file: String,
        val line: Int,
        val rule: String,
        val text: String,
        val fix: String,
    )

    private data class Rule(
        val id: String,
        val pattern: Regex,
        val message: String,
        val fix: String,
    )

    private val rules =
        listOf(
            Rule(
                "no-hardcoded-text",
                Regex("""Text\(\s*(?:text\s*=\s*)?"[^"]*"""),
                "Hardcoded text in Text() composable — use composeStringResource(Res.string.xxx) instead",
                "Replace with Text(composeStringResource(Res.string.your_string_id))",
            ),
            Rule(
                "no-hardcoded-content-description",
                Regex("""contentDescription\s*=\s*"[^"]*""""),
                "Hardcoded contentDescription — use composeStringResource(Res.string.xxx) instead",
                "Replace with contentDescription = composeStringResource(Res.string.your_content_desc_id)",
            ),
        )

    private val suppressionMarker = "@StringResourceExempt"

    @TaskAction
    fun check() {
        val rootDir = project.rootDir
        val violations = mutableListOf<Violation>()

        val moduleDirs =
            rootDir.listFiles()?.filter { dir ->
                dir.isDirectory &&
                    dir.name != "buildSrc" &&
                    dir.name != "build" &&
                    !dir.name.startsWith(".")
            } ?: emptyList()

        for (moduleDir in moduleDirs) {
            scanDirectory(moduleDir, rootDir, violations)
        }

        if (violations.isNotEmpty()) {
            val msg =
                violations.joinToString("\n") { v ->
                    "  ${v.file}:${v.line} [${v.rule}] ${v.text}\n    Fix: ${v.fix}"
                }
            throw GradleException(
                "Hardcoded String Check Failed (${violations.size} violation(s)):\n$msg",
            )
        }

        println("Hardcoded String Check Passed!")
    }

    private fun scanDirectory(
        dir: File,
        rootDir: File,
        violations: MutableList<Violation>,
    ) {
        val srcDir = File(dir, "src")
        if (!srcDir.exists()) return

        srcDir
            .walk()
            .filter { it.extension == "kt" }
            .filter { file ->
                val path = file.path
                !path.contains("/test/") &&
                    !path.contains("/androidTest/") &&
                    !path.contains("/androidInstrumentedTest/") &&
                    !path.contains("/desktopTest/") &&
                    !path.contains("/jvmTest/") &&
                    !path.contains("/screenshotTest/") &&
                    !path.contains("/build/") &&
                    !path.contains("/experimental/")
            }.forEach { file ->
                val relativePath = file.relativeTo(rootDir).path
                var inPreviewFunction = false
                var previewBraceDepth = 0
                var pendingPreview = false

                file.readLines().forEachIndexed { index, line ->
                    if (line.contains("@Preview")) {
                        pendingPreview = true
                        return@forEachIndexed
                    }
                    if (pendingPreview && line.contains("fun ")) {
                        inPreviewFunction = true
                        previewBraceDepth = 0
                        pendingPreview = false
                    }
                    if (inPreviewFunction) {
                        previewBraceDepth += line.count { it == '{' }
                        previewBraceDepth -= line.count { it == '}' }
                        if (previewBraceDepth <= 0 && line.contains("}")) {
                            inPreviewFunction = false
                        }
                        return@forEachIndexed
                    }

                    if (line.contains(suppressionMarker)) return@forEachIndexed

                    val trimmed = line.trimStart()
                    if (trimmed.startsWith("//") || trimmed.startsWith("*")) return@forEachIndexed

                    for (rule in rules) {
                        if (rule.pattern.containsMatchIn(line)) {
                            violations.add(
                                Violation(
                                    file = relativePath,
                                    line = index + 1,
                                    rule = rule.id,
                                    text = rule.message,
                                    fix = rule.fix,
                                ),
                            )
                        }
                    }
                }
            }

        // Recurse into nested module dirs (e.g., experimental/compose-app)
        dir.listFiles()?.filter { it.isDirectory && File(it, "src").exists() && it.name != "build" }?.forEach {
            scanDirectory(it, rootDir, violations)
        }
    }
}
