package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.model.SyncAuthReason

/**
 * Typed failures a [RemoteDataSource] surfaces from its wire layer.
 *
 * Thrown through [RemoteDataSource.subscribe]'s flow or from [RemoteDataSource.push] so the
 * sync manager (the single catch point for the sync loop) can distinguish "the user must sign
 * in" from "the server is unhealthy" from a generic transport error — instead of an HTTP error
 * body silently parsing as an empty snapshot and masquerading as success.
 */
sealed class RemoteSyncException(
    message: String,
) : Exception(message) {
    /**
     * The backend rejected the call with 401 — or would have: a call attempted with no session
     * at all is refused client-side without firing a request ([SyncAuthReason.NO_SESSION]).
     */
    class AuthRequired(
        val reason: SyncAuthReason,
    ) : RemoteSyncException("Sync authentication required: $reason")

    /** The backend answered with a non-2xx, non-401 status. [statusCode] is kept for logging. */
    class ServerError(
        val statusCode: Int,
    ) : RemoteSyncException("Sync server error: HTTP $statusCode")

    /**
     * A session exists but a Bearer token could not be obtained for *transient* reasons (the
     * refresh/restore call hit a network failure or 5xx); no sync request was sent. Distinct
     * from [AuthRequired] on purpose: the user is still signed in, so this must surface on the
     * sync layer's network-failure path — a flaky network must never show "sign in required".
     */
    class TokenUnavailable : RemoteSyncException("Sync token unavailable (transient refresh failure)")
}
