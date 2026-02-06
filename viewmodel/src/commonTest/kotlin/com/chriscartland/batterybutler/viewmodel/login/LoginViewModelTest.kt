package com.chriscartland.batterybutler.viewmodel.login

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.testcommon.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val testUser = User(
        id = "test-id",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = null,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Unknown`() =
        runTest {
            val repo = FakeAuthRepository()
            val viewModel = LoginViewModel(repo)

            val state = viewModel.authState.value

            assertEquals(AuthState.Unknown, state)
        }

    @Test
    fun `authState reflects repository state changes`() =
        runTest {
            val repo = FakeAuthRepository()
            val viewModel = LoginViewModel(repo)
            advanceUntilIdle()

            repo.setAuthState(AuthState.Unauthenticated)
            advanceUntilIdle()

            val state = viewModel.authState.first { it == AuthState.Unauthenticated }
            assertEquals(AuthState.Unauthenticated, state)
        }

    @Test
    fun `isSignInAvailable returns true when configured`() =
        runTest {
            val repo = FakeAuthRepository()
            repo.isConfigured = true
            val viewModel = LoginViewModel(repo)

            assertTrue(viewModel.isSignInAvailable)
        }

    @Test
    fun `isSignInAvailable returns false when not configured`() =
        runTest {
            val repo = FakeAuthRepository()
            repo.isConfigured = false
            val viewModel = LoginViewModel(repo)

            assertFalse(viewModel.isSignInAvailable)
        }

    @Test
    fun `signInWithGoogle calls repository`() =
        runTest {
            val repo = FakeAuthRepository()
            repo.signInResult = Result.Success(testUser)
            val viewModel = LoginViewModel(repo)

            viewModel.signInWithGoogle()
            advanceUntilIdle()

            assertEquals(1, repo.signInCallCount)
        }

    @Test
    fun `signInWithGoogle success updates state to Authenticated`() =
        runTest {
            val repo = FakeAuthRepository()
            repo.signInResult = Result.Success(testUser)
            val viewModel = LoginViewModel(repo)

            viewModel.signInWithGoogle()
            advanceUntilIdle()

            val state = viewModel.authState.first { it is AuthState.Authenticated }
            assertIs<AuthState.Authenticated>(state)
            assertEquals(testUser, state.user)
        }

    @Test
    fun `signInWithGoogle cancelled updates state to Failed`() =
        runTest {
            val repo = FakeAuthRepository()
            val error = AuthError.SignIn.Cancelled()
            repo.signInResult = Result.Error(error)
            val viewModel = LoginViewModel(repo)

            viewModel.signInWithGoogle()
            advanceUntilIdle()

            val state = viewModel.authState.first { it is AuthState.Failed }
            assertIs<AuthState.Failed>(state)
            assertEquals(error, state.error)
        }

    @Test
    fun `signInWithGoogle network error updates state to Failed with NetworkError`() =
        runTest {
            val repo = FakeAuthRepository()
            val error = AuthError.SignIn.NetworkError(
                message = "Connection failed",
                cause = "No internet",
            )
            repo.signInResult = Result.Error(error)
            val viewModel = LoginViewModel(repo)

            viewModel.signInWithGoogle()
            advanceUntilIdle()

            val state = viewModel.authState.first { it is AuthState.Failed }
            assertIs<AuthState.Failed>(state)
            assertIs<AuthError.SignIn.NetworkError>(state.error)
        }

    @Test
    fun `signInWithGoogle not configured updates state to Failed`() =
        runTest {
            val repo = FakeAuthRepository()
            val error = AuthError.Configuration.NotConfigured()
            repo.signInResult = Result.Error(error)
            val viewModel = LoginViewModel(repo)

            viewModel.signInWithGoogle()
            advanceUntilIdle()

            val state = viewModel.authState.first { it is AuthState.Failed }
            assertIs<AuthState.Failed>(state)
            assertIs<AuthError.Configuration.NotConfigured>(state.error)
        }

    @Test
    fun `dismissError calls repository clearError`() =
        runTest {
            val repo = FakeAuthRepository()
            repo.setAuthState(AuthState.Failed(AuthError.SignIn.Cancelled()))
            val viewModel = LoginViewModel(repo)

            viewModel.dismissError()

            assertEquals(1, repo.clearErrorCallCount)
        }

    @Test
    fun `dismissError transitions from Failed to Unauthenticated`() =
        runTest {
            val repo = FakeAuthRepository()
            repo.setAuthState(AuthState.Failed(AuthError.SignIn.Cancelled()))
            val viewModel = LoginViewModel(repo)
            advanceUntilIdle()

            viewModel.dismissError()
            advanceUntilIdle()

            val state = viewModel.authState.first { it == AuthState.Unauthenticated }
            assertEquals(AuthState.Unauthenticated, state)
        }

    @Test
    fun `authenticated state contains correct user data`() =
        runTest {
            val repo = FakeAuthRepository()
            val userWithPhoto = User(
                id = "photo-user",
                email = "photo@example.com",
                displayName = "Photo User",
                photoUrl = "https://example.com/photo.jpg",
            )
            repo.signInResult = Result.Success(userWithPhoto)
            val viewModel = LoginViewModel(repo)

            viewModel.signInWithGoogle()
            advanceUntilIdle()

            val state = viewModel.authState.first { it is AuthState.Authenticated }
            assertIs<AuthState.Authenticated>(state)
            assertEquals("photo-user", state.user.id)
            assertEquals("photo@example.com", state.user.email)
            assertEquals("Photo User", state.user.displayName)
            assertEquals("https://example.com/photo.jpg", state.user.photoUrl)
        }

    @Test
    fun `failed state contains error message`() =
        runTest {
            val repo = FakeAuthRepository()
            val error = AuthError.SignIn.Failed(
                message = "Custom error message",
                cause = "Detailed cause",
            )
            repo.signInResult = Result.Error(error)
            val viewModel = LoginViewModel(repo)

            viewModel.signInWithGoogle()
            advanceUntilIdle()

            val state = viewModel.authState.first { it is AuthState.Failed }
            assertIs<AuthState.Failed>(state)
            assertEquals("Custom error message", state.error.message)
            assertEquals("Detailed cause", state.error.cause)
        }
}
