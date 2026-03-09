import SwiftUI
import shared
import Combine

class LoginViewModelWrapper: ObservableObject {
    @Published var authState: AuthState? = nil
    @Published var errorTitle: String = ""
    @Published var errorMessage: String = ""
    @Published var showRetryButton: Bool = true

    let isSignInAvailable: Bool

    private let viewModel: LoginViewModel
    private let viewModelStore = KmpViewModelStore()
    private var authTask: Task<Void, Never>?

    init(_ viewModel: LoginViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)

        self.isSignInAvailable = viewModel.isSignInAvailable
        self.authState = viewModel.authState.value as? AuthState

        self.authTask = Task { @MainActor [weak self] in
            for await state in viewModel.authState {
                self?.authState = state as? AuthState
                if let failed = state as? AuthStateFailed {
                    let (title, message, retry) = Self.errorInfo(for: failed.error)
                    self?.errorTitle = title
                    self?.errorMessage = message
                    self?.showRetryButton = retry
                }
            }
        }
    }

    deinit {
        authTask?.cancel()
        viewModelStore.clear()
    }

    func signInWithGoogle() {
        viewModel.signInWithGoogle()
    }

    func dismissError() {
        viewModel.dismissError()
    }

    static func errorInfo(for error: AuthError) -> (String, String, Bool) {
        switch error {
        case is AuthErrorConfigurationNotConfigured:
            return ("Coming Soon", "Sign-in is not yet available for this build.", false)
        case is AuthErrorConfigurationServerUnavailable:
            return ("Can't Connect", "The authentication server is not reachable. Please try again later.", true)
        case is AuthErrorSignInCancelled:
            return ("Cancelled", "Sign-in was cancelled. You can try again when you're ready.", true)
        case is AuthErrorSignInNetworkError:
            return ("Connection Problem", "Please check your internet connection and try again.", true)
        case let failed as AuthErrorSignInFailed:
            let msg = failed.cause ?? "Your data is safe. Please try signing in again."
            return ("Sign In Failed", msg, true)
        case is AuthErrorTokenInvalid:
            return ("Session Error", "Please sign in again to continue.", true)
        case is AuthErrorTokenExpired:
            return ("Session Expired", "Your session has expired. Please sign in again.", true)
        default:
            return ("Something Went Wrong", "An unexpected error occurred. Your data is safe.", true)
        }
    }
}
