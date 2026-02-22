package com.chriscartland.batterybutler.viewmodel

import kotlin.comparisons.naturalOrder
import kotlin.comparisons.reverseOrder

/**
 * KMP-compatible extension to sort a map by its keys using a custom comparator.
 *
 * This is a replacement for the JVM-specific `toSortedMap()` that isn't available
 * in Kotlin Multiplatform common code (it returns java.util.SortedMap).
 *
 * @param comparator The comparator to use for sorting keys
 * @return A new map with entries sorted by key according to the comparator
 */
fun <K : Comparable<K>, V> Map<K, V>.toSortedMap(comparator: Comparator<K>): Map<K, V> =
    this.entries
        .sortedWith(Comparator { a, b -> comparator.compare(a.key, b.key) })
        .associate { it.key to it.value }

/**
 * Sorts items, optionally reverses them, and groups them into a map.
 *
 * @param items The list of items to sort and group.
 * @param sortComparator Comparator for sorting items within groups.
 * @param isSortAscending If false, the sorted order is reversed.
 * @param groupKeySelector Extracts the group key from each item, or null for no grouping.
 * @param defaultGroupName Label used when [groupKeySelector] is null (all items in one group).
 * @param isGroupAscending If true, group keys are sorted in natural order; otherwise reversed.
 * @return An ordered map of group-name to sorted items.
 */
fun <T> sortAndGroup(
    items: List<T>,
    sortComparator: Comparator<T>,
    isSortAscending: Boolean,
    groupKeySelector: ((T) -> String)?,
    defaultGroupName: String,
    isGroupAscending: Boolean,
): Map<String, List<T>> {
    var sorted = items.sortedWith(sortComparator)
    if (!isSortAscending) {
        sorted = sorted.reversed()
    }
    if (groupKeySelector == null) {
        return mapOf(defaultGroupName to sorted)
    }
    val grouped = sorted.groupBy(groupKeySelector)
    val comparator = if (isGroupAscending) naturalOrder<String>() else reverseOrder()
    return grouped.toSortedMap(comparator)
}
