package testcoverage

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File

open class TestCoverageCheckTask : DefaultTask() {
    private data class ModuleRule(
        val module: String,
        val classPatterns: List<Regex>,
        val sourceSets: List<String>,
        val testSourceSets: List<String>,
    )

    private data class ClassInfo(
        val name: String,
        val relativePath: String,
        val lineNumber: Int,
        val module: String,
    )

    private data class Exemption(
        val pattern: String,
        val reason: String,
    )

    @TaskAction
    fun check() {
        val rootDir = project.rootDir

        // 1. Define module rules
        val rules = listOf(
            ModuleRule(
                module = "usecase",
                classPatterns = listOf(Regex("""\w*UseCase""")),
                sourceSets = listOf("commonMain"),
                testSourceSets = listOf("commonTest", "jvmTest"),
            ),
            ModuleRule(
                module = "viewmodel",
                classPatterns = listOf(Regex("""\w*ViewModel""")),
                sourceSets = listOf("commonMain"),
                testSourceSets = listOf("commonTest", "desktopTest"),
            ),
            ModuleRule(
                module = "data",
                classPatterns = listOf(Regex("""Default\w+""")),
                sourceSets = listOf("commonMain"),
                testSourceSets = listOf("commonTest"),
            ),
            ModuleRule(
                module = "ai",
                classPatterns = listOf(
                    Regex("""\w*AiEngine"""),
                    Regex("""\w*AiConfig"""),
                ),
                sourceSets = listOf("commonMain", "androidMain"),
                testSourceSets = listOf("commonTest"),
            ),
            // Experimental modules — teaching reference, must maintain full coverage
            ModuleRule(
                module = "experimental/usecase",
                classPatterns = listOf(Regex("""\w*UseCase""")),
                sourceSets = listOf("commonMain"),
                testSourceSets = listOf("commonTest"),
            ),
            ModuleRule(
                module = "experimental/viewmodel",
                classPatterns = listOf(Regex("""\w*ViewModel""")),
                sourceSets = listOf("commonMain"),
                testSourceSets = listOf("commonTest"),
            ),
            ModuleRule(
                module = "experimental/data-local",
                classPatterns = listOf(Regex("""Default\w+""")),
                sourceSets = listOf("commonMain"),
                testSourceSets = listOf("commonTest"),
            ),
        )

        // 2. Parse central exemptions file
        val exemptionsFile = File(rootDir, "test-coverage-exemptions.txt")
        val exemptions = parseExemptions(exemptionsFile)

        // 3. Scan sources and cross-reference tests
        val allClasses = mutableListOf<ClassInfo>()
        val coveredClasses = mutableListOf<ClassInfo>()
        val exemptedClasses = mutableListOf<Pair<ClassInfo, String>>()
        val uncoveredClasses = mutableListOf<ClassInfo>()

        val classRegex = Regex(
            """(?:data|abstract|open|internal|sealed|value|private)?\s*class\s+(\w+)""",
        )

        val excludedDirs = setOf("di", "provider")
        val excludedSuffixes = listOf("Factory", "Component")
        val excludedNames = setOf("KmpViewModelStore")

        for (rule in rules) {
            for (sourceSet in rule.sourceSets) {
                val srcDir = File(rootDir, "${rule.module}/src/$sourceSet/kotlin")
                if (!srcDir.exists()) continue

                srcDir
                    .walk()
                    .filter { it.isFile && it.extension == "kt" }
                    .filter { !it.name.contains("$") }
                    .filter { file ->
                        val relToSrc = file.relativeTo(srcDir).parentFile?.path ?: ""
                        excludedDirs.none { dir ->
                            relToSrc.split(File.separatorChar).contains(dir)
                        }
                    }.forEach { file ->
                        val lines = file.readLines()
                        val text = lines.joinToString("\n")

                        classRegex.findAll(text).forEach classMatch@{ match ->
                            val className = match.groupValues[1]

                            // Check if matches enforced pattern
                            if (!rule.classPatterns.any { it.matches(className) }) return@classMatch

                            // Check hard-coded exclusions
                            if (className in excludedNames) return@classMatch
                            if (excludedSuffixes.any { className.endsWith(it) }) return@classMatch

                            val lineNumber =
                                text.substring(0, match.range.first).count { it == '\n' } + 1
                            val relPath = file.relativeTo(rootDir).path

                            val classInfo = ClassInfo(
                                name = className,
                                relativePath = relPath,
                                lineNumber = lineNumber,
                                module = rule.module,
                            )
                            allClasses.add(classInfo)

                            // 4. Check inline suppression
                            if (hasInlineSuppression(lines, lineNumber - 1)) {
                                val reason = extractSuppressionReason(lines, lineNumber - 1)
                                exemptedClasses.add(classInfo to reason)
                                return@classMatch
                            }

                            // Check central exemptions
                            val exemption = findExemption(className, exemptions)
                            if (exemption != null) {
                                exemptedClasses.add(classInfo to exemption.reason)
                                return@classMatch
                            }

                            // 5. Cross-reference tests
                            val hasTest = rule.testSourceSets.any { testSourceSet ->
                                val testDir =
                                    File(rootDir, "${rule.module}/src/$testSourceSet/kotlin")
                                testDir.exists() &&
                                    testDir.walk().any { testFile ->
                                        testFile.isFile &&
                                            testFile.nameWithoutExtension == "${className}Test"
                                    }
                            }

                            if (hasTest) {
                                coveredClasses.add(classInfo)
                            } else {
                                uncoveredClasses.add(classInfo)
                            }
                        }
                    }
            }
        }

        // 6. Generate report
        val reportFile = File(rootDir, "docs/Test_Coverage_Report.md")
        reportFile.parentFile.mkdirs()
        reportFile.writeText(buildReport(allClasses, coveredClasses, exemptedClasses, uncoveredClasses))

        // 7. Print summary and fail on gaps
        val total = allClasses.size
        val coveredCount = coveredClasses.size
        val exemptedCount = exemptedClasses.size
        val uncoveredCount = uncoveredClasses.size
        println(
            "Test Coverage: $coveredCount tested + $exemptedCount exempt" +
                " / $total total ($uncoveredCount uncovered)",
        )

        if (uncoveredClasses.isNotEmpty()) {
            val msg = uncoveredClasses.joinToString("\n\n") { cls ->
                "${cls.relativePath}:${cls.lineNumber} [test-coverage]\n" +
                    "  Class '${cls.name}' has no corresponding test file.\n" +
                    "  Fix: Create '${cls.name}Test.kt' in a test source set,\n" +
                    "       or add '// @NoTestRequired: <reason>' above the class."
            }
            throw GradleException(
                "Test coverage gap: $uncoveredCount class(es) missing tests:\n\n$msg",
            )
        }
        println("All enforced classes have tests or exemptions!")
    }

