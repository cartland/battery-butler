package bb.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression

class HardcodedContentDescriptionRule(
    config: Config = Config.empty,
) : Rule(config) {
    override val issue = Issue(
        id = "HardcodedContentDescription",
        severity = Severity.Warning,
        description = "contentDescription uses a hardcoded string literal instead of a string resource.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (isInPreviewFunction(expression)) return

        for (arg in expression.valueArguments) {
            val argName = arg.getArgumentName()?.asName?.asString() ?: continue
            if (argName == "contentDescription" && isStringLiteral(arg.getArgumentExpression())) {
                if (!hasExemptComment(arg)) {
                    report(
                        CodeSmell(
                            issue = issue,
                            entity = Entity.from(arg),
                            message = "Use composeStringResource(Res.string.xxx) instead of a hardcoded contentDescription. " +
                                "Add // @StringResourceExempt on the line if intentional.",
                        ),
                    )
                }
            }
        }
    }
}
