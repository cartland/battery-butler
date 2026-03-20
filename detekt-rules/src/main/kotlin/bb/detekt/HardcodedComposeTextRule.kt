package bb.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression

class HardcodedComposeTextRule(
    config: Config = Config.empty,
) : Rule(config) {
    override val issue = Issue(
        id = "HardcodedComposeText",
        severity = Severity.Warning,
        description = "Compose Text() uses a hardcoded string literal instead of a string resource.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val calleeName = expression.calleeExpression?.text ?: return
        if (calleeName != "Text") return
        if (isInPreviewFunction(expression)) return
        if (hasExemptComment(expression)) return

        // Find the text argument: named "text" param, or first positional arg
        val textArg = expression.valueArguments.firstOrNull { arg ->
            arg.getArgumentName()?.asName?.asString() == "text"
        } ?: expression.valueArguments.firstOrNull { arg ->
            arg.getArgumentName() == null
        } ?: return

        val argExpression = textArg.getArgumentExpression() ?: return
        if (isStringLiteral(argExpression)) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Use composeStringResource(Res.string.xxx) instead of a hardcoded string in Text(). " +
                        "Add // @StringResourceExempt on the line if intentional.",
                ),
            )
        }
    }
}
