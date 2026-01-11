import org.gradle.api.Project
import java.io.File

class AppleProjectScanner(private val rootProject: Project) {

    fun scanModules(): Set<String> {
        val modules = mutableSetOf<String>()
        val xcodeProjects = findXcodeProjects()
        
        xcodeProjects.forEach { xcodeProj ->
            // Use directory name without .xcodeproj as module name
            // e.g. ios-app-swift-ui/iosAppSwiftUI.xcodeproj -> iosAppSwiftUI
            // OR use the parent directory? The previous logic used 'ios-app-swift-ui' (the parent dir).
            // Let's stick to the directory name of the module that contains the .xcodeproj for now, 
            // OR just use the xcodeproj name if it maps well.
            // In the previous hardcoded version:
            // "ios-app-swift-ui"
            // "ios-app-compose-ui"
            // These are the FOLDER names in the root project.
            
            // Let's assume the Xcode project lives inside the module folder we want to track.
            // e.g. /path/to/root/ios-app-compose-ui/iosApp.xcodeproj
            // relative path: ios-app-compose-ui/iosApp.xcodeproj
            // module name: ios-app-compose-ui
            
            val moduleName = getModuleNameFromXcodePath(xcodeProj)
            modules.add(moduleName)
        }
        return modules
    }

    fun scanDependencies(): Set<Pair<String, String>> {
        val edges = mutableSetOf<Pair<String, String>>()
        val xcodeProjects = findXcodeProjects()

        xcodeProjects.forEach { xcodeProj ->
            val sourceModule = getModuleNameFromXcodePath(xcodeProj)
            val pbxProj = File(xcodeProj, "project.pbxproj")
            
            if (pbxProj.exists()) {
                val dependencies = parseGradleDependencies(pbxProj)
                dependencies.forEach { targetModule ->
                    edges.add(sourceModule to targetModule)
                }
            }
        }
        return edges
    }

    private fun findXcodeProjects(): Set<File> {
        // Find all directories ending in .xcodeproj
        val projects = mutableSetOf<File>()
        rootProject.rootDir.walkTopDown()
            .maxDepth(3) // Constraint depth to verify performance and avoid deep node_modules or build dirs
            .filter { it.isDirectory && it.name.endsWith(".xcodeproj") }
            .filter { !it.path.contains("/build/") && !it.path.contains("/node_modules/") }
            .forEach { projects.add(it) }
        return projects
    }

    private fun getModuleNameFromXcodePath(xcodeProj: File): String {
        // Strategy: Use the parent directory name relative to root project as the module name.
        // e.g. root/ios-app-compose-ui/iosApp.xcodeproj -> ios-app-compose-ui
        // But if the xcodeproj is in root, use its name?
        
        val relativePath = xcodeProj.relativeTo(rootProject.rootDir)
        val parent = relativePath.parentFile
        
        return if (parent == null || parent.path == ".") {
             xcodeProj.nameWithoutExtension
        } else {
             // If nested deeper? e.g. ios/app/My.xcodeproj
             // For now, assuming standard structure: module/Project.xcodeproj
             parent.name
        }
    }

    private fun parseGradleDependencies(pbxProj: File): Set<String> {
        val dependencies = mutableSetOf<String>()
        val regex = Regex("[:](.+):embedAndSignAppleFrameworkForXcode")
        
        pbxProj.readLines().forEach { line ->
            if (line.contains("embedAndSignAppleFrameworkForXcode")) {
                 val match = regex.find(line)
                 if (match != null) {
                     val gradleModule = match.groupValues[1]
                     dependencies.add(":$gradleModule")
                 }
            }
        }
        return dependencies
    }
}
