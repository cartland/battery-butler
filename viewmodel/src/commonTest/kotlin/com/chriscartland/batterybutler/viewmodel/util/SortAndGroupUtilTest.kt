package com.chriscartland.batterybutler.viewmodel.util

import kotlin.comparisons.naturalOrder
import kotlin.comparisons.reverseOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class SortAndGroupUtilTest {
    // --- toSortedMap tests ---

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

    // --- No grouping tests ---

    @Test
    fun `no grouping ascending sort returns single group sorted ascending`() {
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
    fun `no grouping descending sort returns single group sorted descending`() {
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
    fun `no grouping uses defaultGroupName as key`() {
        val result = sortAndGroup(
            items = listOf(1),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = null,
            defaultGroupName = "All Devices",
            isGroupAscending = true,
        )

        assertEquals(listOf("All Devices"), result.keys.toList())
    }

    @Test
    fun `no grouping isGroupAscending is ignored`() {
        val ascending = sortAndGroup(
            items = listOf(2, 1),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = null,
            defaultGroupName = "All",
            isGroupAscending = true,
        )
        val descending = sortAndGroup(
            items = listOf(2, 1),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = null,
            defaultGroupName = "All",
            isGroupAscending = false,
        )

        assertEquals(ascending, descending)
    }

    // --- With grouping tests ---

    @Test
    fun `grouping separates items by key`() {
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
    fun `grouping ascending groups are in natural order`() {
        val result = sortAndGroup(
            items = listOf("c1", "a1", "b1"),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = { it.first().toString() },
            defaultGroupName = "default",
            isGroupAscending = true,
        )

        assertEquals(listOf("a", "b", "c"), result.keys.toList())
    }

    @Test
    fun `grouping descending groups are in reverse order`() {
        val result = sortAndGroup(
            items = listOf("a1", "b1", "c1"),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = { it.first().toString() },
            defaultGroupName = "default",
            isGroupAscending = false,
        )

        assertEquals(listOf("c", "b", "a"), result.keys.toList())
    }

    @Test
    fun `grouping with descending sort reverses items within each group`() {
        val result = sortAndGroup(
            items = listOf("a1", "a3", "a2"),
            sortComparator = naturalOrder(),
            isSortAscending = false,
            groupKeySelector = { it.first().toString() },
            defaultGroupName = "default",
            isGroupAscending = true,
        )

        assertEquals(listOf("a3", "a2", "a1"), result["a"])
    }

    @Test
    fun `grouping ascending sort and ascending groups matrix`() {
        val result = sortAndGroup(
            items = listOf("b2", "a2", "b1", "a1"),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = { it.first().toString() },
            defaultGroupName = "default",
            isGroupAscending = true,
        )

        assertEquals(listOf("a", "b"), result.keys.toList())
        assertEquals(listOf("a1", "a2"), result["a"])
        assertEquals(listOf("b1", "b2"), result["b"])
    }

    @Test
    fun `grouping ascending sort and descending groups matrix`() {
        val result = sortAndGroup(
            items = listOf("b2", "a2", "b1", "a1"),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = { it.first().toString() },
            defaultGroupName = "default",
            isGroupAscending = false,
        )

        assertEquals(listOf("b", "a"), result.keys.toList())
        assertEquals(listOf("a1", "a2"), result["a"])
        assertEquals(listOf("b1", "b2"), result["b"])
    }

    @Test
    fun `grouping descending sort and ascending groups matrix`() {
        val result = sortAndGroup(
            items = listOf("b2", "a2", "b1", "a1"),
            sortComparator = naturalOrder(),
            isSortAscending = false,
            groupKeySelector = { it.first().toString() },
            defaultGroupName = "default",
            isGroupAscending = true,
        )

        assertEquals(listOf("a", "b"), result.keys.toList())
        assertEquals(listOf("a2", "a1"), result["a"])
        assertEquals(listOf("b2", "b1"), result["b"])
    }

    @Test
    fun `grouping descending sort and descending groups matrix`() {
        val result = sortAndGroup(
            items = listOf("b2", "a2", "b1", "a1"),
            sortComparator = naturalOrder(),
            isSortAscending = false,
            groupKeySelector = { it.first().toString() },
            defaultGroupName = "default",
            isGroupAscending = false,
        )

        assertEquals(listOf("b", "a"), result.keys.toList())
        assertEquals(listOf("a2", "a1"), result["a"])
        assertEquals(listOf("b2", "b1"), result["b"])
    }

    // --- Edge case tests ---

    @Test
    fun `empty list returns single empty group when no grouping`() {
        val result = sortAndGroup(
            items = emptyList<Int>(),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = null,
            defaultGroupName = "All",
            isGroupAscending = true,
        )

        assertEquals(mapOf("All" to emptyList()), result)
    }

    @Test
    fun `empty list returns empty map when grouping is set`() {
        val result = sortAndGroup(
            items = emptyList<String>(),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = { it },
            defaultGroupName = "default",
            isGroupAscending = true,
        )

        assertEquals(emptyMap(), result)
    }

    @Test
    fun `single item no grouping returns single group with one item`() {
        val result = sortAndGroup(
            items = listOf(42),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = null,
            defaultGroupName = "All",
            isGroupAscending = true,
        )

        assertEquals(mapOf("All" to listOf(42)), result)
    }

    @Test
    fun `single item with grouping returns one group with one item`() {
        val result = sortAndGroup(
            items = listOf("a1"),
            sortComparator = naturalOrder(),
            isSortAscending = true,
            groupKeySelector = { it.first().toString() },
            defaultGroupName = "default",
            isGroupAscending = true,
        )

        assertEquals(mapOf("a" to listOf("a1")), result)
    }

    @Test
    fun `all items in same group with custom comparator`() {
        data class Item(
            val name: String,
            val priority: Int,
        )

        val result = sortAndGroup(
            items = listOf(
                Item("c", 1),
                Item("a", 3),
                Item("b", 2),
            ),
            sortComparator = compareBy { it.priority },
            isSortAscending = true,
            groupKeySelector = { "same" },
            defaultGroupName = "default",
            isGroupAscending = true,
        )

        assertEquals(listOf("c", "b", "a"), result["same"]?.map { it.name })
    }

    @Test
    fun `custom comparator with thenBy preserves secondary sort`() {
        data class Item(
            val group: String,
            val name: String,
        )

        val result = sortAndGroup(
            items = listOf(
                Item("x", "b"),
                Item("x", "a"),
                Item("y", "c"),
            ),
            sortComparator = compareBy<Item> { it.group }.thenBy { it.name },
            isSortAscending = true,
            groupKeySelector = { it.group },
            defaultGroupName = "default",
            isGroupAscending = true,
        )

        assertEquals(listOf("a", "b"), result["x"]?.map { it.name })
        assertEquals(listOf("c"), result["y"]?.map { it.name })
    }

    @Test
    fun `group key with null fallback maps to fallback group`() {
        data class Item(
            val name: String,
            val location: String?,
        )

        val result = sortAndGroup(
            items = listOf(
                Item("a", "Kitchen"),
                Item("b", null),
                Item("c", "Kitchen"),
            ),
            sortComparator = compareBy { it.name },
            isSortAscending = true,
            groupKeySelector = { it.location ?: "Unknown" },
            defaultGroupName = "default",
            isGroupAscending = true,
        )

        assertEquals(listOf("a", "c"), result["Kitchen"]?.map { it.name })
        assertEquals(listOf("b"), result["Unknown"]?.map { it.name })
    }

    @Test
    fun `stable sort preserves original order for equal elements`() {
        data class Item(
            val key: Int,
            val label: String,
        )

        val result = sortAndGroup(
            items = listOf(
                Item(1, "first"),
                Item(1, "second"),
                Item(1, "third"),
            ),
            sortComparator = compareBy { it.key },
            isSortAscending = true,
            groupKeySelector = null,
            defaultGroupName = "All",
            isGroupAscending = true,
        )

        assertEquals(
            listOf("first", "second", "third"),
            result["All"]?.map { it.label },
        )
    }
}
