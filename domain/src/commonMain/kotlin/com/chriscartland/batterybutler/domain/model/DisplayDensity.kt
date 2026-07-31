package com.chriscartland.batterybutler.domain.model

/**
 * How densely list rows are drawn, app-wide.
 *
 * This is the **stored** form of the preference. [UNSPECIFIED] means the user has never chosen —
 * it is not a third layout. Anything rendering a list resolves it to a concrete density with
 * [orDefault], so the default lives in exactly one place and a future change of heart about the
 * fresh-install default does not have to be repeated at every call site.
 *
 * Keeping an explicit "not chosen" value (rather than a nullable) means a stored value and an
 * absent one are the same type, so the repository never has to decide whether `null` came from an
 * empty store or a corrupt entry.
 */
enum class DisplayDensity {
    UNSPECIFIED,
    COMPACT,
    EXPANDED,
    ;

    /**
     * Resolve to the density to actually draw with. [UNSPECIFIED] becomes [EXPANDED] — the
     * long-standing default before this preference existed, so upgrading installs see no change
     * until they opt in.
     */
    fun orDefault(): DisplayDensity = if (this == UNSPECIFIED) EXPANDED else this

    /** True when rows should use the condensed single-line layout. */
    val isCompact: Boolean
        get() = orDefault() == COMPACT
}
