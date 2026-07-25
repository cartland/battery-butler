package com.chriscartland.batterybutler.data.repository.auth

import com.chriscartland.batterybutler.datanetwork.LabsSignInIdentity
import com.chriscartland.batterybutler.datanetwork.auth.GoogleIdToken
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [labsUserFrom] keys the signed-in Labs [com.chriscartland.batterybutler.domain.model.User] on
 * the **Firebase uid** the token exchange minted (`signInWithIdp`'s `localId`) — the id the Labs
 * backend authorizes and attributes writes with (e.g. device-image `uploadedByUid`). The Google
 * profile email, the previous id, is only the last-resort fallback for a wire response that
 * omitted the uid.
 */
class LabsUserMappingTest {
    private val google = GoogleIdToken(
        idToken = "google-id-token-that-is-long-enough-to-truncate",
        email = "google-profile@example.com",
        displayName = "Display Name",
        photoUrl = "https://example.com/photo.png",
    )

    @Test
    fun `the Firebase uid from the signInWithIdp response becomes the User id`() {
        val user = labsUserFrom(
            identity = LabsSignInIdentity(firebaseUid = "firebase-uid-1", email = "account@example.com"),
            google = google,
        )

        assertEquals("firebase-uid-1", user.id, "User.id must be the Firebase uid, not the Google email")
        assertEquals("account@example.com", user.email, "the email from the exchange is preserved")
        assertEquals("Display Name", user.displayName)
        assertEquals("https://example.com/photo.png", user.photoUrl)
    }

    @Test
    fun `a response without an email keeps the Google profile email`() {
        val user = labsUserFrom(
            identity = LabsSignInIdentity(firebaseUid = "firebase-uid-1", email = null),
            google = google,
        )

        assertEquals("firebase-uid-1", user.id)
        assertEquals("google-profile@example.com", user.email)
    }

    @Test
    fun `a response without a uid falls back to the previous email-shaped id`() {
        val user = labsUserFrom(
            identity = LabsSignInIdentity(firebaseUid = null, email = null),
            google = google,
        )

        assertEquals("google-profile@example.com", user.id)
    }

    @Test
    fun `with neither uid nor any email the id falls back to a token prefix`() {
        val user = labsUserFrom(
            identity = LabsSignInIdentity(firebaseUid = null, email = null),
            google = google.copy(email = null),
        )

        assertEquals(google.idToken.take(32), user.id)
    }
}
