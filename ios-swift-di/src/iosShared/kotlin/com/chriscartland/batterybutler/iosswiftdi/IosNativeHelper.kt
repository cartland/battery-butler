package com.chriscartland.batterybutler.iosswiftdi

import com.chriscartland.batterybutler.ai.AiEngine
import com.chriscartland.batterybutler.ai.AiMessage
import com.chriscartland.batterybutler.ai.ToolHandler
import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSourceState
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

class IosNativeHelper {
    fun createComponent(): NativeComponent {
        val databaseFactory = DatabaseFactory()
        val dataStoreFactory = DataStoreFactory()
        val noOpAiEngine = object : AiEngine {
            override val isAvailable: Flow<Boolean> = flowOf(false)

            override suspend fun generateResponse(
                prompt: String,
                toolHandler: ToolHandler?,
            ): Flow<AiMessage> = flowOf()

            override val compatibility: Flow<Boolean> = flowOf(false)
        }
        val noOpRemoteDataSource = object : RemoteDataSource {
            override val state: StateFlow<RemoteDataSourceState> =
                MutableStateFlow(RemoteDataSourceState.NotStarted)

            override fun subscribe(): Flow<RemoteUpdate> = flowOf()

            override suspend fun push(update: RemoteUpdate): Boolean = true
        }
        val component = InjectNativeComponent(
            databaseFactory,
            dataStoreFactory,
            noOpAiEngine,
            noOpRemoteDataSource,
        )
        return component
    }
}
