package datastoreguard

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class DataStoreSingletonCheckTask : DefaultTask() {
    private data class Violation(
        val file: String,
        val line: Int,
        val methodName: String,
        val fix: String,
    )

    /**
     * Method names on `@Provides` functions that MUST have a singleton scope annotation.
     * These methods return instances that crash if created more than once
     * (DataStore: IllegalStateException, Room: database locking issues).
     */
    private val guardedMethods = setOf(
        "providePreferencesDataStore",
        "provideAppDatabase",
    )

    /**
     * Scope annotations that satisfy the singleton requirement.
     * Each DI component defines its own scope annotation.
     */
    private val scopeAnnotations = setOf(
        "@Singleton",
        "@SharedSingleton",
        "@ExperimentalSingleton",
    )

    /**
     * Files that are intentionally exempt from the scope requirement.
     * DataComponent.kt is a base interface — overrides in concrete components add the scope.
     */
    private val exemptFiles = setOf(
        "DataComponent.kt",
    )

    private val providesPattern = Regex("""@Provides""")

    @TaskAction
    fun check() {
        val rootDir = project.rootDir
        val violations = mutableListOf<Violation>()

        // Scan all Kotlin source files across the project
        rootDir
            .walk()
            .filter { it.extension == "kt" }
            .filter { !it.path.contains("/build/") }
            .filter { !it.path.contains("/test/") && !it.path.contains("/androidTest/") }
            .filter { it.name !in exemptFiles }
            .forEach { file ->
                val lines = file.readLines()
                val relativePath = file.relativeTo(rootDir).path

                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    // Check if this line defines a guarded method
                    val matchedMethod = guardedMethods.firstOrNull { methodName ->
                        trimmed.contains("fun $methodName(")
                    } ?: return@forEachIndexed

                    // Look for @Provides in a nearby preceding line (within 5 lines)
                    val lookbackStart = maxOf(0, index - 5)
                    val precedingLines = lines.subList(lookbackStart, index + 1)
                    val hasProvidesAnnotation = precedingLines.any { providesPattern.containsMatchIn(it) }

                    // Only check methods that are @Provides (skip plain interface declarations)
                    if (!hasProvidesAnnotation) return@forEachIndexed

                    // Check if a scope annotation exists on the method or a preceding line
                    val hasScopeAnnotation = precedingLines.any { precedingLine ->
                        scopeAnnotations.any { annotation -> precedingLine.trim().contains(annotation) }
                    }

                    if (!hasScopeAnnotation) {
                        violations.add(
                            Violation(
                                file = relativePath,
                                line = index + 1,
                                methodName = matchedMethod,
                                fix = "Add a scope annotation " +
                                    "(${scopeAnnotations.joinToString(" or ")}) " +
                                    "to $matchedMethod(). " +
                                    "DataStore and Room instances crash if created more than once.",
                            ),
                        )
                    }
                }
            }

        if (violations.isNotEmpty()) {
            val msg =
                violations.joinToString("\n") { v ->
                    "  ${v.file}:${v.line} [datastore-singleton-guard] " +
                        "${v.methodName}() is missing a singleton scope annotation\n" +
                        "    Fix: ${v.fix}"
                }
            throw GradleException(
                "DataStore Singleton Check Failed (${violations.size} violation(s)):\n$msg",
            )
        }

        println("DataStore Singleton Check Passed!")
    }
}
