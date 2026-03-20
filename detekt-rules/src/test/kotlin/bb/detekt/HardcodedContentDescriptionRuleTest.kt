package bb.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HardcodedContentDescriptionRuleTest {
    private val rule = HardcodedContentDescriptionRule(Config.empty)

    @Test
    fun `reports hardcoded contentDescription`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assertEquals("HardcodedContentDescription", findings[0].id)
    }

    @Test
    fun `does not report variable contentDescription`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                val desc = stringResource(Res.string.close)
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = desc
                )
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `does not report null contentDescription`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null
                )
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
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
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
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close" // @StringResourceExempt
                )
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `does not report stringResource contentDescription`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.close)
                )
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `does not flag other named parameters`() {
        val code =
            """
            @Composable
            fun MyScreen() {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = "red"
                )
            }
            """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }
}
