package codeanalysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MermaidEmbedderTest {
    @TempDir
    lateinit var tempDir: File

    private fun createTargetFile(content: String): File {
        val file = File(tempDir, "target.md")
        file.writeText(content)
        return file
    }

    @Test
    fun `embeds content between markers`() {
        val target = createTargetFile(
            """
            |# Title
            |<!-- GENERATED:BEGIN chart.mmd -->
            |old content
            |<!-- GENERATED:END chart.mmd -->
            |# Footer
            """.trimMargin() + "\n",
        )

        MermaidEmbedder.embed(target, "chart.mmd", "graph TD\n    A --> B")

        val result = target.readText()
        assertTrue(result.contains("graph TD"), "Should contain embedded mermaid content")
        assertTrue(result.contains("A --> B"), "Should contain edge definition")
        assertTrue(!result.contains("old content"), "Should replace old content")
    }

    @Test
    fun `wraps content in mermaid fenced code block`() {
        val target = createTargetFile(
            """
            |<!-- GENERATED:BEGIN chart.mmd -->
            |<!-- GENERATED:END chart.mmd -->
            """.trimMargin() + "\n",
        )

        MermaidEmbedder.embed(target, "chart.mmd", "graph TD")

        val result = target.readText()
        assertTrue(result.contains("```mermaid"), "Should have mermaid code fence opener")
        assertTrue(result.contains("```\n"), "Should have code fence closer")
    }

    @Test
    fun `is idempotent`() {
        val target = createTargetFile(
            """
            |# Title
            |<!-- GENERATED:BEGIN chart.mmd -->
            |<!-- GENERATED:END chart.mmd -->
            |# Footer
            """.trimMargin() + "\n",
        )

        MermaidEmbedder.embed(target, "chart.mmd", "graph TD\n    A --> B")
        val firstResult = target.readText()

        MermaidEmbedder.embed(target, "chart.mmd", "graph TD\n    A --> B")
        val secondResult = target.readText()

        assertEquals(firstResult, secondResult, "Running embed twice should produce the same result")
    }

    @Test
    fun `preserves content outside markers`() {
        val target = createTargetFile(
            """
            |# Header
            |Some text above.
            |<!-- GENERATED:BEGIN chart.mmd -->
            |<!-- GENERATED:END chart.mmd -->
            |Some text below.
            |# Footer
            """.trimMargin() + "\n",
        )

        MermaidEmbedder.embed(target, "chart.mmd", "graph TD")

        val result = target.readText()
        assertTrue(result.contains("# Header"), "Should preserve header")
        assertTrue(result.contains("Some text above."), "Should preserve text above markers")
        assertTrue(result.contains("Some text below."), "Should preserve text below markers")
        assertTrue(result.contains("# Footer"), "Should preserve footer")
    }
}
