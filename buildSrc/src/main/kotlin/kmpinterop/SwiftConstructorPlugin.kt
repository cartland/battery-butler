package kmpinterop

import org.gradle.api.Plugin
import org.gradle.api.Project

class SwiftConstructorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("checkSwiftConstructors", SwiftConstructorCheckTask::class.java) {
            group = "verification"
            description = "Checks that Swift passes every parameter when constructing Kotlin state classes."
        }
    }
}
