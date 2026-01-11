import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

data class GraphData(
    val modules: Set<String>,
    val edges: Set<Pair<String, String>>
)

class ProjectScanner(private val project: Project) {

    private val iosScanner = IosProjectScanner(project.rootProject)

    fun scan(includeIos: Boolean): GraphData {
        val subprojects = project.rootProject.subprojects
        
        // 1. Scan Kotlin Modules
        val modules = mutableSetOf<String>()
        subprojects.forEach { subproject ->
            if (subproject.buildFile.exists()) {
                modules.add(subproject.path)
            }
        }

        // 2. Scan Kotlin Dependencies
        val edges = mutableSetOf<Pair<String, String>>()
        val configurationsToCheck = listOf(
            "implementation", "api", 
            "commonMainImplementation", "commonMainApi",
            "androidMainImplementation", "commonTestImplementation"
        )
        
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

        // 3. Merge iOS Data if requested
        if (includeIos) {
            modules.addAll(iosScanner.scanModules())
            edges.addAll(iosScanner.scanDependencies())
        }

        // Filter edges to ensure both nodes exist in the graph
        val validEdges = edges.filter { (source, target) -> 
            modules.contains(source) && modules.contains(target)
        }.toSet()

        return GraphData(modules, validEdges)
    }
}
