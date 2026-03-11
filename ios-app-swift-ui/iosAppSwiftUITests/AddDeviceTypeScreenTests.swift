import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class AddDeviceTypeScreenTests: XCTestCase {
    func testAddDeviceTypeContentView_Empty() {
        let state = AddDeviceTypeState()

        let view = AddDeviceTypeContentView(
            state: state,
            onUpdateName: { _ in },
            onUpdateBatteryType: { _ in },
            onSelectIcon: { _ in },
            onIncrementQuantity: {},
            onDecrementQuantity: {},
            onSave: {},
            onCancel: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testAddDeviceTypeContentView_Filled() {
        var state = AddDeviceTypeState()
        state.name = "Smoke Alarm"
        state.batteryType = "9V"
        state.selectedIcon = "detector_smoke"
        state.batteryQuantity = 2

        let view = AddDeviceTypeContentView(
            state: state,
            onUpdateName: { _ in },
            onUpdateBatteryType: { _ in },
            onSelectIcon: { _ in },
            onIncrementQuantity: {},
            onDecrementQuantity: {},
            onSave: {},
            onCancel: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testAddDeviceTypeContentView_Error() {
        var state = AddDeviceTypeState()
        state.name = "Smoke Alarm"
        state.batteryType = "9V"
        state.saveError = "A device type with this name already exists."

        let view = AddDeviceTypeContentView(
            state: state,
            onUpdateName: { _ in },
            onUpdateBatteryType: { _ in },
            onSelectIcon: { _ in },
            onIncrementQuantity: {},
            onDecrementQuantity: {},
            onSave: {},
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
