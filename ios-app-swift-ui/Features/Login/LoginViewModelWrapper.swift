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
            return (
                String(localized: "login.error.coming_soon.title"),
                String(localized: "login.error.coming_soon.message"),
                false
            )
        case is AuthErrorConfigurationServerUnavailable:
            return (
                String(localized: "login.error.cant_connect.title"),
                String(localized: "login.error.cant_connect.message"),
                true
            )
        case is AuthErrorSignInCancelled:
            return (
                String(localized: "login.error.cancelled.title"),
                String(localized: "login.error.cancelled.message"),
                true
            )
        case is AuthErrorSignInNetworkError:
            return (
                String(localized: "login.error.network.title"),
                String(localized: "login.error.network.message"),
                true
            )
        case let failed as AuthErrorSignInFailed:
            let msg = failed.cause ?? String(localized: "login.error.sign_in_failed.default_message")
            return (
                String(localized: "login.error.sign_in_failed.title"),
                msg,
                true
            )
        case is AuthErrorTokenInvalid:
            return (
                String(localized: "login.error.session.title"),
                String(localized: "login.error.session.message"),
                true
            )
        case is AuthErrorTokenExpired:
            return (
                String(localized: "login.error.expired.title"),
                String(localized: "login.error.expired.message"),
                true
            )
        default:
            return (
                String(localized: "login.error.unknown.title"),
                String(localized: "login.error.unknown.message"),
                true
            )
        }
    }
}
