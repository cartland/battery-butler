data class GraphConfig(
    val moduleGroups: Map<String, String>,
    val groupPrefixes: Map<String, String>,
    val groupOrder: List<String>,
    val defaultGroup: String = "Others",
) {
    companion object {
        val default = GraphConfig(
            moduleGroups = mapOf(
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
            ),
            groupPrefixes = mapOf(
                "ios-app" to "iOS Apps",
            ),
            groupOrder = listOf(
                "iOS Apps",
                "Compose Apps",
                "Server",
                "Compose UI",
                "Compose + ViewModel UI",
                "Presentation Layer",
                "Domain Layer",
                "Data Layer",
                "Deprecated",
                "Others",
            ),
        )
    }
}
