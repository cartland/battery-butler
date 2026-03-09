package codeanalysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MermaidEmbedderTest {
    @TempDir
    lateinit var tempDir: File

    private fun createFile(content: String): File {
        val file = File(tempDir, "test.md")
        file.writeText(content)
        return file
    }

    @Test
    fun `embeds content between markers`() {
        val file = createFile(
            """
            |# Title
            |<!-- GENERATED:BEGIN chart.mmd -->
            |<!-- GENERATED:END chart.mmd -->
            |# Footer
            """.trimMargin() + "\n",
        )

        MermaidEmbedder.embed(file, "chart.mmd", "graph TD\n    A --> B")

        val result = file.readText()
        assert(result.contains("```mermaid")) { "Should contain mermaid code block" }
        assert(result.contains("graph TD")) { "Should contain graph content" }
        assert(result.contains("A --> B")) { "Should contain graph edges" }
        assert(result.contains("# Title")) { "Should preserve title" }
        assert(result.contains("# Footer")) { "Should preserve footer" }
    }

    @Test
    fun `preserves content outside markers`() {
        val file = createFile(
            """
            |Line 1
            |Line 2
            |<!-- GENERATED:BEGIN chart.mmd -->
            |<!-- GENERATED:END chart.mmd -->
            |Line 3
            |Line 4
            """.trimMargin() + "\n",
        )

        MermaidEmbedder.embed(file, "chart.mmd", "sankey-beta")

        val result = file.readText()
        assert(result.contains("Line 1")) { "Should preserve Line 1" }
        assert(result.contains("Line 2")) { "Should preserve Line 2" }
        assert(result.contains("Line 3")) { "Should preserve Line 3" }
        assert(result.contains("Line 4")) { "Should preserve Line 4" }
    }

    @Test
    fun `is idempotent`() {
        val file = createFile(
            """
            |<!-- GENERATED:BEGIN chart.mmd -->
            |<!-- GENERATED:END chart.mmd -->
            """.trimMargin() + "\n",
        )

        MermaidEmbedder.embed(file, "chart.mmd", "graph TD\n    A --> B")
        val firstResult = file.readText()

        MermaidEmbedder.embed(file, "chart.mmd", "graph TD\n    A --> B")
        val secondResult = file.readText()

        assertEquals(firstResult, secondResult, "Running embed twice should produce identical output")
    }

    @Test
    fun `no-op when markers are missing`() {
        val original = "# No markers here\nJust plain text\n"
        val file = createFile(original)

        MermaidEmbedder.embed(file, "chart.mmd", "graph TD\n    A --> B")

        assertEquals(original, file.readText(), "File should be unchanged when markers are missing")
    }

    @Test
    fun `handles graph TD content without frontmatter`() {
        val file = createFile(
            """
            |<!-- GENERATED:BEGIN structure.mmd -->
            |<!-- GENERATED:END structure.mmd -->
            """.trimMargin() + "\n",
        )

        val graphContent = "%% GENERATED FILE - DO NOT EDIT\ngraph TD\n    A --> B\n    B --> C"
        MermaidEmbedder.embed(file, "structure.mmd", graphContent)

        val result = file.readText()
        val lines = result.lines()
        val mermaidIdx = lines.indexOf("```mermaid")
        assert(mermaidIdx >= 0) { "Should have mermaid block" }
        assertEquals(
            "%% GENERATED FILE - DO NOT EDIT",
            lines[mermaidIdx + 1],
            "First line after mermaid marker should be the comment banner",
        )
    }

    @Test
    fun `replaces old content between markers on re-embed`() {
        val file = createFile(
            """
            |<!-- GENERATED:BEGIN chart.mmd -->
            |```mermaid
            |old content
            |```
            |<!-- GENERATED:END chart.mmd -->
            """.trimMargin() + "\n",
        )

        MermaidEmbedder.embed(file, "chart.mmd", "new content")

        val result = file.readText()
        assert(!result.contains("old content")) { "Old content should be removed" }
        assert(result.contains("new content")) { "New content should be present" }
    }

    @Test
    fun `handles multiple marker pairs without cross-contamination`() {
        val file = createFile(
            """
            |<!-- GENERATED:BEGIN first.mmd -->
            |<!-- GENERATED:END first.mmd -->
            |Middle text
            |<!-- GENERATED:BEGIN second.mmd -->
            |<!-- GENERATED:END second.mmd -->
            """.trimMargin() + "\n",
        )

        MermaidEmbedder.embed(file, "first.mmd", "first content")

        val result = file.readText()
        assert(result.contains("first content")) { "Should embed first content" }
        assert(result.contains("Middle text")) { "Should preserve middle text" }
        // The second markers should still be empty (no content injected)
        val secondBegin = result.indexOf("<!-- GENERATED:BEGIN second.mmd -->")
        val secondEnd = result.indexOf("<!-- GENERATED:END second.mmd -->")
        val between = result.substring(secondBegin + "<!-- GENERATED:BEGIN second.mmd -->".length, secondEnd).trim()
        assertEquals("", between, "Second marker pair should remain empty")
    }
}
