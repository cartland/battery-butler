import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

abstract class GenerateGraphTask : DefaultTask() {

    @get:OutputFile
    val kotlinModuleMmdFile: File = project.file("docs/diagrams/kotlin_module_structure.mmd")

    @get:OutputFile
    val kotlinModuleSvgFile: File = project.file("docs/diagrams/kotlin_module_structure.svg")

    @get:OutputFile
    val fullSystemMmdFile: File = project.file("docs/diagrams/full_system_structure.mmd")

    @get:OutputFile
    val fullSystemSvgFile: File = project.file("docs/diagrams/full_system_structure.svg")

    init {
        notCompatibleWithConfigurationCache("Accesses project instance at execution time")
    }

    @TaskAction
    fun generate() {
        val scanner = ProjectScanner(project)
        val generator = MermaidGenerator()
        val logger = project.logger

        // 1. Kotlin Module Graph
        val kotlinGraphData = scanner.scan(includeIos = false)
        val kotlinContent = generator.generateContent(kotlinGraphData, logger)
        val kotlinChanged = updateFile(kotlinModuleMmdFile, kotlinContent)
        
        if (kotlinChanged || !kotlinModuleSvgFile.exists()) {
            generateSvg(kotlinModuleMmdFile, kotlinModuleSvgFile)
        }

        // 2. Full System Graph
        val fullGraphData = scanner.scan(includeIos = true)
        val fullContent = generator.generateContent(fullGraphData, logger)
        val fullChanged = updateFile(fullSystemMmdFile, fullContent)
        
        if (fullChanged || !fullSystemSvgFile.exists()) {
             generateSvg(fullSystemMmdFile, fullSystemSvgFile)
        }
    }

    private fun updateFile(file: File, content: String): Boolean {
        val currentContent = if (file.exists()) file.readText() else ""
        if (currentContent != content) {
            file.parentFile.mkdirs()
            file.writeText(content)
            println("Generated Graph at: ${file.absolutePath}")
            return true
        } else {
            println("Graph content is unchanged: ${file.name}")
            return false
        }
    }

    private fun generateSvg(inputMmd: File, outputSvg: File) {
        println("Generating SVG for ${inputMmd.name}...")
        project.exec {
            // Pass empty string as separate argument without quotes for Gradle to handle
            commandLine("npx", "-y", "@mermaid-js/mermaid-cli", "-i", inputMmd.absolutePath, "-o", outputSvg.absolutePath, "-t", "default", "--cssFile", "")
        }
        println("Generated SVG at: ${outputSvg.absolutePath}")
    }
}
