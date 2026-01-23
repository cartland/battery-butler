package codeshareanalysis

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.File

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
        // Sort buckets by size or predefined order? 
        // User's example had specific order. 
        // Let's sort by size descending to match the "Module Breakdown" request, or default config order?
        // User request: "Application Breakdown... predefined categories"
        // Let's stick to config order if possible, or Size. 
        // User Example: Shared (68%), CMP (13%), iOS (0.2%). It seems Size descending.
        
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

    private fun calculatePercentage(part: Int, total: Int): String {
        if (total == 0) return "0.0%"
        val pct = (part.toDouble() / total.toDouble()) * 100
        return "%.1f%%".format(pct)
    }

    private fun formatNumber(num: Int): String {
        return String.format("%,d", num)
    }
}
