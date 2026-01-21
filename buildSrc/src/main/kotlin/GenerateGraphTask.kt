import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateGraphTask : DefaultTask() {
    private val config = GraphConfig.default

    @get:OutputFile
    val kotlinModuleMmdFile: File = project.file(config.outputPaths.kotlinGraphMmd)

    @get:OutputFile
    val kotlinModuleSvgFile: File = project.file(config.outputPaths.kotlinGraphSvg)

    @get:OutputFile
    val fullSystemMmdFile: File = project.file(config.outputPaths.fullGraphMmd)

    @get:OutputFile
    val fullSystemSvgFile: File = project.file(config.outputPaths.fullGraphSvg)

    @get:org.gradle.api.tasks.InputFiles
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    val buildFiles: org.gradle.api.file.FileCollection = project.layout.files(
        project.rootProject.file("settings.gradle.kts"),
        project.rootProject.file("build.gradle.kts"),
        project.rootProject.file("gradle/libs.versions.toml"),
    ).plus(
        project.rootProject.subprojects.map { it.buildFile }
            .filter { it.exists() }
            .let { project.files(it) }
    )

    init {
        notCompatibleWithConfigurationCache("Accesses project instance at execution time")
    }

    @TaskAction
    fun generate() {
        val scanner = ProjectScanner(project, config)
        val generator = MermaidGenerator(config)
        val logger = project.logger

        val kotlinGraphData = scanner.scan(includeIos = false)
        val kotlinContent = generator.generateContent(kotlinGraphData, logger)
        val kotlinChanged = updateFile(kotlinModuleMmdFile, kotlinContent)

        if (kotlinChanged || !kotlinModuleSvgFile.exists()) {
            generateSvg(kotlinModuleMmdFile, kotlinModuleSvgFile)
        }

        val fullGraphData = scanner.scan(includeIos = true)
        val fullContent = generator.generateContent(fullGraphData, logger)
        val fullChanged = updateFile(fullSystemMmdFile, fullContent)

        if (fullChanged || !fullSystemSvgFile.exists()) {
            generateSvg(fullSystemMmdFile, fullSystemSvgFile)
        }
    }

    private fun updateFile(
        file: File,
        content: String,
    ): Boolean {
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

    private fun generateSvg(
        inputMmd: File,
        outputSvg: File,
    ) {
        val npxPath = listOf(
            "/Users/cartland/.nvm/versions/node/v24.2.0/bin/npx",
            "/usr/local/bin/npx",
            "/opt/homebrew/bin/npx",
            "/usr/bin/npx"
        ).firstOrNull { File(it).exists() } ?: "npx"

        println("Generating SVG for ${inputMmd.name} using $npxPath...")
        project.exec {
            // Pass empty string as separate argument without quotes for Gradle to handle
            commandLine(
                npxPath,
                "-y",
                "@mermaid-js/mermaid-cli@11.4.2",
                "-i",
                inputMmd.absolutePath,
                "-o",
                outputSvg.absolutePath,
                "-t",
                config.mermaidCli.theme,
                "--cssFile",
                if (config.mermaidCli.cssFile.isNotEmpty()) {
                     project.rootProject.file(config.mermaidCli.cssFile).absolutePath
                } else {
                     ""
                },
                "-p",
                project.rootProject.file(".config/puppeteer-config.json").absolutePath,
            )
        }
        println("Generated SVG at: ${outputSvg.absolutePath}")
    }
}
