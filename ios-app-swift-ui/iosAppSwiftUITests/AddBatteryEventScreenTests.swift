import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class AddBatteryEventScreenTests: XCTestCase {
    func testAddBatteryEventContentView_WithDevices() {
        let view = AddBatteryEventContentView(
            devices: [TestData.device, TestData.device2],
            selectedDeviceId: .constant(nil),
            eventDate: .constant(Date(timeIntervalSince1970: 1704067200)),
            batteryType: .constant(""),
            notes: .constant(""),
            showMoreDetails: .constant(false),
            isAiBatchImportEnabled: false,
            aiMessages: [],
            batchInput: .constant(""),
            onSaveEvent: {},
            onProcessBatch: {},
            onClearAiMessages: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }

    func testAddBatteryEventContentView_Empty() {
        let view = AddBatteryEventContentView(
            devices: [],
            selectedDeviceId: .constant(nil),
            eventDate: .constant(Date(timeIntervalSince1970: 1704067200)),
            batteryType: .constant(""),
            notes: .constant(""),
            showMoreDetails: .constant(false),
            isAiBatchImportEnabled: false,
            aiMessages: [],
            batchInput: .constant(""),
            onSaveEvent: {},
            onProcessBatch: {},
            onClearAiMessages: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }
}
