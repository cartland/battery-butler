package architecture

import org.gradle.api.Plugin
import org.gradle.api.Project

class ArchitecturePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Register the task on the project
        project.tasks.register("checkArchitecture", ArchitectureCheckTask::class.java) {
            group = "verification"
            description = "Checks that module dependencies adhere to the defined architecture."
        }
    }
}
