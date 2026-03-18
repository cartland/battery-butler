package bb.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HardcodedComposeTextRuleTest {
    private val rule = HardcodedComposeTextRule(Config.empty)

    @Test
    fun `reports hardcoded string in Text`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                Text("Hello")
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assertEquals("HardcodedComposeText", findings[0].id)
    }

    @Test
    fun `reports hardcoded named text parameter`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                Text(text = "Hello")
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report stringResource call`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                Text(stringResource(Res.string.hello))
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `does not report variable reference`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                val label = stringResource(Res.string.hello)
                Text(label)
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `exempts Preview functions`() {
        val code =
            """
            @Preview
            @Composable
            fun MyPreview() {
                Text("Hello")
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `exempts StringResourceExempt comment`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                Text("Hello") // @StringResourceExempt
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `reports string with template but no expressions`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                Text("Hello World")
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report string template with variable`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                val name = "World"
                Text("Hello ${'$'}name")
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `does not flag non-Text calls`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                Button("Click me")
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }
}
