import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class LoginScreenTests: XCTestCase {
    func testLoginContentView_Default() {
        let view = LoginContentView(
            authState: nil,
            isSignInAvailable: true,
            errorTitle: "",
            errorMessage: "",
            showRetryButton: false,
            showError: .constant(false),
            onSignIn: {},
            onSkipLogin: {},
            onRetry: {},
            onDismissError: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testLoginContentView_Authenticating() {
        let view = LoginContentView(
            authState: AuthStateAuthenticating(),
            isSignInAvailable: true,
            errorTitle: "",
            errorMessage: "",
            showRetryButton: false,
            showError: .constant(false),
            onSignIn: {},
            onSkipLogin: {},
            onRetry: {},
            onDismissError: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
