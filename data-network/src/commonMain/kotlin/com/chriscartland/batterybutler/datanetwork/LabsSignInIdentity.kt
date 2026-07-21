package com.chriscartland.batterybutler.datanetwork

/**
 * The identity the Labs backend minted at sign-in — parsed from the `accounts:signInWithIdp`
 * response and threaded through [LabsAuthGateway.signInToLabsWithGoogle] so the auth repository
 * can key the signed-in [com.chriscartland.batterybutler.domain.model.User] on the **Firebase
 * uid** rather than the Google profile's email. The backend authorizes and attributes writes by
 * this uid (e.g. device-image `uploadedByUid`), so it is the canonical user id; the email is a
 * display-time convenience.
 *
 * Both fields are lenient (`null` when the wire response omitted or blanked them — the response
 * DTO defaults every field, the lenient-consumer side of the defensive wire boundary): a missing
 * uid falls back to the caller's previous id derivation instead of failing the sign-in.
 */
data class LabsSignInIdentity(
    val firebaseUid: String?,
    val email: String?,
)
