package codeanalysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SankeyChartGeneratorTest {
    private val minimalScanResult = CodeScanner.ScanResult(
        totalLines = 100,
        bucketCounts = mapOf("App" to 100),
        moduleCounts = mapOf(":app" to 100),
        bucketModuleCounts = mapOf("App" to mapOf(":app" to 100)),
        extensionCounts = mapOf("kt" to 100),
        moduleExtensionCounts = mapOf(":app" to mapOf("kt" to 100)),
    )

    @Test
    fun `generated output starts with frontmatter delimiter`() {
        val generator = SankeyChartGenerator()
        val output = generator.generate(minimalScanResult)
        val firstLine = output.lines().first()
        assertEquals("---", firstLine, "First line must be YAML frontmatter delimiter '---'")
    }

    @Test
    fun `no content appears before first frontmatter delimiter`() {
        val generator = SankeyChartGenerator()
        val output = generator.generate(minimalScanResult)
        val beforeFrontmatter = output.substringBefore("---").trim()
        assertTrue(
            beforeFrontmatter.isEmpty(),
            "Nothing should appear before the first '---' but found: '$beforeFrontmatter'",
        )
    }

    @Test
    fun `generated output contains sankey-beta keyword`() {
        val generator = SankeyChartGenerator()
        val output = generator.generate(minimalScanResult)
        assertTrue(output.contains("sankey-beta"), "Output must contain 'sankey-beta'")
    }

    @Test
    fun `comment appears after frontmatter block`() {
        val generator = SankeyChartGenerator()
        val output = generator.generate(minimalScanResult)
        val lines = output.lines()
        val closingFrontmatterIndex = lines.drop(1).indexOfFirst { it == "---" } + 1
        assertTrue(closingFrontmatterIndex > 0, "Must have closing frontmatter delimiter")
        val commentIndex = lines.indexOfFirst { it.startsWith("%%") }
        assertTrue(commentIndex > closingFrontmatterIndex, "Comment must appear after frontmatter block")
    }
}
