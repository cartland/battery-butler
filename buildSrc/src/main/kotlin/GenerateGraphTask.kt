import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

abstract class GenerateGraphTask : DefaultTask() {

    @get:OutputFile
    val outputFile: File = project.file("docs/diagrams/kotlin_module_structure.mmd")

    @get:OutputFile
    val svgFile: File = project.file("docs/diagrams/kotlin_module_structure.svg")

    init {
        notCompatibleWithConfigurationCache("Accesses project instance at execution time")
    }

    @TaskAction
    fun generate() {
        // Helpers
        val scanner = ProjectScanner(project)
        val generator = MermaidGenerator(project)

        val allProjects = project.rootProject.subprojects
        val activeModules = scanner.scanModules(allProjects)
        val dependencyEdges = scanner.scanDependencies(allProjects)
        
        // 1. Generate Standard Kotlin Graph
        generator.generateDiagram(
            activeModules = activeModules,
            dependencyEdges = dependencyEdges,
            outputMmd = outputFile,
            outputSvg = svgFile,
            includeIos = false
        )

        // 2. Generate Full System Graph (with iOS)
        val fullInfoMmd = project.file("docs/diagrams/full_system_structure.mmd")
        val fullInfoSvg = project.file("docs/diagrams/full_system_structure.svg")
        generator.generateDiagram(
            activeModules = activeModules,
            dependencyEdges = dependencyEdges,
            outputMmd = fullInfoMmd,
            outputSvg = fullInfoSvg,
            includeIos = true
        )
    }
}
