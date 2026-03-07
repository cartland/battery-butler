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
    val reportFile: File = project.rootProject.file("docs/CODE_ANALYSIS.md")

    @get:OutputFile
    val sankeyFile: File = project.rootProject.file("docs/diagrams/code_distribution.mmd")

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
        val sankeyContent = SankeyChartGenerator().generate(result)

        reportFile.parentFile.mkdirs()
        reportFile.writeText(content)

        sankeyFile.parentFile.mkdirs()
        sankeyFile.writeText(sankeyContent + "\n")

        // Embed the .mmd content into the report between markers
        embedMermaid(reportFile, sankeyContent)

        println("Code Share Analysis generated at: ${reportFile.absolutePath}")
        println("Sankey chart generated at: ${sankeyFile.absolutePath}")
        println("Total Lines: ${result.totalLines}")
    }

    private fun embedMermaid(
        target: File,
        mermaidContent: String,
    ) {
        val beginMarker = "<!-- GENERATED:BEGIN ${sankeyFile.name} -->"
        val endMarker = "<!-- GENERATED:END ${sankeyFile.name} -->"
        val lines = target.readLines()
        val output = StringBuilder()
        var skipping = false

        for (line in lines) {
            when {
                line == beginMarker -> {
                    output.appendLine(line)
                    output.appendLine("```mermaid")
                    output.appendLine(mermaidContent)
                    output.appendLine("```")
                    skipping = true
                }
                line == endMarker -> {
                    skipping = false
                    output.appendLine(line)
                }
                !skipping -> output.appendLine(line)
            }
        }

        target.writeText(output.toString())
    }
}
