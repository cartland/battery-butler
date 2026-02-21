package screenshotcoverage

import org.gradle.api.Plugin
import org.gradle.api.Project

class PreviewCoveragePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("checkPreviewCoverage", PreviewCoverageCheckTask::class.java) {
            group = "verification"
            description = "Checks that all @Preview composables have screenshot tests."
        }
    }
}
