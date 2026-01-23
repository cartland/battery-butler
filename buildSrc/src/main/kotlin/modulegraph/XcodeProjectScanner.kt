package modulegraph

import org.gradle.api.Project
import java.io.File

class XcodeProjectScanner(
    private val rootProject: Project,
    private val config: GraphConfig = GraphConfig.default,
) {
    private val xcodeConfig = config.scanner.xcode

    fun scanModules(): Set<String> {
        val modules = mutableSetOf<String>()
        val xcodeProjects = findXcodeProjects()

        xcodeProjects.forEach { xcodeProj ->
            val moduleName = getModuleNameFromXcodePath(xcodeProj)
            modules.add(moduleName)
        }
        return modules
    }

    fun scanDependencies(): Set<Pair<String, String>> {
        val edges = mutableSetOf<Pair<String, String>>()
        val xcodeProjects = findXcodeProjects()

        xcodeProjects.forEach { xcodeProj ->
            val sourceModule = getModuleNameFromXcodePath(xcodeProj)
            val pbxProj = File(xcodeProj, xcodeConfig.projectFile)

            if (pbxProj.exists()) {
                val dependencies = parseGradleDependencies(pbxProj)
                dependencies.forEach { targetModule ->
                    edges.add(sourceModule to targetModule)
                }
            }
        }
        return edges
    }

    private fun findXcodeProjects(): Set<File> {
        val projects = mutableSetOf<File>()
        rootProject.rootDir
            .walkTopDown()
            .maxDepth(xcodeConfig.searchDepth)
            .filter { it.isDirectory && it.name.endsWith(xcodeConfig.projectExtension) }
            .filter { file ->
                xcodeConfig.ignoredDirs.none { file.path.contains(it) }
            }.forEach { projFile -> projects.add(projFile) }
        return projects
    }

    private fun getModuleNameFromXcodePath(xcodeProj: File): String {
        val relativePath = xcodeProj.relativeTo(rootProject.rootDir)
        val parent = relativePath.parentFile

        return if (parent == null || parent.path == ".") {
            xcodeProj.nameWithoutExtension
        } else {
            parent.name
        }
    }

    private fun parseGradleDependencies(pbxProj: File): Set<String> {
        val dependencies = mutableSetOf<String>()
        val regex = Regex(xcodeConfig.gradleModuleRegex)

        pbxProj.readLines().forEach { line ->
            if (line.contains(xcodeConfig.embedFrameworkToken)) {
                val match = regex.find(line)
                if (match != null) {
                    val gradleModule = match.groupValues[1]
                    dependencies.add(":$gradleModule")
                }
            }
        }
        return dependencies
    }
}
