import org.gradle.api.logging.Logger

class MermaidGenerator {
    private val moduleGroups = mapOf(
        ":compose-app" to "Compose Apps",
        ":ios-swift-di" to "iOS Apps",
        ":server:app" to "Server",
        ":ui-feature" to "Compose + ViewModel UI",
        ":ui-core" to "Compose UI",
        ":viewmodel" to "Presentation Layer",
        ":usecase" to "Domain Layer",
        ":domain" to "Domain Layer",
        ":server:domain" to "Server",
        ":data" to "Data Layer",
        ":networking" to "Data Layer",
        ":server:data" to "Server",
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
        "Deprecated",
    )

    fun generateContent(
        graphData: GraphData,
        logger: Logger? = null,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("graph TD")

        // Grouping
        val modulesByGroup = graphData.modules
            .groupBy { modulePath ->
                val group = resolveGroup(modulePath)
                if (group == "Others") {
                    logger?.warn(
                        "Architecture Diagram Warning: Module '$modulePath' is not explicitly mapped to a layer. It will appear in 'Others'.",
                    )
                }
                group
            }.toMutableMap()

        // Ensure manual iOS modules fall into correct groups if present (though map covers them usually, manual override just in case)
        // Actually, the resolveGroup function should handle them if we map them.
        // Let's add them to the map to be safe in resolveGroup, or explicit handling.
        // Since we decoupled, we trust graphData.modules contains them.

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

        graphData.edges
            .sortedWith(compareBy({ it.first }, { it.second }))
            .forEach { (source, target) ->
                if (previousSource != null && previousSource != source) {
                    sb.appendLine()
                }
                sb.appendLine("    ${getNodeId(source)} --> ${getNodeId(target)}")
                previousSource = source
            }

        return sb.toString()
    }

    private fun resolveGroup(modulePath: String): String =
        moduleGroups[modulePath] ?: when {
            modulePath.startsWith("ios-app") -> "iOS Apps"
            else -> "Others"
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
        if (modulePath == ":ios-swift-di") return "IosSwiftDi"

        // Fallback: :group:module -> GroupModule
        return modulePath.split(":").joinToString("") { it.capitalize() }
    }

    private fun String.capitalize(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
