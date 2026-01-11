import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

class ProjectScanner(private val rootProject: Project) {

    fun scanModules(allProjects: Set<Project>): Set<String> {
        val activeModules = mutableSetOf<String>()
        allProjects.forEach { subproject ->
            val modulePath = subproject.path
            if (modulePath != ":buildSrc" && modulePath != ":server") {
                activeModules.add(modulePath)
            }
        }
        return activeModules
    }

    fun scanDependencies(allProjects: Set<Project>): Set<Pair<String, String>> {
        val dependencyEdges = mutableSetOf<Pair<String, String>>()
        val configurationsToCheck = listOf(
            "implementation", "api", 
            "commonMainImplementation", "commonMainApi",
            "androidMainImplementation", "commonTestImplementation"
        )
        
        allProjects.forEach { subproject ->
             if (subproject.path == ":buildSrc" || subproject.path == ":server") return@forEach
             
             configurationsToCheck.forEach { configName ->
                val config = subproject.configurations.findByName(configName)
                if (config != null) {
                    config.dependencies.forEach { dep ->
                        if (dep is ProjectDependency) {
                             dependencyEdges.add(subproject.path to dep.dependencyProject.path)
                        }
                    }
                }
            }
        }
        return dependencyEdges
    }
}
