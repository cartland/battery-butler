package themelayer

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File

open class ThemeLayerCheckTask : DefaultTask() {
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
                "no-raw-color-literal",
                Regex("""Color\(0x"""),
                "Raw Color literals must be defined in presentation-core/theme/",
                "Use MaterialTheme.colorScheme.* or LocalButlerColors.current instead",
            ),
            Rule(
                "no-dark-theme-check",
                Regex("""isSystemInDarkTheme\(\)"""),
                "Theme detection must happen in presentation-core",
                "Use DeviceIconMapper.getResolvedIconAccent() or LocalButlerColors.current instead",
            ),
            Rule(
                "no-color-import",
                Regex("""import\s+androidx\.compose\.ui\.graphics\.Color"""),
                "Import Color only in presentation-core/theme/",
                "Access colors through MaterialTheme.colorScheme.* or LocalButlerColors.current",
            ),
        )

    private val scanDirs = listOf(
        "presentation-feature/src",
        "compose-app/src",
    )

    @TaskAction
    fun check() {
        val rootDir = project.rootDir
        val violations = mutableListOf<Violation>()

        for (dirPath in scanDirs) {
            val srcDir = File(rootDir, dirPath)
            if (!srcDir.exists()) {
                println("Theme Layer Check: $dirPath not found, skipping.")
                continue
            }

            srcDir
                .walk()
                .filter { it.extension == "kt" }
                .filter { !it.path.contains("/test/") && !it.path.contains("/androidTest/") }
                .forEach { file ->
                    val relativePath = file.relativeTo(rootDir).path
                    file.readLines().forEachIndexed { index, line ->
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
        }

        if (violations.isNotEmpty()) {
            val msg =
                violations.joinToString("\n") { v ->
                    "  ${v.file}:${v.line} [${v.rule}] ${v.text}\n    Fix: ${v.fix}"
                }
            throw GradleException(
                "Theme Layer Check Failed (${violations.size} violation(s)):\n$msg",
            )
        }

        println("Theme Layer Check Passed!")
    }
}
