package previewtime

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Checks that @Preview functions do not transitively use Clock.System.now().
 *
 * Pass 1: Collect "time-sensitive" composables — functions with Clock.System.now()
 *         (or similar) as a default parameter value.
 *
 * Pass 2: For each @Preview function, BFS through the call chain looking for calls
 *         to time-sensitive composables where the time parameter is not explicitly passed.
 *         This catches chains like: Preview → Content → Body → DeviceListItem(nowInstant missing).
 *
 * Suppress: Add `// @PreviewTimeExempt` on the call line to suppress.
 */
open class PreviewTimeCheckTask : DefaultTask() {
    private data class Violation(
        val file: String,
        val line: Int,
        val chain: List<String>,
        val missingParam: String,
        val targetFunc: String,
    )

    private data class ParsedFunction(
        val name: String,
        val relativePath: String,
        val startLine: Int,
        val bodyLines: List<String>,
        val isPreview: Boolean,
        val isComposable: Boolean,
    )

    private data class FunctionCall(
        val calledName: String,
        val callLine: Int,
        val argText: String,
    )

    private val clockPatterns = listOf(
        Regex("""Clock\s*\.\s*System\s*\.\s*now\s*\(\s*\)"""),
        Regex("""System\s*\.\s*currentTimeMillis\s*\(\s*\)"""),
        Regex("""Calendar\s*\.\s*getInstance\s*\(\s*\)"""),
        Regex("""LocalDate\s*\.\s*now\s*\(\s*\)"""),
        Regex("""LocalDateTime\s*\.\s*now\s*\(\s*\)"""),
        Regex("""Instant\s*\.\s*now\s*\(\s*\)"""),
    )

    private val exemptComment = "@PreviewTimeExempt"
    private val skipDirs = setOf("build", "test", ".gradle", ".bazel")
    private val maxDepth = 6

    @TaskAction
    fun check() {
        val rootDir = project.rootDir
        val kotlinFiles = rootDir
            .walkTopDown()
            .onEnter { dir -> dir.name !in skipDirs }
            .filter { it.extension == "kt" && !it.path.contains("/experimental/") }
            .toList()

        // Pass 1: Collect time-sensitive composables.
        val timeSensitive = mutableMapOf<String, MutableSet<String>>()
        kotlinFiles.forEach { file -> collectTimeSensitive(file.readLines(), timeSensitive) }

        if (timeSensitive.isEmpty()) {
            logger.lifecycle("Preview time check passed: no time-sensitive composables found.")
            return
        }

        logger.lifecycle(
            "Found ${timeSensitive.size} time-sensitive composable(s): " +
                timeSensitive.entries.joinToString { "${it.key}(${it.value.joinToString()})" },
        )

        // Parse all composable functions across the codebase.
        val allFunctions = mutableListOf<ParsedFunction>()
        kotlinFiles.forEach { file ->
            val lines = file.readLines()
            val rel = file.relativeTo(rootDir).path
            allFunctions.addAll(parseFunctions(lines, rel))
        }

        val functionsByName = allFunctions.groupBy { it.name }
        val previewFunctions = allFunctions.filter { it.isPreview }

        // Pass 2: BFS from each @Preview through the call chain.
        val violations = mutableListOf<Violation>()
        for (preview in previewFunctions) {
            checkPreview(preview, timeSensitive, functionsByName, violations)
        }

        if (violations.isNotEmpty()) {
            // Deduplicate by (file, line, targetFunc, missingParam)
            val unique = violations.distinctBy { Triple(it.file, it.line, "${it.targetFunc}:${it.missingParam}") }

            val report = buildString {
                appendLine()
                appendLine("=== Preview Time Check: ${unique.size} violation(s) found ===")
                appendLine()
                unique.forEach { v ->
                    val chain = v.chain.joinToString(" → ")
                    appendLine("${v.file}:${v.line} [preview-uses-clock-now]")
                    appendLine("  Chain: $chain")
                    appendLine("  '${v.targetFunc}' is called without '${v.missingParam}', defaulting to Clock.System.now().")
                    appendLine("  Fix: Thread '${v.missingParam}' as an explicit parameter through the call chain.")
                    appendLine("  Suppress: Add // $exemptComment on the call line if intentional.")
                    appendLine()
                }
                appendLine("@Preview functions must not transitively use Clock.System.now().")
                appendLine("Pass a fixed Instant to ensure deterministic screenshot tests.")
            }
            throw GradleException(report)
        }

        logger.lifecycle("Preview time check passed: ${previewFunctions.size} preview(s) checked, no violations.")
    }

    // ── Pass 1 ──────────────────────────────────────────────────────────────

