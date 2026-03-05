import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class DeviceTypeRowTests: XCTestCase {
    func testDeviceTypeRow_WithBatteryType() {
        let view = DeviceTypeRow(deviceType: TestData.deviceType)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }

    func testDeviceTypeRow_WithoutBatteryType() {
        let noBattery = DeviceType(
            id: "t3",
            name: "Generic Sensor",
            defaultIcon: "sensor",
            batteryType: "",
            batteryQuantity: 0
        )
        let view = DeviceTypeRow(deviceType: noBattery)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }
}
