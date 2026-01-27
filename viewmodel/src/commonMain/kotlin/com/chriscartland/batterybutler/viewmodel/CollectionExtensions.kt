package com.chriscartland.batterybutler.viewmodel

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
