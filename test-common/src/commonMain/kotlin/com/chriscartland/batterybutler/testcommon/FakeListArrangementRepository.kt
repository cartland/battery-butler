package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.ListArrangement
import com.chriscartland.batterybutler.domain.model.ListScreen
import com.chriscartland.batterybutler.domain.repository.ListArrangementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of [ListArrangementRepository] for testing.
 *
 * Keeps one arrangement per [ListScreen], so a test can assert that the two list screens really do
 * store their choices independently. Defaults to an all-null arrangement — the fresh-install state,
 * where every ViewModel resolves its own default.
 *
 * Example usage:
 * ```kotlin
 * val repo = FakeListArrangementRepository()
 * repo.setArrangement(ListScreen.DEVICES, ListArrangement(sortKey = "name"))
 * assertEquals("name", repo.arrangement(ListScreen.DEVICES).first().sortKey)
 * ```
 */
class FakeListArrangementRepository(
    initial: Map<ListScreen, ListArrangement> = emptyMap(),
) : ListArrangementRepository {
    private val stored = MutableStateFlow(initial)

    override fun arrangement(screen: ListScreen): Flow<ListArrangement> = stored.map { it[screen] ?: ListArrangement() }

    override suspend fun setArrangement(
        screen: ListScreen,
        arrangement: ListArrangement,
    ) {
        stored.value = stored.value + (screen to arrangement)
    }
}
