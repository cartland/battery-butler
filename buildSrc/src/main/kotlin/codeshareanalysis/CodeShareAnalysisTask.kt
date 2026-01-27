package codeshareanalysis

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CodeShareAnalysisTask : DefaultTask() {
    private val config = CodeShareConfig.default

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sourceFiles: FileTree
        get() = project.rootProject.fileTree(project.rootProject.projectDir) {
            include(config.fileExtensions.map { "**/*.$it" })
            exclude(config.ignoredDirs.map { "**$it**" })
        }

    @get:OutputFile
    val reportFile: File = project.rootProject.file("docs/Code_Share_Analysis.md")

    init {
        group = "documentation"
        description = "Generates a code sharing analysis report."
    }

    @TaskAction
    fun analyze() {
        val scanner = CodeScanner(project, config)
        val generator = ReportGenerator()

        println("Scanning codebase for lines of code...")
        val result = scanner.scan()

        println("Generating report...")
        val content = generator.generate(result)

        reportFile.parentFile.mkdirs()
        reportFile.writeText(content)

        println("Code Share Analysis generated at: ${reportFile.absolutePath}")
        println("Total Lines: ${result.totalLines}")
    }
}
