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

    private fun countLines(file: File): Int = file.readLines().size

    private fun findModuleForFile(
        file: File,
        rootDir: File,
    ): String? {
        // Iterate up until we hit root or find a build.gradle.kts
        var current = file.parentFile
        while (current != null && current != rootDir && current.path.startsWith(rootDir.path)) {
            if (File(current, "build.gradle.kts").exists() || File(current, "build.gradle").exists()) {
                return getGradleProjectPath(current, rootDir)
            }
            // Handle Xcode projects (use folder name as module name)
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
            // Set directory explicitly and inherit environment for worktree support
            val processBuilder = ProcessBuilder("git", "ls-files", "-z")
                .directory(rootDir)
                .redirectErrorStream(true)

            // Ensure environment is inherited (especially GIT_DIR for worktrees)
            processBuilder.environment().putAll(System.getenv())

            val process = processBuilder.start()

            val content = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                // If git fails (e.g. not a git repo), we might want to fallback or just warn.
                System.err.println("Warning: 'git ls-files' failed with exit code $exitCode in directory: ${rootDir.absolutePath}")
                System.err.println("Output: $content")
                return emptyList()
            }

            return content
                .split('\u0000')
                .filter { it.isNotBlank() }
                .map { File(rootDir, it) }
        } catch (e: Exception) {
            System.err.println("Exception running git ls-files: ${e.message}")
            return emptyList()
        }
    }
}
