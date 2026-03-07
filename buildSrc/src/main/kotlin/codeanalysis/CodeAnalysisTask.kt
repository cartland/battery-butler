package codeanalysis

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CodeAnalysisTask : DefaultTask() {
    private val config = CodeAnalysisConfig.default

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sourceFiles: FileTree
        get() = project.rootProject.fileTree(project.rootProject.projectDir) {
            include(config.fileExtensions.map { "**/*.$it" })
            exclude(config.ignoredDirs.map { "**$it**" })
        }

    @get:OutputFile
    val reportFile: File = project.rootProject.file("docs/CODE_ANALYSIS.md")

    @get:OutputFile
    val sankeyFile: File = project.rootProject.file("docs/diagrams/code_distribution.mmd")

    init {
        group = "documentation"
        description = "Generates a code analysis report."
    }

    @get:Internal
    var additionalEmbedTargetPaths: List<String> = listOf("README.md")

    @TaskAction
    fun analyze() {
        val scanner = CodeScanner(project, config)
        val sankeyConfig = SankeyChartConfig.default
        val generator = ReportGenerator(extensionDisplayNames = sankeyConfig.extensionDisplayNames)

        println("Scanning codebase for lines of code...")
        val result = scanner.scan()

        println("Generating report...")
        val content = generator.generate(result)
        val sankeyContent = SankeyChartGenerator(sankeyConfig).generate(result)

        reportFile.parentFile.mkdirs()
        reportFile.writeText(content)

        sankeyFile.parentFile.mkdirs()
        sankeyFile.writeText(sankeyContent + "\n")

        // Embed the .mmd content into the report between markers
        MermaidEmbedder.embed(reportFile, sankeyFile.name, sankeyContent)

        // Also embed into additional targets if they have the markers
        for (path in additionalEmbedTargetPaths) {
            val targetFile = project.rootProject.file(path)
            if (targetFile.exists() && targetFile.readText().contains("<!-- GENERATED:BEGIN ${sankeyFile.name} -->")) {
                MermaidEmbedder.embed(targetFile, sankeyFile.name, sankeyContent)
                println("Sankey chart embedded into: ${targetFile.absolutePath}")
            }
        }

        println("Code analysis generated at: ${reportFile.absolutePath}")
        println("Sankey chart generated at: ${sankeyFile.absolutePath}")
        println("Total Lines: ${result.totalLines}")
    }
}
