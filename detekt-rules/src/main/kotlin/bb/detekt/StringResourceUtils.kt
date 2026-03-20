package bb.detekt

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Returns true if [expression] is a pure string literal (no template entries like ${} or $var).
 */
internal fun isStringLiteral(expression: KtExpression?): Boolean {
    if (expression !is KtStringTemplateExpression) return false
    return expression.entries.all { it is KtLiteralStringTemplateEntry }
}

/**
 * Returns true if [element] is inside a function annotated with @Preview.
 */
internal fun isInPreviewFunction(element: KtElement): Boolean {
    var current = element.parent
    while (current != null) {
        if (current is KtNamedFunction) {
            return current.annotationEntries.any { annotation ->
                val name = annotation.typeReference?.text
                name == "Preview" || name?.endsWith(".Preview") == true
            }
        }
        current = current.parent
    }
    return false
}

/**
 * Returns true if @StringResourceExempt appears on any line spanned by [element].
 */
internal fun hasExemptComment(element: KtElement): Boolean {
    val fileText = element.containingFile?.text ?: return false
    val startOffset = element.textOffset
    val endOffset = startOffset + element.textLength
    val firstLineStart = fileText.lastIndexOf('\n', startOffset - 1) + 1
    val lastLineEnd = fileText.indexOf('\n', endOffset).let { if (it < 0) fileText.length else it }
    return "@StringResourceExempt" in fileText.substring(firstLineStart, lastLineEnd)
}
