package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.preferences.PreferencesDataSource
import com.chriscartland.batterybutler.domain.model.ListArrangement
import com.chriscartland.batterybutler.domain.model.ListScreen
import com.chriscartland.batterybutler.domain.repository.ListArrangementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import me.tatarka.inject.annotations.Inject

/**
 * DataStore-backed implementation of [ListArrangementRepository].
 *
 * Each field is its own entry rather than one encoded blob, so a value written by a newer build —
 * or an unreadable one — degrades on its own instead of taking the whole arrangement with it.
 */
@Inject
class DataStoreListArrangementRepository(
    private val preferencesDataSource: PreferencesDataSource,
) : ListArrangementRepository {
    override fun arrangement(screen: ListScreen): Flow<ListArrangement> =
        combine(
            preferencesDataSource.stringValue(screen.key(SORT)),
            preferencesDataSource.stringValue(screen.key(GROUP)),
            preferencesDataSource.stringValue(screen.key(SORT_ASCENDING)),
            preferencesDataSource.stringValue(screen.key(GROUP_ASCENDING)),
        ) { sort, group, sortAscending, groupAscending ->
            ListArrangement(
                sortKey = sort,
                groupKey = group,
                isSortAscending = sortAscending.toBooleanOrNull(),
                isGroupAscending = groupAscending.toBooleanOrNull(),
            )
            // `distinctUntilChanged` for the same reason as DataStoreDisplayDensityRepository:
            // DataStore.data re-emits the WHOLE preferences object on every edit to ANY key, and
            // this store is shared with the data mode, density, and Labs session entries. Without
            // it, signing in would re-emit a structurally-identical arrangement and re-sort and
            // recompose every list for no reason.
        }.distinctUntilChanged()

    override suspend fun setArrangement(
        screen: ListScreen,
        arrangement: ListArrangement,
    ) {
        arrangement.sortKey?.let { preferencesDataSource.setStringValue(screen.key(SORT), it) }
        arrangement.groupKey?.let { preferencesDataSource.setStringValue(screen.key(GROUP), it) }
        arrangement.isSortAscending?.let {
            preferencesDataSource.setStringValue(screen.key(SORT_ASCENDING), it.toString())
        }
        arrangement.isGroupAscending?.let {
            preferencesDataSource.setStringValue(screen.key(GROUP_ASCENDING), it.toString())
        }
    }

    private companion object {
        private const val SORT = "sort"
        private const val GROUP = "group"
        private const val SORT_ASCENDING = "sort_ascending"
        private const val GROUP_ASCENDING = "group_ascending"

        fun ListScreen.key(field: String): String = "list_arrangement.$storageId.$field"

        /** Anything that isn't exactly "true" or "false" reads as never-chosen, never as a crash. */
        fun String?.toBooleanOrNull(): Boolean? =
            when (this) {
                "true" -> true
                "false" -> false
                else -> null
            }
    }
}