    private fun hasInlineSuppression(
        lines: List<String>,
        classLineIndex: Int,
    ): Boolean {
        for (i in (classLineIndex - 1) downTo maxOf(0, classLineIndex - 10)) {
            val trimmed = lines[i].trim()
            if (trimmed.startsWith("// @NoTestRequired:")) return true
            if (trimmed.startsWith("@") ||
                trimmed.startsWith("//") ||
                trimmed.startsWith("/*") ||
                trimmed.startsWith("*")
            ) {
                continue
            }
            if (trimmed.isEmpty()) break
            break
        }
        return false
    }

    private fun extractSuppressionReason(
        lines: List<String>,
        classLineIndex: Int,
    ): String {
        for (i in (classLineIndex - 1) downTo maxOf(0, classLineIndex - 10)) {
            val trimmed = lines[i].trim()
            if (trimmed.startsWith("// @NoTestRequired:")) {
                return trimmed.removePrefix("// @NoTestRequired:").trim()
            }
            if (trimmed.startsWith("@") ||
                trimmed.startsWith("//") ||
                trimmed.startsWith("/*") ||
                trimmed.startsWith("*")
            ) {
                continue
            }
            if (trimmed.isEmpty()) break
            break
        }
        return "unknown"
    }

    private fun parseExemptions(file: File): List<Exemption> {
        if (!file.exists()) return emptyList()
        return file
            .readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split("|", limit = 2)
                if (parts.size == 2) {
                    Exemption(
                        pattern = parts[0].trim(),
                        reason = parts[1].trim(),
                    )
                } else {
                    null
                }
            }
    }

    private fun findExemption(
        className: String,
        exemptions: List<Exemption>,
    ): Exemption? =
        exemptions.firstOrNull { exemption ->
            if (exemption.pattern.contains("*")) {
                val regex = Regex(exemption.pattern.replace("*", ".*"))
                regex.matches(className)
            } else {
                exemption.pattern == className
            }
        }

    @Suppress("LongMethod")
    private fun buildReport(
        all: List<ClassInfo>,
        covered: List<ClassInfo>,
        exempted: List<Pair<ClassInfo, String>>,
        uncovered: List<ClassInfo>,
    ): String {
        val total = all.size
        val coveredCount = covered.size
        val exemptedCount = exempted.size
        val pct = if (total > 0) ((coveredCount + exemptedCount) * 100 / total) else 100

        return buildString {
            appendLine("# Test Coverage Report")
            appendLine()
            appendLine(
                "**Coverage: $coveredCount tested + $exemptedCount exempt" +
                    " / $total total ($pct%)**",
            )
            appendLine()

            if (uncovered.isNotEmpty()) {
                appendLine("## Uncovered Classes")
                appendLine()
                uncovered.sortedBy { it.name }.forEach { cls ->
                    appendLine("- `${cls.name}` in `${cls.relativePath}:${cls.lineNumber}`")
                }
                appendLine()
            }

            appendLine("## Covered Classes")
            appendLine()
            if (covered.isEmpty()) {
                appendLine("_None_")
            } else {
                covered.sortedBy { it.name }.forEach { cls ->
                    appendLine("- `${cls.name}` in `${cls.relativePath}`")
                }
            }
            appendLine()

            if (exempted.isNotEmpty()) {
                appendLine("## Exempted Classes")
                appendLine()
                exempted.sortedBy { it.first.name }.forEach { (cls, reason) ->
                    appendLine("- `${cls.name}` — $reason")
                }
            }
        }
    }
}
