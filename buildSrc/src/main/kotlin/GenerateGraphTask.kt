import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateGraphTask : DefaultTask() {

    @get:OutputFile
    val outputFile: File = project.file("docs/diagrams/architecture.mmd")

    init {
        notCompatibleWithConfigurationCache("Accesses project instance at execution time")
    }

    @TaskAction
    fun generate() {
        // Collect all subprojects
        val allProjects = project.rootProject.subprojects
        
        // Modules map for "clean" naming and grouping
        val moduleGroups = mapOf(
            ":compose-app" to "App & Entry Points",
            ":shared" to "App & Entry Points",
            ":server:app" to "App & Entry Points",
            
            ":ui-feature" to "UI Layer",
            ":ui-core" to "UI Layer",
            
            ":viewmodel" to "Presentation Layer",
            
            ":usecase" to "Domain Layer",
            ":domain" to "Domain Layer",
            ":server:domain" to "Domain Layer",
            
            ":data" to "Data Layer",
            ":networking" to "Data Layer",
            ":server:data" to "Data Layer"
        )
        
        val groupOrder = listOf(
            "App & Entry Points",
            "UI Layer",
            "Presentation Layer",
            "Domain Layer",
            "Data Layer"
        )

        val dependencyEdges = mutableSetOf<Pair<String, String>>()
        val activeModules = mutableSetOf<String>()

        allProjects.forEach { subproject ->
            val modulePath = subproject.path
            // Filter out buildSrc and container projects that don't have src/commonMain or src/main
            if (modulePath == ":buildSrc") return@forEach
            // :server is just a container, usually has no source or dependencies of its own relevant to architecture
            if (modulePath == ":server") return@forEach
            
            activeModules.add(modulePath)

            // Inspect dependencies
            val configurationsToCheck = listOf(
                "implementation", "api", 
                "commonMainImplementation", "commonMainApi",
                "androidMainImplementation", "commonTestImplementation"
            )
            
            val projectDeps = mutableSetOf<String>()

            configurationsToCheck.forEach { configName ->
                val config = subproject.configurations.findByName(configName)
                if (config != null) {
                    config.dependencies.forEach { dep ->
                        if (dep is ProjectDependency) {
                             projectDeps.add(dep.dependencyProject.path)
                        }
                    }
                }
            }
            
            projectDeps.forEach { targetPath ->
                dependencyEdges.add(modulePath to targetPath)
            }
        }

        val sb = StringBuilder()
        sb.appendLine("graph TD")

        // Grouping
        val modulesByGroup = activeModules.groupBy { modulePath ->
            val group = moduleGroups[modulePath]
            if (group == null) {
                logger.warn("Architecture Diagram Warning: Module '$modulePath' is not explicitly mapped to a layer. " +
                        "It will appear in 'Others'. " +
                        "To fix this, add '$modulePath' to the 'moduleGroups' map in buildSrc/src/main/kotlin/GenerateGraphTask.kt.")
                "Others"
            } else {
                group
            }
        }

        // Ordered Groups
        groupOrder.forEach { group ->
            val modules = modulesByGroup[group] ?: return@forEach
            sb.appendLine("    subgraph \"$group\"")
            modules.sorted().forEach { modulePath ->
                 sb.appendLine("        ${getNodeId(modulePath)}[\"$modulePath\"]")
            }
            sb.appendLine("    end")
            sb.appendLine()
        }
        
        // Others
        val otherModules = modulesByGroup["Others"]
        if (!otherModules.isNullOrEmpty()) {
             sb.appendLine("    subgraph \"Others\"")
             otherModules.sorted().forEach { modulePath ->
                 sb.appendLine("        ${getNodeId(modulePath)}[\"$modulePath\"]")
             }
             sb.appendLine("    end")
             sb.appendLine()
        }

        // Edges
        sb.appendLine("    %% Dependencies")
        var previousSource: String? = null
        dependencyEdges
            .filter { activeModules.contains(it.first) && activeModules.contains(it.second) }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .forEach { (source, target) ->
                if (previousSource != null && previousSource != source) {
                    sb.appendLine()
                }
                sb.appendLine("    ${getNodeId(source)} --> ${getNodeId(target)}")
                previousSource = source
            }

        outputFile.parentFile.mkdirs()
        outputFile.writeText(sb.toString())
        println("Generated Architecture Graph at: ${outputFile.absolutePath}")
    }

    private fun getNodeId(modulePath: String): String {
        // Custom overrides for naming consistency
        if (modulePath == ":compose-app") return "ComposeApp"
        if (modulePath == ":server:app") return "ServerApp"
        if (modulePath == ":server:domain") return "ServerDomain"
        if (modulePath == ":server:data") return "ServerData"
        if (modulePath == ":ui-feature") return "UIFeature"
        if (modulePath == ":ui-core") return "UICore"
        if (modulePath == ":viewmodel") return "ViewModel"
        if (modulePath == ":usecase") return "UseCase"
        
        // Fallback: :group:module -> GroupModule
        return modulePath.split(":").joinToString("") { it.capitalize() }
    }
    
    // String extension for older Kotlin versions if needed, but buildSrc usually runs recent Kotlin.
    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
