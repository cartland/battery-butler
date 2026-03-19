package previewtime

import org.gradle.api.Plugin
import org.gradle.api.Project

class PreviewTimePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register(
            "checkPreviewTime",
            PreviewTimeCheckTask::class.java,
        ) {
            group = "verification"
            description =
                "Checks that @Composable functions do not use Clock.System.now() as a default parameter value."
        }
    }
}
