package codeshareanalysis

import java.time.Instant
import java.time.format.DateTimeFormatter

class ReportGenerator {
    fun generate(result: CodeScanner.ScanResult): String {
        val sb = StringBuilder()

        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

        sb.appendLine("# Code Share Analysis")
        sb.appendLine()
        sb.appendLine("This document provides a breakdown of the codebase by application layer and module.")
        sb.appendLine("Total Lines of Code: ${result.totalLines}")
        sb.appendLine("Generated at: $timestamp")
        sb.appendLine()

        sb.appendLine("## Application Breakdown")
        val sortedBuckets = result.bucketCounts.toList().sortedByDescending { it.second }

        sortedBuckets.forEach { (name, count) ->
            val percentage = calculatePercentage(count, result.totalLines)
            sb.appendLine("* $name: ${formatNumber(count)} lines ($percentage)")
        }
        sb.appendLine()

        sb.appendLine("## Module Breakdown")
        val sortedModules = result.moduleCounts.toList().sortedByDescending { it.second }
        sortedModules.forEach { (name, count) ->
            val percentage = calculatePercentage(count, result.totalLines)
            sb.appendLine("* `$name`: ${formatNumber(count)} lines ($percentage)")
        }

        return sb.toString()
    }

    private fun calculatePercentage(
        part: Int,
        total: Int,
    ): String {
        if (total == 0) return "0.0%"
        val pct = (part.toDouble() / total.toDouble()) * 100
        return "%.1f%%".format(pct)
    }

    private fun formatNumber(num: Int): String = String.format("%,d", num)
}
