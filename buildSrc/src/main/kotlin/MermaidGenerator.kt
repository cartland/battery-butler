import org.gradle.api.Project
import java.io.File

class MermaidGenerator(private val project: Project) {

    private val moduleGroups = mapOf(
        ":compose-app" to "Compose Apps",
        ":ios-integration" to "iOS Apps",
        ":server:app" to "Server",
        ":ui-feature" to "Compose + ViewModel UI",
        ":ui-core" to "Compose UI",
        ":viewmodel" to "Presentation Layer",
        ":usecase" to "Domain Layer",
        ":domain" to "Domain Layer",
        ":server:domain" to "Server",
        ":data" to "Data Layer",
        ":networking" to "Data Layer",
        ":server:data" to "Server"
    )
    
    private val groupOrder = listOf(
        "iOS Apps",
        "Compose Apps",
        "Server",
        "Compose UI",
        "Compose + ViewModel UI",
        "Presentation Layer",
        "Domain Layer",
        "Data Layer",
        "Deprecated"
    )

    fun generateDiagram(
        activeModules: Set<String>,
        dependencyEdges: Set<Pair<String, String>>,
        outputMmd: File,
        outputSvg: File,
        includeIos: Boolean
    ) {
        val sb = StringBuilder()
        sb.appendLine("graph TD")

        // Grouping
        val modulesByGroup = activeModules.groupBy { modulePath ->
            val group = moduleGroups[modulePath]
            if (group == null) {
                // Only log warning for the primary graph to avoid duplicate logs
                if (!includeIos) {
                    project.logger.warn("Architecture Diagram Warning: Module '$modulePath' is not explicitly mapped to a layer. It will appear in 'Others'.")
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
    
    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
