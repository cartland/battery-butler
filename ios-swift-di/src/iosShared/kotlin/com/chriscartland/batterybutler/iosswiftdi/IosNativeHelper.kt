package com.chriscartland.batterybutler.iosswiftdi

import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSourceState
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.domain.repository.AuthRepository
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
        val noOpAuthRepository = object : AuthRepository {
            override val authState: StateFlow<AuthState> =
                MutableStateFlow(AuthState.Unauthenticated)
            override val currentUser: Flow<User?> = flowOf(null)

            override fun isSignInAvailable(): Boolean = false

            override suspend fun signInWithGoogle(): Result<User, AuthError> =
                Result.Error(
                    AuthError.Configuration.NotConfigured(
                        message = "Not available",
                        cause = "Auth not configured for iOS native",
                    ),
                )

            override suspend fun signOut() {}

            override suspend fun refreshToken(): Result<Unit, AuthError> =
                Result.Error(
                    AuthError.Token.Expired(
                        message = "Not available",
                        cause = "Auth not configured for iOS native",
                    ),
                )

            override fun clearError() {}
        }
        val component = InjectNativeComponent(
            databaseFactory,
            dataStoreFactory,
            noOpAiEngine,
            noOpRemoteDataSource,
            noOpAuthRepository,
        )
        return component
    }
}
