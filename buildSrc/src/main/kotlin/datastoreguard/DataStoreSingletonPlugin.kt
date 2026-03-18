package datastoreguard

import org.gradle.api.Plugin
import org.gradle.api.Project

class DataStoreSingletonPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register(
            "checkDataStoreSingleton",
            DataStoreSingletonCheckTask::class.java,
        ) {
            group = "verification"
            description =
                "Checks that @Provides methods for DataStore and Room have singleton scope annotations."
        }
    }
}
