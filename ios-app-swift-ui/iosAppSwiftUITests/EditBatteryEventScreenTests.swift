import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class EditBatteryEventScreenTests: XCTestCase {
    func testEditBatteryEventContentView_Success() {
        let dummyEvent = BatteryEvent(
            id: "e1",
            deviceId: "d1",
            date: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000), // Jan 1, 2024
            batteryType: "AAA",
            notes: "Replaced during cleaning"
        )

        let dummyDevice = Device(
            id: "d1",
            name: "Living Room Remote",
            typeId: "t1",
            batteryLastReplaced: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000),
            lastUpdated: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000),
            location: "Living Room",
            imagePath: nil
        )

        let dummyDeviceType = DeviceType(
            id: "t1",
            name: "TV Remote",
            defaultIcon: "tv",
            batteryType: "AAA",
            batteryQuantity: 2
        )

        let state = EditBatteryEventUiStateSuccess(
            event: dummyEvent,
            device: dummyDevice,
            deviceType: dummyDeviceType
        )

        let view = EditBatteryEventContentView(
            state: state,
            eventDate: .constant(Date(timeIntervalSince1970: 1704067200)), // Jan 1, 2024
            batteryType: .constant("AAA"),
            notes: .constant("Replaced during cleaning"),
            hasInitializedFields: .constant(true),
            onSave: {},
            onDelete: {},
            onCancel: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }

    func testEditBatteryEventContentView_Loading() {
        let view = EditBatteryEventContentView(
            state: EditBatteryEventUiStateLoading(),
            eventDate: .constant(Date()),
            batteryType: .constant(""),
            notes: .constant(""),
            hasInitializedFields: .constant(false),
            onSave: {},
            onDelete: {},
            onCancel: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }

    func testEditBatteryEventContentView_NotFound() {
        let view = EditBatteryEventContentView(
            state: EditBatteryEventUiStateNotFound(),
            eventDate: .constant(Date()),
            batteryType: .constant(""),
            notes: .constant(""),
            hasInitializedFields: .constant(false),
            onSave: {},
            onDelete: {},
            onCancel: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }
}
