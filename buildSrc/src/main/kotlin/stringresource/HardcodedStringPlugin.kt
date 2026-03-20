package stringresource

import org.gradle.api.Plugin
import org.gradle.api.Project

class HardcodedStringPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("checkHardcodedStrings", HardcodedStringCheckTask::class.java) {
            group = "verification"
            description = "Checks for hardcoded strings in Compose UI code that should use string resources."
        }
    }
}