    private fun collectTimeSensitive(
        lines: List<String>,
        result: MutableMap<String, MutableSet<String>>,
    ) {
        var inComposableSignature = false
        var funcName = ""
        var parenDepth = 0

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()

            if (trimmed.startsWith("@Composable")) {
                for (ahead in (index + 1) until minOf(index + 5, lines.size)) {
                    val funMatch = Regex("""fun\s+(\w+)\s*\(""").find(lines[ahead].trim())
                    if (funMatch != null) {
                        inComposableSignature = true
                        funcName = funMatch.groupValues[1]
                        parenDepth = 0
                        break
                    }
                }
            }

            if (inComposableSignature) {
                for (ch in line) {
                    if (ch == '(') parenDepth++
                    if (ch == ')') parenDepth--
                }

                if (line.contains("=") && !line.contains(exemptComment)) {
                    for (pattern in clockPatterns) {
                        if (pattern.containsMatchIn(line)) {
                            val paramName = Regex("""(\w+)\s*:""")
                                .find(line.trim())
                                ?.groupValues
                                ?.get(1) ?: "unknown"
                            result.getOrPut(funcName) { mutableSetOf() }.add(paramName)
                        }
                    }
                }

                if (parenDepth <= 0 && line.contains(")")) {
                    inComposableSignature = false
                }
            }
        }
    }

    // ── Function parsing ────────────────────────────────────────────────────

    private fun parseFunctions(
        lines: List<String>,
        relativePath: String,
    ): List<ParsedFunction> {
        val functions = mutableListOf<ParsedFunction>()
        var i = 0

        while (i < lines.size) {
            var isPreview = false
            var isComposable = false
            val annotationStart = i

            // Collect annotations before a fun declaration.
            while (i < lines.size) {
                val t = lines[i].trim()
                if (t.startsWith("@Preview")) isPreview = true
                if (t.startsWith("@Composable")) isComposable = true
                if (t.startsWith("fun ") || t.contains(" fun ")) break
                if (!t.startsWith("@") && !t.startsWith("//") && !t.startsWith("/*") &&
                    !t.startsWith("*") && t.isNotEmpty()
                ) {
                    isPreview = false
                    isComposable = false
                    break
                }
                i++
            }

            if (i >= lines.size) break

            val funMatch = Regex("""fun\s+(\w+)\s*[\(<]""").find(lines[i].trim())
            if (funMatch == null || (!isComposable && !isPreview)) {
                i++
                continue
            }

            val funcName = funMatch.groupValues[1]

            // Find opening brace.
            var braceStart = i
            while (braceStart < lines.size && !lines[braceStart].contains("{")) braceStart++
            if (braceStart >= lines.size) {
                i++
                continue
            }

            // Track brace depth to find function end.
            var braceDepth = 0
            var funcEndLine = braceStart
            for (j in braceStart until lines.size) {
                for (ch in lines[j]) {
                    if (ch == '{') braceDepth++
                    if (ch == '}') braceDepth--
                }
                if (braceDepth <= 0) {
                    funcEndLine = j
                    break
                }
            }

            functions.add(
                ParsedFunction(
                    name = funcName,
                    relativePath = relativePath,
                    startLine = annotationStart + 1,
                    bodyLines = lines.subList(braceStart, funcEndLine + 1),
                    isPreview = isPreview,
                    isComposable = isComposable || isPreview,
                ),
            )

            i = funcEndLine + 1
        }

        return functions
    }

    // ── Call extraction ─────────────────────────────────────────────────────

    private fun findCalls(
        bodyLines: List<String>,
        baseLineNum: Int,
    ): List<FunctionCall> {
        val calls = mutableListOf<FunctionCall>()
        val joined = bodyLines.joinToString("\n")
        val callPattern = Regex("""([A-Z]\w+)\s*\(""")

        var searchFrom = 0
        while (true) {
            val match = callPattern.find(joined, searchFrom) ?: break
            val calledName = match.groupValues[1]
            val openParenIndex = match.range.last

            var depth = 1
            var pos = openParenIndex + 1
            while (pos < joined.length && depth > 0) {
                if (joined[pos] == '(') depth++
                if (joined[pos] == ')') depth--
                pos++
            }

            val argText = joined.substring(openParenIndex + 1, maxOf(openParenIndex + 1, pos - 1))
            val callLine = baseLineNum + joined.substring(0, match.range.first).count { it == '\n' }

            calls.add(FunctionCall(calledName, callLine, argText))
            searchFrom = match.range.first + 1
        }

        return calls
    }

    // ── Pass 2: BFS from @Preview ───────────────────────────────────────────

    private data class BfsEntry(
        val func: ParsedFunction,
        val chain: List<String>,
    )

    private fun checkPreview(
        preview: ParsedFunction,
        timeSensitive: Map<String, Set<String>>,
        functionsByName: Map<String, List<ParsedFunction>>,
        violations: MutableList<Violation>,
    ) {
        val queue = ArrayDeque<BfsEntry>()
        queue.add(BfsEntry(preview, listOf(preview.name)))
        val visited = mutableSetOf(preview.name)

        while (queue.isNotEmpty()) {
            val entry = queue.removeFirst()
            if (entry.chain.size > maxDepth) continue

            val calls = findCalls(entry.func.bodyLines, entry.func.startLine)

            for (call in calls) {
                // Is this a call to a time-sensitive composable?
                val tsParams = timeSensitive[call.calledName]
                if (tsParams != null) {
                    for (param in tsParams) {
                        if (!isParamPassed(param, call.argText) && !call.argText.contains(exemptComment)) {
                            violations.add(
                                Violation(
                                    file = entry.func.relativePath,
                                    line = call.callLine,
                                    chain = entry.chain + call.calledName,
                                    missingParam = param,
                                    targetFunc = call.calledName,
                                ),
                            )
                        }
                    }
                    // Don't recurse into time-sensitive funcs (they're the leaf).
                    continue
                }

                // Not time-sensitive — explore further if it's a known composable.
                if (call.calledName in visited) continue
                visited.add(call.calledName)

                val calledFuncs = functionsByName[call.calledName] ?: continue
                for (calledFunc in calledFuncs) {
                    if (calledFunc.isComposable) {
                        queue.add(BfsEntry(calledFunc, entry.chain + call.calledName))
                    }
                }
            }
        }
    }

    private fun isParamPassed(
        paramName: String,
        argText: String,
    ): Boolean = Regex("""\b${Regex.escape(paramName)}\s*=""").containsMatchIn(argText)
}
