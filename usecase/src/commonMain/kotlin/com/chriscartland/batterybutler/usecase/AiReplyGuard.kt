package com.chriscartland.batterybutler.usecase

/**
 * Deterministic cleanup of what a model wrote, so a poor one can't mislead the person.
 *
 * Only cards change data, so the checks here are about words: tool syntax that leaked into the
 * reply is removed, and a reply claiming a change that no card backs up gets a line saying so.
 * The check for claims is deliberately broad: its false positive is a true statement.
 */
internal object AiReplyGuard {
    private val FENCE = Regex("```.*?```", RegexOption.DOT_MATCHES_ALL)
    private val TOOL_JSON = Regex("""^\s*\{\s*"(tool|name|functionCall)"\s*:.*\}\s*$""", RegexOption.MULTILINE)
    private val TOOL_CALL = Regex("""^\s*(print\()?(default_api\.)?[a-z][A-Za-z]+\([a-zA-Z_]+\s*=.*\)\)?\s*$""", RegexOption.MULTILINE)
    private val CLAIM = Regex(
        """\b(i(?:'ve| have)?|has been|have been|was|were|is now|are now)\s+(?:\w+\s+)?(added|recorded|logged|deleted|removed|updated|changed|marked|cleared|renamed|moved)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun clean(text: String): String =
        text
            .replace(FENCE, "")
            .replace(TOOL_JSON, "")
            .replace(TOOL_CALL, "")
            .trim()

    fun claimsAChange(text: String): Boolean = CLAIM.containsMatchIn(text)
}
