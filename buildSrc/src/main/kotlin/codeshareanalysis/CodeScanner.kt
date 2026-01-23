package codeshareanalysis

import org.gradle.api.Project
import java.io.File

class CodeScanner(
    private val project: Project,
    private val config: CodeShareConfig = CodeShareConfig.default,
) {
    data class ScanResult(
        val totalLines: Int,
        val bucketCounts: Map<String, Int>,
        val moduleCounts: Map<String, Int>,
    )

    fun scan(): ScanResult {
        val rootDir = project.rootDir
        val bucketMap = mutableMapOf<String, Int>()
        val moduleMap = mutableMapOf<String, Int>()
        var totalLines = 0

        // 1. Get all git tracked files
        val files = getGitTrackedFiles(rootDir)

        files
            .filter { file ->
                // Still respect ignored dirs if defined in config, just in case
                config.ignoredDirs.none { file.path.contains(it) }
            }.filter { file ->
                file.isFile && config.fileExtensions.any { file.extension == it }
            }.forEach { file ->
                val lines = countLines(file)
                totalLines += lines

                // Bucket Attribution
                val bucket = config.buckets.firstOrNull { it.regex.matches(file.path) }
                val bucketName = bucket?.name ?: "Other"
                bucketMap[bucketName] = (bucketMap[bucketName] ?: 0) + lines

                // Module Attribution
                // Attempt to attribute to a gradle module by finding the nearest build.gradle.kts
                val module = findModuleForFile(file, rootDir)
                if (module != null) {
                    moduleMap[module] = (moduleMap[module] ?: 0) + lines
                }
            }

        return ScanResult(totalLines, bucketMap, moduleMap)
    }

    private fun countLines(file: File): Int {
        // Simple line count, ignoring empty lines maybe?
        // User didn't specify, but standard LOC usually implies non-empty or just raw lines.
        // Let's do raw lines for simplicity and predictability.
        return file.readLines().size
    }

    private fun findModuleForFile(
        file: File,
        rootDir: File,
    ): String? {
        // Iterate up until we hit root or find a build.gradle.kts
        var current = file.parentFile
        while (current != null && current != rootDir && current.path.startsWith(rootDir.path)) {
            if (File(current, "build.gradle.kts").exists() || File(current, "build.gradle").exists()) {
                // Determine module spec path (e.g. :server:app)
                return getGradleProjectPath(current, rootDir)
            }
            // Also handle Xcode projects?
            // "This must handle Xcode projects... described by their Gradle module name :domain etc...
            // wait, Xcode projects are NOT gradle modules usually.
            // But the user sample said: ":<module_name>".
            // For Xcode projects like "ios-app-swift-ui", maybe we just use that folder name as the module name?
            // Check for Xcode project in this directory
            // Check for Xcode project in this directory
            val xcodeProj = current.listFiles()?.firstOrNull { it.name.endsWith(".xcodeproj") }
            if (xcodeProj != null) {
                return ":" + xcodeProj.name
            }
            if (File(current, "project.pbxproj").exists()) {
                return ":" + current.parentFile.name + ".xcodeproj"
            }

            current = current.parentFile
        }
        return null
    }

    private fun getGradleProjectPath(
        moduleDir: File,
        rootDir: File,
    ): String {
        val relative = moduleDir.relativeTo(rootDir).path
        return ":" + relative.replace(File.separator, ":")
    }

    private fun getGitTrackedFiles(rootDir: File): List<File> {
        try {
            // Use -z to separate filenames with null bytes to handle spaces/special chars safely
            val process = ProcessBuilder("git", "ls-files", "-z")
                .directory(rootDir)
                .start()

            val content = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                // If git fails (e.g. not a git repo), we might want to fallback or just warn.
                // For now, returning empty list is safer than crashing, but we log to stderr.
                System.err.println("Warning: 'git ls-files' failed with exit code $exitCode. Analysis strictly requires a git repository.")
                return emptyList()
            }

            return content
                .split('\u0000')
                .filter { it.isNotBlank() }
                .map { File(rootDir, it) }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}
