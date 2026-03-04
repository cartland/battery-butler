import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

// SKIPPED: DeviceTypeListScreen does not have a separate ContentView.
// The UI body is in DeviceTypeListScreen and depends on
// DeviceTypeListViewModelWrapper and NativeComponent.
// To enable snapshot tests, extract a stateless DeviceTypeListContentView.
//
// However, DeviceTypeRow is a stateless subview and is tested in
// DeviceTypeRowTests.swift.

final class DeviceTypeListScreenTests: XCTestCase {
    // No testable ContentView available yet.
    // DeviceTypeRow tested separately in DeviceTypeRowTests.swift.
}
