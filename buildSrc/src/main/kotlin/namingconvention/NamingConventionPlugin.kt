package namingconvention

import org.gradle.api.Plugin
import org.gradle.api.Project

class NamingConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("checkNamingConventions", NamingConventionCheckTask::class.java) {
            group = "verification"
            description = "Checks that class names follow KMP-friendly naming conventions."
        }
    }
}
