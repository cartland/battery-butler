import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

// SKIPPED: HomeScreen does not have a separate HomeContentView.
// The UI body is in HomeScreen and depends on HomeViewModelWrapper and NativeComponent.
// To enable snapshot tests, HomeScreen needs refactoring to extract a stateless
// HomeContentView that accepts grouped devices and action callbacks.
//
// However, DeviceRow is a stateless subview used by HomeScreen and CAN be tested.
// See DeviceRowTests.swift.

final class HomeScreenTests: XCTestCase {
    // No testable HomeContentView available yet.
    // See comment above for the recommended extraction.
}
