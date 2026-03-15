package importboundary

import org.gradle.api.Plugin
import org.gradle.api.Project

class ImportBoundaryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("checkImportBoundary", ImportBoundaryCheckTask::class.java) {
            group = "verification"
            description = "Checks that modules do not import from forbidden architectural layers."
        }
    }
}
