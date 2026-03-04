import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

// SKIPPED: LoginScreen does not have a separate ContentView.
// The UI body is directly in LoginScreen and is tightly coupled to
// LoginViewModelWrapper (reads wrapper.authState, wrapper.isSignInAvailable).
// To enable snapshot tests, LoginScreen needs refactoring to extract a
// stateless LoginContentView with @Binding parameters.
//
// Recommended ContentView signature:
//   struct LoginContentView: View {
//       let isAuthenticating: Bool
//       let isSignInAvailable: Bool
//       let onSignIn: () -> Void
//       let onSkipLogin: () -> Void
//   }

final class LoginScreenTests: XCTestCase {
    // No testable ContentView available yet.
    // See comment above for the recommended extraction.
}
