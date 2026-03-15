package importboundary

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File

open class ImportBoundaryCheckTask : DefaultTask() {
    private data class Violation(
        val file: String,
        val line: Int,
        val rule: String,
        val text: String,
        val fix: String,
    )

    private data class BoundaryRule(
        val moduleDir: String,
        val forbiddenPackages: List<String>,
        val fixGuidance: String,
    )

    private val basePackage = "com.chriscartland.batterybutler"

    private val rules =
        listOf(
            BoundaryRule(
                moduleDir = "presentation-feature",
                forbiddenPackages =
                    listOf(
                        ".domain.repository.",
                        ".usecase.",
                        ".data.",
                        ".ai.",
                    ),
                fixGuidance = "Access data through a ViewModel, not directly from the feature layer",
            ),
            BoundaryRule(
                moduleDir = "presentation-core",
                forbiddenPackages =
                    listOf(
                        ".data.",
                        ".usecase.",
                        ".viewmodel.",
                    ),
                fixGuidance =
                    "presentation-core is a lower UI layer — move business logic access to presentation-feature",
            ),
            BoundaryRule(
                moduleDir = "usecase",
                forbiddenPackages =
                    listOf(
                        ".data.",
                        ".viewmodel.",
                    ),
                fixGuidance = "UseCases should depend on domain interfaces only, not data or viewmodel",
            ),
            BoundaryRule(
                moduleDir = "domain",
                forbiddenPackages =
                    listOf(
                        ".data.",
                        ".presentation.",
                    ),
                fixGuidance = "Domain is the innermost layer — it must not depend on outer layers",
            ),
        )

    private val importPattern = Regex("""^import\s+([\w.]+)""")
    private val exemptPattern = Regex("""//\s*@ImportBoundaryExempt:""")

    @TaskAction
    fun check() {
        val rootDir = project.rootDir
        val violations = mutableListOf<Violation>()

        for (rule in rules) {
            val moduleDir = File(rootDir, rule.moduleDir)
            val srcDir = File(moduleDir, "src")
            if (!srcDir.exists()) {
                println("Import Boundary Check: ${rule.moduleDir}/src not found, skipping.")
                continue
            }

            // Find all source set dirs under src/, excluding test source sets
            val sourceSetDirs =
                srcDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

            for (sourceSetDir in sourceSetDirs) {
                val sourceSetName = sourceSetDir.name
                if (sourceSetName.contains("test", ignoreCase = true)) {
                    continue
                }

                val kotlinDir = File(sourceSetDir, "kotlin")
                if (!kotlinDir.exists()) continue

                kotlinDir
                    .walk()
                    .filter { it.extension == "kt" }
                    .forEach { file ->
                        val relativePath = file.relativeTo(rootDir).path
                        file.readLines().forEachIndexed { index, line ->
                            if (exemptPattern.containsMatchIn(line)) return@forEachIndexed

                            val match = importPattern.find(line) ?: return@forEachIndexed
                            val importedPackage = match.groupValues[1]

                            if (!importedPackage.startsWith(basePackage)) return@forEachIndexed

                            val suffix = importedPackage.removePrefix(basePackage)
                            for (forbidden in rule.forbiddenPackages) {
                                if (suffix.startsWith(forbidden)) {
                                    violations.add(
                                        Violation(
                                            file = relativePath,
                                            line = index + 1,
                                            rule = "import-boundary",
                                            text =
                                                "Forbidden import from '$forbidden' in module '${rule.moduleDir}'",
                                            fix = rule.fixGuidance,
                                        ),
                                    )
                                }
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
                "Import Boundary Check Failed (${violations.size} violation(s)):\n$msg",
            )
        }

        println("Import Boundary Check Passed!")
    }
}
