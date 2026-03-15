package themelayer

import org.gradle.api.Plugin
import org.gradle.api.Project

class ThemeLayerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("checkThemeLayer", ThemeLayerCheckTask::class.java) {
            group = "verification"
            description =
                "Checks that UI modules (presentation-feature, compose-app) do not bypass the theme layer."
        }
    }
}
