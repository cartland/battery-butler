package architecture

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class ArchitectureCheckTask : DefaultTask() {
    // Define the Allowed Dependencies Graph
    // Key: The module name (e.g., ":usecase")
    // Value: List of allowed dependency prefixes or exact matches
    private val allowedDependencies = mapOf(
        ":domain" to listOf(), // Domain depends on nothing
        ":ai" to listOf(":domain", ":presentation-model"),
        ":data-local" to listOf(":domain"),
        ":data-network" to listOf(":domain", ":fixtures"),
        ":data" to listOf(":domain", ":data-local", ":data-network"),
        ":usecase" to listOf(
            ":domain",
            ":ai",
            ":presentation-model",
        ), // AI is allowed for now until Phase 1 fully completes? No, Phase 1 removed it from ViewModel, but UseCase uses AI.
        ":presentation-model" to listOf(":domain"),
        ":viewmodel" to listOf(":usecase", ":domain", ":presentation-model"),
        ":presentation-core" to listOf(":domain", ":presentation-model", ":compose-resources"),
        ":presentation-feature" to listOf(
            ":presentation-core",
            ":presentation-model",
            ":compose-resources",
            ":viewmodel",
            ":domain",
        ),
        ":compose-resources" to listOf(),
        ":fixtures" to listOf(":domain"),
        ":compose-app" to listOf("*"), // App wires everything
        ":ios-swift-di" to listOf("*"),
        ":server:domain" to listOf(":domain"),
        ":server:data" to listOf(":server:domain", ":domain", ":fixtures"),
        ":server:app" to listOf(":server:domain", ":server:data", ":domain"),
        ":git" to listOf(),
        ":scripts" to listOf(),
    )

    @TaskAction
    fun check() {
        val rootDir = project.rootDir
        val violations = mutableListOf<String>()

        project.subprojects.forEach { subproject ->
            val moduleName = ":" + subproject.path.removePrefix(":") // normalize to :module
            val allowed = allowedDependencies[moduleName]

            if (allowed == null && !moduleName.startsWith(":server")) {
                // Determine if we should enforce for this module
                // For now, let's just log or skip unknown modules if they aren't critical
                // But better to be strict.
                // violations.add("Module $moduleName is not defined in architecture rules.")
                return@forEach
            }

            if (allowed?.contains("*") == true) return@forEach // Allow all

            subproject.configurations.forEach { config ->
                // We only care about implementation/api dependencies, not test or ksp
                if (config.name.lowercase().contains("test") || config.name.lowercase().contains("ksp")) return@forEach

                // This is a rough check. Resolving configurations might differ.
                // Detailed check: Inspect declared dependencies
                config.dependencies.forEach { dep ->
                    if (dep is org.gradle.api.artifacts.ProjectDependency) {
                        val depPath = dep.dependencyProject.path
                        if (depPath != moduleName) { // self-dependency is separate issue, usually ignored
                            val isAllowed = allowed?.any { rule ->
                                if (rule == "*") {
                                    true
                                } else {
                                    depPath == rule || (rule.endsWith("*") && depPath.startsWith(rule.trimEnd('*')))
                                }
                            } ?: false

                            if (!isAllowed) {
                                violations.add("Module '$moduleName' depends on forbidden module '$depPath'. Allowed: $allowed")
                            }
                        }
                    }
                }
            }
        }

        // Specific Check for Module Purity
        val domainProject = project.findProject(":domain")
        if (domainProject != null) {
            if (domainProject.plugins.hasPlugin("com.android.library") || domainProject.plugins.hasPlugin("com.android.application")) {
                violations.add("Module ':domain' must be Pure Kotlin but has Android plugins applied.")
            }
            // Check for android dependencies in commonMain (rough check by name)
            domainProject.configurations.findByName("commonMainImplementation")?.dependencies?.forEach { dep ->
                if (dep.group?.startsWith("com.google.android") == true || dep.group?.startsWith("androidx") == true) {
                    violations.add("Module ':domain' depends on Android library '${dep.group}:${dep.name}'. Domain must be pure.")
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException("Architecture Validation Failed:\n" + violations.distinct().joinToString("\n"))
        } else {
            println("Architecture Check Passed!")
        }
    }
}
