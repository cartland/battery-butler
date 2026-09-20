package com.chriscartland.batterybutler.viewmodel.util

/**
 * Sorts items, optionally reverses them, and groups them into a map.
 *
 * **Groups are ordered by the sort, not alphabetically.** Each group takes its priority from its
 * own first item, so with "sort by battery age" the group containing the most urgent device leads.
 * This falls out of [groupBy] preserving encounter order on the already-sorted list — the previous
 * behaviour sorted the group keys alphabetically afterwards, which threw that ordering away and
 * left the group order unrelated to the sort the user picked.
 *
 * @param items The list of items to sort and group.
 * @param sortComparator Comparator for sorting items within groups.
 * @param isSortAscending If false, the sorted order is reversed.
 * @param groupKeySelector Extracts the group key from each item, or null for no grouping.
 * @param defaultGroupName Label used when [groupKeySelector] is null (all items in one group).
 * @param isGroupAscending If true, groups keep the order the sort produced; otherwise that order
 *   is reversed. It no longer means "alphabetical", because group order no longer is.
 * @param priorityFirst Optional predicate for items that should lead regardless of the sort. Applied
 *   after the ascending/descending reversal and *before* grouping, so a prioritised item both leads
 *   its own group and pulls that group to the front. Used for the "needs a new battery" mark.
 * @return An ordered map of group-name to sorted items.
 */
fun <T> sortAndGroup(
    items: List<T>,
    sortComparator: Comparator<T>,
    isSortAscending: Boolean,
    groupKeySelector: ((T) -> String)?,
    defaultGroupName: String,
    isGroupAscending: Boolean,
    priorityFirst: ((T) -> Boolean)? = null,
): Map<String, List<T>> {
    var sorted = items.sortedWith(sortComparator)
    if (!isSortAscending) {
        sorted = sorted.reversed()
    }
    if (priorityFirst != null) {
        // Stable, so within the prioritised and non-prioritised halves the chosen sort survives.
        // Deliberately after the reversal above: folding this into the comparator instead would
        // invert it to "priority last" whenever the user flipped the sort direction.
        sorted = sorted.sortedByDescending(priorityFirst)
    }
    if (groupKeySelector == null) {
        return mapOf(defaultGroupName to sorted)
    }
    // groupBy preserves encounter order for both the keys and the values within each key, so the
    // sort above already decided the group order.
    val grouped = sorted.groupBy(groupKeySelector)
    if (isGroupAscending) {
        return grouped
    }
    return grouped.entries.reversed().associate { it.key to it.value }
}
