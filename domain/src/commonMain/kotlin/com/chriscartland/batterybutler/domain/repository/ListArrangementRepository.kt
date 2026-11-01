package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.ListArrangement
import com.chriscartland.batterybutler.domain.model.ListScreen
import kotlinx.coroutines.flow.Flow

/**
 * Persists each list screen's sort/group choices across app restarts.
 *
 * Emits the **stored** arrangement, with nulls for anything the user has never chosen — see
 * [ListArrangement].
 */
interface ListArrangementRepository {
    fun arrangement(screen: ListScreen): Flow<ListArrangement>

    suspend fun setArrangement(
        screen: ListScreen,
        arrangement: ListArrangement,
    )
}
