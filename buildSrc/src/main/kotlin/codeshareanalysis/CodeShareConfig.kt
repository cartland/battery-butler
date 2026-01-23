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
                // Shared code is likely everything else in root modules that are KMP
                // Assuming domain, data, usecase, etc. are shared.
                // We can be specific or catch-all "Shared" if it doesn't match above specific platforms.
                // Wait, "Shared Code" in user request is 68%. This likely includes all core modules.
                // Let's list known shared modules or regex for root level modules.
                // Matches: /domain/, /data/, /usecase/, /viewmodel/, /presentation-core/, /networking/, /compose-resources/
                Bucket(
                    "Shared Code",
                    Regex(
                        ".*[/\\\\](domain|data|usecase|viewmodel|presentation-core|presentation-feature|networking|compose-resources)[/\\\\].*",
                    ),
                ),
                // Catch all "Other" - e.g. buildSrc itself, or gradle scripts (if we counted kts, but we default to kt)
                Bucket("Other", Regex(".*")),
            ),
        )
    }
}
