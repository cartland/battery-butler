package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.datanetwork.LabsAuthGateway
import com.chriscartland.batterybutler.datanetwork.LabsSessionInvalidation
import com.chriscartland.batterybutler.datanetwork.LabsSignInIdentity
import com.chriscartland.batterybutler.datanetwork.LabsTokenResult
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.SyncAuthReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Controllable [LabsAuthGateway] fake. Tests script the token/restore results and can push
 * [LabsSessionInvalidation] events through [emitInvalidation] to exercise the reactive
 * session-loss path.
 */
class FakeLabsAuthGateway : LabsAuthGateway {
    // Buffered so a test can emit before pumping the collector's dispatcher.
    private val _sessionInvalidations = MutableSharedFlow<LabsSessionInvalidation>(extraBufferCapacity = 8)
    override val sessionInvalidations: Flow<LabsSessionInvalidation> = _sessionInvalidations

    var signInResult: Result<LabsSignInIdentity, AuthError> =
        Result.Success(LabsSignInIdentity(firebaseUid = "fake-firebase-uid", email = "fake@example.com"))
    var restoreResult: Result<Unit, AuthError> = Result.Success(Unit)
    var tokenResult: LabsTokenResult = LabsTokenResult.NoSession

    var signInCount = 0
    var signOutCount = 0
    var restoreCount = 0
    val rejectedReasons = mutableListOf<SyncAuthReason>()

    /** Emits an invalidation event (suspends until the collector receives it). */
    suspend fun emitInvalidation(invalidation: LabsSessionInvalidation) {
        _sessionInvalidations.emit(invalidation)
    }

    override suspend fun signInToLabsWithGoogle(googleIdToken: String): Result<LabsSignInIdentity, AuthError> {
        signInCount++
        return signInResult
    }

    override suspend fun getLabsToken(forceRefresh: Boolean): LabsTokenResult = tokenResult

    override suspend fun getLabsIdToken(): String? = (tokenResult as? LabsTokenResult.Token)?.idToken

    override suspend fun reportSessionRejected(reason: SyncAuthReason) {
        rejectedReasons += reason
    }

    override suspend fun signOutLabs() {
        signOutCount++
    }

    override suspend fun restoreSession(): Result<Unit, AuthError> {
        restoreCount++
        return restoreResult
    }
}
