package codeshareanalysis

data class CodeShareConfig(
    val buckets: List<Bucket>,
    val fileExtensions: List<String> = listOf("kt", "swift", "java"),
    val ignoredDirs: List<String> = listOf("/build/", "/node_modules/", "/.git/", "/.gradle/", "/.idea/", "/bazel-", "/.bazel/"),
) {
    data class Bucket(
        val name: String,
        val regex: Regex,
    )

    companion object {
        val default = CodeShareConfig(
            buckets = listOf(
                // Order matters: First match wins
                Bucket("Server", Regex(".*[/\\\\]server[/\\\\].*")),
                Bucket("iOS Swift App", Regex(".*[/\\\\]ios-app-swift-ui[/\\\\].*")),
                Bucket("Android Screenshot Tests", Regex(".*[/\\\\]android-screenshot-tests[/\\\\].*")),
                Bucket("CMP Android, iOS, Desktop", Regex(".*[/\\\\]compose-app[/\\\\].*")),
                Bucket(
                    "Shared Code",
                    Regex(
                        ".*[/\\\\](domain|data|data-local|data-network|ai|usecase|viewmodel|presentation-core|presentation-feature|networking|compose-resources)[/\\\\].*",
                    ),
                ),
                Bucket("Other", Regex(".*")),
            ),
        )
    }
}
