package modulegraph

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

data class GraphData(
    val modules: Set<String>,
    val edges: Set<Pair<String, String>>,
)

class ProjectScanner(
    private val project: Project,
    private val config: GraphConfig = GraphConfig.default,
) {
    private val xcodeScanner = XcodeProjectScanner(project.rootProject, config)

    fun scan(includeIos: Boolean): GraphData {
        val subprojects = project.rootProject.subprojects

        val modules = mutableSetOf<String>()
        subprojects.forEach { subproject ->
            if (subproject.buildFile.exists()) {
                modules.add(subproject.path)
            }
        }

        val edges = mutableSetOf<Pair<String, String>>()
        val configurationsToCheck = config.scanner.gradleConfigurations

        subprojects.forEach { subproject ->
            if (!subproject.buildFile.exists()) return@forEach

            configurationsToCheck.forEach { configName ->
                val config = subproject.configurations.findByName(configName)
                if (config != null) {
                    config.dependencies.forEach { dep ->
                        if (dep is ProjectDependency) {
                            edges.add(subproject.path to dep.dependencyProject.path)
                        }
                    }
                }
            }
        }

        if (includeIos) {
            modules.addAll(xcodeScanner.scanModules())
            edges.addAll(xcodeScanner.scanDependencies())
        }

        val validEdges = edges
            .filter { (source, target) ->
                modules.contains(source) && modules.contains(target)
            }.toSet()

        return GraphData(modules, validEdges)
    }
}
