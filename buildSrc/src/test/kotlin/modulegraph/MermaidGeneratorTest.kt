package modulegraph

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MermaidGeneratorTest {
    private val simpleConfig = GraphConfig(
        moduleGroups = mapOf(
            ":app" to "App Layer",
            ":domain" to "Domain Layer",
        ),
        groupPrefixes = emptyMap(),
        groupOrder = listOf("App Layer", "Domain Layer"),
        outputPaths = GraphConfig.OutputPaths("", "", "", ""),
        mermaidCli = GraphConfig.MermaidCli("", "", emptyList()),
        scanner = GraphConfig.ScannerConfig(
            emptyList(),
            GraphConfig.XcodeConfig(0, emptyList(), "", "", "", ""),
        ),
    )

    private val generator = MermaidGenerator(simpleConfig)

    @Test
    fun `output starts with graph TD`() {
        val data = GraphData(modules = setOf(":app"), edges = emptySet())
        val output = generator.generateContent(data)
        assertEquals("graph TD", output.lines().first())
    }

    @Test
    fun `contains node definitions for provided modules`() {
        val data = GraphData(modules = setOf(":app", ":domain"), edges = emptySet())
        val output = generator.generateContent(data)
        assertTrue(output.contains("App[\":app\"]"), "Should contain node for :app")
        assertTrue(output.contains("Domain[\":domain\"]"), "Should contain node for :domain")
    }

    @Test
    fun `contains edge definitions for provided dependencies`() {
        val data = GraphData(
            modules = setOf(":app", ":domain"),
            edges = setOf(":app" to ":domain"),
        )
        val output = generator.generateContent(data)
        assertTrue(output.contains("App --> Domain"), "Should contain edge from :app to :domain")
    }

    @Test
    fun `modules are grouped into subgraphs`() {
        val data = GraphData(modules = setOf(":app", ":domain"), edges = emptySet())
        val output = generator.generateContent(data)
        assertTrue(output.contains("subgraph \"App Layer\""), "Should have App Layer subgraph")
        assertTrue(output.contains("subgraph \"Domain Layer\""), "Should have Domain Layer subgraph")
    }

    @Test
    fun `handles empty input gracefully`() {
        val data = GraphData(modules = emptySet(), edges = emptySet())
        val output = generator.generateContent(data)
        assertEquals("graph TD", output.lines().first())
        assertTrue(output.contains("%% Dependencies"), "Should still have dependencies section")
    }

    @Test
    fun `unmapped module goes to default group`() {
        val data = GraphData(modules = setOf(":unknown-module"), edges = emptySet())
        val output = generator.generateContent(data)
        assertTrue(
            output.contains("subgraph \"Others\""),
            "Unmapped module should appear in 'Others' group",
        )
    }
}
