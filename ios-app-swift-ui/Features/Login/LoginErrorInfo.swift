import Foundation
import shared

// bb-ovm1: Relocated from LoginViewModelWrapper (deleted in the KMP-ObservableViewModel
// migration). Pure mapping of AuthError → (title, message, showRetry), kept static for
// direct unit testing (see LoginErrorInfoTests).
enum LoginErrorInfo {
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
