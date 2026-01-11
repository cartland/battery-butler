import org.gradle.api.Project
import java.io.File

class IosProjectScanner(private val rootProject: Project) {

    fun scanModules(): Set<String> {
        val modules = mutableSetOf<String>()
        if (rootProject.file("ios-app-swift-ui").exists()) {
            modules.add("ios-app-swift-ui")
        }
        if (rootProject.file("ios-app-compose-ui").exists()) {
            modules.add("ios-app-compose-ui")
        }
        return modules
    }

    fun scanDependencies(): Set<Pair<String, String>> {
        val edges = mutableSetOf<Pair<String, String>>()
        if (rootProject.file("ios-app-swift-ui").exists()) {
            edges.add("ios-app-swift-ui" to ":ios-integration")
        }
        if (rootProject.file("ios-app-compose-ui").exists()) {
            edges.add("ios-app-compose-ui" to ":compose-app")
        }
        return edges
    }
}
