package com.chriscartland.batterybutler.viewmodel

import kotlin.comparisons.naturalOrder
import kotlin.comparisons.reverseOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionExtensionsTest {
    @Test
    fun `toSortedMap sorts by natural order`() {
        val map = mapOf("c" to 3, "a" to 1, "b" to 2)

        val result = map.toSortedMap(naturalOrder())

        assertEquals(listOf("a", "b", "c"), result.keys.toList())
    }

    @Test
    fun `toSortedMap sorts by reverse order`() {
        val map = mapOf("c" to 3, "a" to 1, "b" to 2)

        val result = map.toSortedMap(reverseOrder())

        assertEquals(listOf("c", "b", "a"), result.keys.toList())
    }

    @Test
    fun `sortAndGroup with no grouping returns single group`() {
        val result = sortAndGroup(
            items = listOf(3, 1, 2),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = null,
            defaultGroupName = "All",
            isGroupAscending = true,
        )

        assertEquals(mapOf("All" to listOf(1, 2, 3)), result)
    }

    @Test
    fun `sortAndGroup descending reverses order`() {
        val result = sortAndGroup(
            items = listOf(1, 2, 3),
            sortComparator = naturalOrder(),
            isSortAscending = false,
            groupKeySelector = null,
            defaultGroupName = "All",
            isGroupAscending = true,
        )

        assertEquals(mapOf("All" to listOf(3, 2, 1)), result)
    }

    @Test
    fun `sortAndGroup with grouping separates items`() {
        val result = sortAndGroup(
            items = listOf("a1", "b1", "a2"),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = { it.first().toString() },
            defaultGroupName = "default",
            isGroupAscending = true,
        )

        assertEquals(listOf("a1", "a2"), result["a"])
        assertEquals(listOf("b1"), result["b"])
    }

    @Test
    fun `sortAndGroup with descending groups reverses group order`() {
        val result = sortAndGroup(
            items = listOf("a1", "b1", "a2"),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = { it.first().toString() },
            defaultGroupName = "default",
            isGroupAscending = false,
        )

        assertEquals(listOf("b", "a"), result.keys.toList())
    }
}
