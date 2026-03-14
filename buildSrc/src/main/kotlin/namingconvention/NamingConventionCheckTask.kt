package namingconvention

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File

open class NamingConventionCheckTask : DefaultTask() {
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
                "no-ui-in-class-name",
                Regex("""(class|interface|object)\s+\w*[a-z]Ui[A-Z]\w*"""),
                "Class names must not contain 'Ui' — collides with iOS UIKit naming",
                "Replace 'Ui' with 'Screen' (for states) or remove it (for models). " +
                    "Example: HomeUiState -> HomeScreenState, ChatUiMessage -> ChatMessage",
            ),
            Rule(
                "no-view-in-class-name",
                Regex("""(class|interface|object)\s+\w*View(?!Model)\w*"""),
                "Class names must not contain 'View' (except ViewModel) — ambiguous between Android View system and Compose",
                "Use a more specific name. Example: ViewState -> ScreenState, ViewData -> DisplayData",
            ),
        )

    private val suppressionMarker = "@NamingExempt"

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
                "Naming Convention Check Failed (${violations.size} violation(s)):\n$msg",
            )
        }

        println("Naming Convention Check Passed!")
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
                    !path.contains("/desktopTest/") &&
                    !path.contains("/jvmTest/") &&
                    !path.contains("/screenshotTest/") &&
                    !path.contains("/build/")
            }.forEach { file ->
                val relativePath = file.relativeTo(rootDir).path
                file.readLines().forEachIndexed { index, line ->
                    if (line.contains(suppressionMarker)) return@forEachIndexed
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
