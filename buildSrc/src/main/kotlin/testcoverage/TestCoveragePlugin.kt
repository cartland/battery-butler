package testcoverage

import org.gradle.api.Plugin
import org.gradle.api.Project

class TestCoveragePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("checkTestCoverage", TestCoverageCheckTask::class.java) {
            group = "verification"
            description = "Checks that enforced classes have corresponding test files."
        }
    }
}
