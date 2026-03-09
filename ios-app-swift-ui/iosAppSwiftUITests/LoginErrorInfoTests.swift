import XCTest
import shared
@testable import BatteryButler

final class LoginErrorInfoTests: XCTestCase {

    func testConfigurationNotConfigured() {
        let error: AuthError = AuthErrorConfigurationNotConfigured(
            message: "Sign-in not available", cause: nil
        )
        let (title, message, showRetry) = LoginViewModelWrapper.errorInfo(for: error)
        XCTAssertEqual(title, "Coming Soon")
        XCTAssertTrue(message.contains("not yet available"))
        XCTAssertFalse(showRetry)
    }

    func testConfigurationServerUnavailable() {
        let error: AuthError = AuthErrorConfigurationServerUnavailable(
            message: "Server unavailable", cause: nil
        )
        let (title, _, showRetry) = LoginViewModelWrapper.errorInfo(for: error)
        XCTAssertEqual(title, "Can't Connect")
        XCTAssertTrue(showRetry)
    }

    func testSignInCancelled() {
        let error: AuthError = AuthErrorSignInCancelled(
            message: "Sign-in cancelled", cause: nil
        )
        let (title, message, showRetry) = LoginViewModelWrapper.errorInfo(for: error)
        XCTAssertEqual(title, "Cancelled")
        XCTAssertTrue(message.contains("cancelled"))
        XCTAssertTrue(showRetry)
    }

    func testSignInNetworkError() {
        let error: AuthError = AuthErrorSignInNetworkError(
            message: "Network error", cause: nil
        )
        let (title, _, showRetry) = LoginViewModelWrapper.errorInfo(for: error)
        XCTAssertEqual(title, "Connection Problem")
        XCTAssertTrue(showRetry)
    }

    func testSignInFailedWithCause() {
        let error: AuthError = AuthErrorSignInFailed(
            message: "Sign-in failed", cause: "OAuth token expired"
        )
        let (title, message, showRetry) = LoginViewModelWrapper.errorInfo(for: error)
        XCTAssertEqual(title, "Sign In Failed")
        XCTAssertEqual(message, "OAuth token expired")
        XCTAssertTrue(showRetry)
    }

    func testSignInFailedWithoutCause() {
        let error: AuthError = AuthErrorSignInFailed(
            message: "Sign-in failed", cause: nil
        )
        let (title, message, showRetry) = LoginViewModelWrapper.errorInfo(for: error)
        XCTAssertEqual(title, "Sign In Failed")
        XCTAssertTrue(message.contains("try signing in again"))
        XCTAssertTrue(showRetry)
    }

    func testTokenInvalid() {
        let error: AuthError = AuthErrorTokenInvalid(
            message: "Invalid token", cause: nil
        )
        let (title, _, showRetry) = LoginViewModelWrapper.errorInfo(for: error)
        XCTAssertEqual(title, "Session Error")
        XCTAssertTrue(showRetry)
    }

    func testTokenExpired() {
        let error: AuthError = AuthErrorTokenExpired(
            message: "Session expired", cause: nil
        )
        let (title, message, showRetry) = LoginViewModelWrapper.errorInfo(for: error)
        XCTAssertEqual(title, "Session Expired")
        XCTAssertTrue(message.contains("expired"))
        XCTAssertTrue(showRetry)
    }
}
