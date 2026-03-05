import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class DeviceRowTests: XCTestCase {
    func testDeviceRow_WithLocation() {
        let view = DeviceRow(device: TestData.device)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }

    func testDeviceRow_WithoutLocation() {
        let view = DeviceRow(device: TestData.device3)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }
}
