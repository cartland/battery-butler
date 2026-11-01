package com.chriscartland.batterybutler.domain.model

/**
 * A user's saved sort/group choices for one list screen.
 *
 * Every field is nullable and null means "never chosen" — resolving that to a concrete default is
 * the caller's job, for the same reason [DisplayDensity.UNSPECIFIED] exists: the default then lives
 * in exactly one place (the ViewModel that renders the list) instead of being repeated at each
 * call site, and a stored value and an absent one stay the same type.
 *
 * The sort and group choices are held as opaque [String] tokens rather than enums because the
 * enums that describe them (`SortOption`, `GroupOption`, and the device-type list's own pair) live
 * in `:presentation-model`, which `:domain` must not depend on. Each ViewModel owns the mapping
 * between its enum and the token, which also means an unrecognised token — a downgrade, a hand-
 * edited store — degrades to the default rather than crashing.
 */
data class ListArrangement(
    val sortKey: String? = null,
    val groupKey: String? = null,
    val isSortAscending: Boolean? = null,
    val isGroupAscending: Boolean? = null,
)

/**
 * Which list screen a [ListArrangement] belongs to.
 *
 * Each list keeps its own arrangement — sorting devices by battery age says nothing about how you
 * want device types ordered. [storageId] is the on-disk prefix and is deliberately a literal rather
 * than `name`, so renaming a constant can't silently orphan every install's saved choice.
 */
enum class ListScreen(
    val storageId: String,
) {
    DEVICES("devices"),
    DEVICE_TYPES("device_types"),
}
