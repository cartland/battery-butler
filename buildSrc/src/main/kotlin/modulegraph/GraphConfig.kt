package modulegraph


data class GraphConfig(
    val moduleGroups: Map<String, String>,
    val groupPrefixes: Map<String, String>,
    val groupOrder: List<String>,
    val defaultGroup: String = "Others",
    val outputPaths: OutputPaths,
    val mermaidCli: MermaidCli,
    val scanner: ScannerConfig,
) {
    data class OutputPaths(
        val kotlinGraphMmd: String,
        val kotlinGraphSvg: String,
        val fullGraphMmd: String,
        val fullGraphSvg: String,
    )

    data class MermaidCli(
        val theme: String,
        val cssFile: String,
    )

    data class ScannerConfig(
        val gradleConfigurations: List<String>,
        val xcode: XcodeConfig,
    )

    data class XcodeConfig(
        val searchDepth: Int,
        val ignoredDirs: List<String>,
        val projectExtension: String,
        val projectFile: String,
        val embedFrameworkToken: String,
        val gradleModuleRegex: String,
    )

    companion object {
        val default = GraphConfig(
            moduleGroups = mapOf(
                ":android-app" to "Android App",
                ":android-screenshot-tests" to "Screenshot Tests",
                ":compose-app" to "Compose Apps",
                ":ios-swift-di" to "iOS Apps",
                ":server:app" to "Server",
                ":presentation-feature" to "Presentation",
                ":presentation-core" to "Presentation",
                ":viewmodel" to "Presentation",
                ":presentation-model" to "Presentation",
                ":compose-resources" to "Presentation",
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
                "Android App",
                "iOS Apps",
                "Compose Apps",
                "Server",
                "Presentation",
                "Domain Layer",
                "Data Layer",
                "Screenshot Tests",
                "Others",
                "Deprecated",
            ),
            outputPaths = OutputPaths(
                kotlinGraphMmd = "docs/diagrams/kotlin_module_structure.mmd",
                kotlinGraphSvg = "docs/diagrams/kotlin_module_structure.svg",
                fullGraphMmd = "docs/diagrams/full_system_structure.mmd",
                fullGraphSvg = "docs/diagrams/full_system_structure.svg",
            ),
            mermaidCli = MermaidCli(
                theme = "default",
                cssFile = "buildSrc/config/mermaid/mermaid.css",
            ),
            scanner = ScannerConfig(
                gradleConfigurations = listOf(
                    "implementation",
                    "api",
                    "commonMainImplementation",
                    "commonMainApi",
                    "androidMainImplementation",
                    "commonTestImplementation",
                ),
                xcode = XcodeConfig(
                    searchDepth = 3,
                    ignoredDirs = listOf("/build/", "/node_modules/"),
                    projectExtension = ".xcodeproj",
                    projectFile = "project.pbxproj",
                    embedFrameworkToken = "embedAndSignAppleFrameworkForXcode",
                    gradleModuleRegex = "[:](.+):embedAndSignAppleFrameworkForXcode",
                ),
            ),
        )
    }
}
