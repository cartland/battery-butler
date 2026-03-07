package codeshareanalysis

class SankeyChartConfig(
    val rootNodeLabel: String = "Codebase",
    val width: Int = 800,
    val height: Int = 1000,
    val nodeAlignment: String = "justify",
    val linkColor: String = "gradient",
    val showValues: Boolean = true,
    val bucketDisplayNames: Map<String, String> = emptyMap(),
    val moduleSuffixesToStrip: List<String> = emptyList(),
    val modulePrefix: String = "",
) {
    fun displayBucketName(rawName: String): String = bucketDisplayNames[rawName] ?: rawName

    fun displayModuleName(rawName: String): String {
        var name = rawName.removePrefix(modulePrefix)
        moduleSuffixesToStrip.forEach { name = name.removeSuffix(it) }
        return name
    }

    companion object {
        val default =
            SankeyChartConfig(
                bucketDisplayNames =
                    mapOf(
                        "CMP Android, iOS, Desktop" to "CMP Apps",
                        "Android Screenshot Tests" to "Screenshot Tests",
                    ),
                modulePrefix = ":",
                moduleSuffixesToStrip = listOf(".xcodeproj"),
            )
    }
}
