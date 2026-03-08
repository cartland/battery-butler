import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class EditDeviceTypeScreenTests: XCTestCase {
    func testEditDeviceTypeContentView_Loaded() {
        var state = EditDeviceTypeState()
        state.isLoading = false
        state.name = "TV Remote"
        state.batteryType = "AAA"
        state.originalId = "t1"

        let view = EditDeviceTypeContentView(
            state: state,
            onUpdateName: { _ in },
            onUpdateBatteryType: { _ in },
            onSave: {},
            onDelete: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testEditDeviceTypeContentView_Loading() {
        let state = EditDeviceTypeState()

        let view = EditDeviceTypeContentView(
            state: state,
            onUpdateName: { _ in },
            onUpdateBatteryType: { _ in },
            onSave: {},
            onDelete: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testEditDeviceTypeContentView_NotFound() {
        var state = EditDeviceTypeState()
        state.isLoading = false
        state.isNotFound = true

        let view = EditDeviceTypeContentView(
            state: state,
            onUpdateName: { _ in },
            onUpdateBatteryType: { _ in },
            onSave: {},
            onDelete: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
