import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateGraphTask : DefaultTask() {

    @get:OutputFile
    val outputFile: File = project.file("docs/diagrams/kotlin_module_structure.mmd")

    @get:OutputFile
    val svgFile: File = project.file("docs/diagrams/kotlin_module_structure.svg")

    init {
        notCompatibleWithConfigurationCache("Accesses project instance at execution time")
    }

    @TaskAction
    fun generate() {
        val allProjects = project.rootProject.subprojects
        val activeModules = scanModules(allProjects)
        val dependencyEdges = scanDependencies(allProjects)
        
        // 1. Generate Standard Kotlin Graph
        generateDiagram(
            activeModules = activeModules,
            dependencyEdges = dependencyEdges,
            outputMmd = outputFile,
            outputSvg = svgFile,
            includeIos = false
        )

        // 2. Generate Full System Graph (with iOS)
        val fullInfoMmd = project.file("docs/diagrams/full_system_structure.mmd")
        val fullInfoSvg = project.file("docs/diagrams/full_system_structure.svg")
        generateDiagram(
            activeModules = activeModules,
            dependencyEdges = dependencyEdges,
            outputMmd = fullInfoMmd,
            outputSvg = fullInfoSvg,
            includeIos = true
        )
    }

    private fun scanModules(allProjects: Set<Project>): Set<String> {
        val activeModules = mutableSetOf<String>()
        allProjects.forEach { subproject ->
            val modulePath = subproject.path
            if (modulePath != ":buildSrc" && modulePath != ":server") {
                activeModules.add(modulePath)
            }
        }
        return activeModules
    }

    private fun scanDependencies(allProjects: Set<Project>): Set<Pair<String, String>> {
        val dependencyEdges = mutableSetOf<Pair<String, String>>()
        val configurationsToCheck = listOf(
            "implementation", "api", 
            "commonMainImplementation", "commonMainApi",
            "androidMainImplementation", "commonTestImplementation"
        )
        
        allProjects.forEach { subproject ->
             if (subproject.path == ":buildSrc" || subproject.path == ":server") return@forEach
             
             configurationsToCheck.forEach { configName ->
                val config = subproject.configurations.findByName(configName)
                if (config != null) {
                    config.dependencies.forEach { dep ->
                        if (dep is ProjectDependency) {
                             dependencyEdges.add(subproject.path to dep.dependencyProject.path)
                        }
                    }
                }
            }
        }
        return dependencyEdges
    }

    private fun generateDiagram(
        activeModules: Set<String>,
        dependencyEdges: Set<Pair<String, String>>,
        outputMmd: File,
        outputSvg: File,
        includeIos: Boolean
    ) {
        val moduleGroups = mapOf(
            ":compose-app" to "Compose Apps",

            ":ios-integration" to "iOS Apps",
            ":server:app" to "Server",
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
            "iOS Apps",
            "Compose Apps",
            "Server",
            "UI Layer",
            "Presentation Layer",
            "Domain Layer",
            "Data Layer",
            "Deprecated"
        )

        val sb = StringBuilder()
        sb.appendLine("graph TD")

        // Grouping
        val modulesByGroup = activeModules.groupBy { modulePath ->
            val group = moduleGroups[modulePath]
            if (group == null) {
                // Only log warning for the primary graph to avoid duplicate logs
                if (!includeIos) {
                    logger.warn("Architecture Diagram Warning: Module '$modulePath' is not explicitly mapped to a layer. It will appear in 'Others'.")
                }
                "Others"
            } else {
                group
            }
        }.toMutableMap()
        
        if (includeIos) {
            // Add fake iOS modules to the grouping
            // Check existence of directories
            if (project.rootProject.file("ios-app-swift-ui").exists()) {
                 modulesByGroup.computeIfAbsent("iOS Apps") { mutableListOf() }
                 (modulesByGroup["iOS Apps"] as MutableList).add("ios-app-swift-ui")
            }
            if (project.rootProject.file("ios-app-compose-ui").exists()) {
                 modulesByGroup.computeIfAbsent("iOS Apps") { mutableListOf() }
                 (modulesByGroup["iOS Apps"] as MutableList).add("ios-app-compose-ui")
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
        
        val allEdges = dependencyEdges.toMutableSet()
        if (includeIos) {
             if (project.rootProject.file("ios-app-swift-ui").exists()) {
                 allEdges.add("ios-app-swift-ui" to ":ios-integration")
             }
             if (project.rootProject.file("ios-app-compose-ui").exists()) {
                 allEdges.add("ios-app-compose-ui" to ":compose-app")
             }
        }

        allEdges
            .filter { 
                (activeModules.contains(it.first) || (includeIos && it.first.startsWith("ios-app"))) && 
                (activeModules.contains(it.second) || (includeIos && it.second.startsWith("ios-app")))
             }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .forEach { (source, target) ->
                if (previousSource != null && previousSource != source) {
                    sb.appendLine()
                }
                sb.appendLine("    ${getNodeId(source)} --> ${getNodeId(target)}")
                previousSource = source
            }

        val newContent = sb.toString()
        val contentChanged = !outputMmd.exists() || outputMmd.readText() != newContent
        
        if (contentChanged) {
            outputMmd.parentFile.mkdirs()
            outputMmd.writeText(newContent)
            println("Generated Graph at: ${outputMmd.absolutePath}")
        } else {
             println("Graph content is unchanged: ${outputMmd.name}")
        }

        if (contentChanged || !outputSvg.exists()) {
             println("Generating SVG for ${outputMmd.name}...")
             project.exec {
                 // Pass empty string as separate argument without quotes for Gradle to handle
                 commandLine("npx", "-y", "@mermaid-js/mermaid-cli", "-i", outputMmd.absolutePath, "-o", outputSvg.absolutePath, "-t", "default", "--cssFile", "")
             }
             println("Generated SVG at: ${outputSvg.absolutePath}")
        }
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
        if (modulePath == ":ios-integration") return "IosIntegration"
        
        // Fallback: :group:module -> GroupModule
        return modulePath.split(":").joinToString("") { it.capitalize() }
    }
    
    // String extension for older Kotlin versions if needed, but buildSrc usually runs recent Kotlin.
    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
