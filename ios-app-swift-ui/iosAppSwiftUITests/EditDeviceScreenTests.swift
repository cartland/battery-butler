import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class EditDeviceScreenTests: XCTestCase {
    func testEditDeviceContentView_Success() {
        let dummyDevice = Device(
            id: "d1",
            name: "Living Room Remote",
            typeId: "t1",
            batteryLastReplaced: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000), // Jan 1, 2024
            lastUpdated: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000),
            location: "Living Room",
            imagePath: nil
        )

        let dummyTypes = [
            DeviceType(id: "t1", name: "TV Remote", defaultIcon: "tv", batteryType: "AAA", batteryQuantity: 2)
        ]

        let state = EditDeviceUiStateSuccess(device: dummyDevice, deviceTypes: dummyTypes)

        // Use bindings populated with initial fake data to simulate `.onAppear` having fired
        let view = EditDeviceContentView(
            state: state,
            name: .constant("Living Room Remote"),
            location: .constant("Living Room"),
            selectedTypeId: .constant("t1"),
            hasInitializedFields: .constant(true),
            onSave: {},
            onDelete: {},
            onCancel: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testEditDeviceContentView_Loading() {
        let view = EditDeviceContentView(
            state: EditDeviceUiStateLoading(),
            name: .constant(""),
            location: .constant(""),
            selectedTypeId: .constant(""),
            hasInitializedFields: .constant(false),
            onSave: {},
            onDelete: {},
            onCancel: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testEditDeviceContentView_NotFound() {
        let view = EditDeviceContentView(
            state: EditDeviceUiStateNotFound(),
            name: .constant(""),
            location: .constant(""),
            selectedTypeId: .constant(""),
            hasInitializedFields: .constant(false),
            onSave: {},
            onDelete: {},
            onCancel: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
