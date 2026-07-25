package com.chriscartland.batterybutler.datanetwork.rest

import com.chriscartland.batterybutler.domain.model.SyncAuthReason
import kotlinx.serialization.Serializable

/*
 * The Labs backend's error envelope:
 * `{"error":{"code":"unauthorized","message":"...","details":{"reason":"expired"}}}`.
 *
 * Parsed *leniently* — every field is defaulted, `details` may be absent, and `details.reason`
 * may be absent or carry values this client doesn't know (a server rollout can add them any
 * time). A body that isn't this shape at all (HTML from a proxy, plain text) must degrade to
 * "reason unknown", never to a parse failure: the HTTP status code is the truth, the envelope
 * is advisory detail.
 */

/** Top-level Labs API error envelope. */
@Serializable
internal data class ApiErrorEnvelopeWire(
    val error: ApiErrorBodyWire = ApiErrorBodyWire(),
)

/** The `error` object inside [ApiErrorEnvelopeWire]. */
@Serializable
internal data class ApiErrorBodyWire(
    val code: String = "",
    val message: String = "",
    val details: ApiErrorDetailsWire? = null,
)

/** The optional `details` object; only `reason` is modeled, other keys are ignored. */
@Serializable
internal data class ApiErrorDetailsWire(
    val reason: String = "",
)

/**
 * Extracts a [SyncAuthReason] from a 401 response body. Any unparseable or unrecognized body
 * yields [SyncAuthReason.UNKNOWN] — the 401 status already established *that* auth failed;
 * this only refines *why*.
 */
internal fun parseAuthReason(body: String): SyncAuthReason =
    try {
        when (
            syncJson
                .decodeFromString<ApiErrorEnvelopeWire>(body)
                .error.details
                ?.reason
        ) {
            "expired" -> SyncAuthReason.TOKEN_EXPIRED
            "invalid" -> SyncAuthReason.TOKEN_INVALID
            else -> SyncAuthReason.UNKNOWN
        }
    } catch (_: Exception) {
        SyncAuthReason.UNKNOWN
    }
