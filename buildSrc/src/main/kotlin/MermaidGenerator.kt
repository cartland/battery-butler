import org.gradle.api.logging.Logger

class MermaidGenerator(
    private val config: GraphConfig = GraphConfig.default,
) {
    fun generateContent(
        graphData: GraphData,
        logger: Logger? = null,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("graph TD")

        val modulesByGroup = graphData.modules
            .groupBy { modulePath ->
                val group = resolveGroup(modulePath)
                if (group == config.defaultGroup) {
                    logger?.warn(
                        "Architecture Diagram Warning: Module '$modulePath' is not explicitly mapped to a layer. It will appear in '${config.defaultGroup}'.",
                    )
                }
                group
            }.toMutableMap()

        config.groupOrder.forEach { group ->
            val modules = modulesByGroup[group] ?: return@forEach
            sb.appendLine("    subgraph \"$group\"")
            modules.sorted().forEach { modulePath ->
                sb.appendLine("        ${getNodeId(modulePath)}[\"$modulePath\"]")
            }
            sb.appendLine("    end")
            sb.appendLine()
        }

        val otherModules = modulesByGroup[config.defaultGroup]
        if (!otherModules.isNullOrEmpty() && !config.groupOrder.contains(config.defaultGroup)) {
            sb.appendLine("    subgraph \"${config.defaultGroup}\"")
            otherModules.sorted().forEach { modulePath ->
                sb.appendLine("        ${getNodeId(modulePath)}[\"$modulePath\"]")
            }
            sb.appendLine("    end")
            sb.appendLine()
        }

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

    private fun resolveGroup(modulePath: String): String {
        config.moduleGroups[modulePath]?.let { return it }

        config.groupPrefixes.forEach { (prefix, group) ->
            if (modulePath.startsWith(prefix)) return group
        }

        return config.defaultGroup
    }

    private fun getNodeId(modulePath: String): String =
        modulePath
            .split(':', '-')
            .filter { it.isNotEmpty() }
            .joinToString("") { it.capitalize() }

    private fun String.capitalize(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
