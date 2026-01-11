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
        // Helpers
        val scanner = ProjectScanner(project)
        val generator = MermaidGenerator()

        val allProjects = project.rootProject.subprojects
        val activeModules = scanner.scanModules(allProjects)
        val dependencyEdges = scanner.scanDependencies(allProjects)
        
        // Environment Facts
        val iosAppSwiftUiExists = project.rootProject.file("ios-app-swift-ui").exists()
        val iosAppComposeUiExists = project.rootProject.file("ios-app-compose-ui").exists()
        val logger = project.logger

        // Execution Logic
        val runMermaidCli: (File, File) -> Unit = { inputMmd, outputSvg ->
            project.exec {
                 // Pass empty string as separate argument without quotes for Gradle to handle
                 commandLine("npx", "-y", "@mermaid-js/mermaid-cli", "-i", inputMmd.absolutePath, "-o", outputSvg.absolutePath, "-t", "default", "--cssFile", "")
             }
        }

        // 1. Generate Standard Kotlin Graph
        generator.generateDiagram(
            activeModules = activeModules,
            dependencyEdges = dependencyEdges,
            outputMmd = kotlinModuleMmdFile,
            outputSvg = kotlinModuleSvgFile,
            includeIos = false,
            iosAppSwiftUiExists = iosAppSwiftUiExists,
            iosAppComposeUiExists = iosAppComposeUiExists,
            logger = logger,
            runMermaidCli = runMermaidCli
        )

        // 2. Generate Full System Graph (with iOS)
        generator.generateDiagram(
            activeModules = activeModules,
            dependencyEdges = dependencyEdges,
            outputMmd = fullSystemMmdFile,
            outputSvg = fullSystemSvgFile,
            includeIos = true,
            iosAppSwiftUiExists = iosAppSwiftUiExists,
            iosAppComposeUiExists = iosAppComposeUiExists,
            logger = logger,
            runMermaidCli = runMermaidCli
        )
    }
}
