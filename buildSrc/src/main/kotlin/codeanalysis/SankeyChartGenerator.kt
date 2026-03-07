package codeanalysis

class SankeyChartGenerator(
    private val config: SankeyChartConfig = SankeyChartConfig.default,
) {
    fun generate(result: CodeScanner.ScanResult): String {
        val sb = StringBuilder()
        sb.appendLine("%% GENERATED FILE - DO NOT EDIT")
        sb.appendLine("---")
        sb.appendLine("config:")
        sb.appendLine("  sankey:")
        sb.appendLine("    showValues: ${config.showValues}")
        sb.appendLine("    width: ${config.width}")
        sb.appendLine("    height: ${config.height}")
        sb.appendLine("    nodeAlignment: ${config.nodeAlignment}")
        sb.appendLine("    linkColor: ${config.linkColor}")
        sb.appendLine("---")
        sb.appendLine("sankey-beta")
        sb.appendLine()

        // Layer 1: Root → Buckets (sorted desc by line count)
        val sortedBuckets = result.bucketCounts.toList().sortedByDescending { it.second }
        sortedBuckets.forEach { (bucketName, count) ->
            sb.appendLine("${config.rootNodeLabel},${config.displayBucketName(bucketName)},$count")
        }
        sb.appendLine()

        // Layer 2: Bucket → Modules (sorted desc within each bucket)
        sortedBuckets.forEach { (bucketName, _) ->
            val modules = result.bucketModuleCounts[bucketName] ?: return@forEach
            val displayBucket = config.displayBucketName(bucketName)
            modules.toList().sortedByDescending { it.second }.forEach { (moduleName, count) ->
                sb.appendLine("$displayBucket,${config.displayModuleName(moduleName)},$count")
            }
            sb.appendLine()
        }

        // Layer 3: Module → Language (sorted desc within each module)
        val sortedModules = result.moduleExtensionCounts.entries
            .sortedByDescending { it.value.values.sum() }
        sortedModules.forEach { (moduleName, extensions) ->
            extensions.toList().sortedByDescending { it.second }.forEach { (ext, count) ->
                sb.appendLine("${config.displayModuleName(moduleName)},${config.displayExtensionName(ext)},$count")
            }
        }

        return sb.toString().trimEnd()
    }
}
