import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class EventDetailScreenTests: XCTestCase {
    func testEventDetailContentView_Success() {
        let successState = EventDetailScreenStateSuccess(
            event: TestData.batteryEvent,
            device: TestData.device,
            deviceType: TestData.deviceType
        )

        let view = EventDetailContentView(state: successState)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testEventDetailContentView_NotFound() {
        let notFoundState = EventDetailScreenStateNotFound()

        let view = EventDetailContentView(state: notFoundState)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testEventDetailContentView_DeletedDevice() {
        let state = EventDetailScreenStateSuccess(
            event: TestData.batteryEvent,
            device: nil,
            deviceType: nil
        )

        let view = EventDetailContentView(state: state)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testEventDetailContentView_Loading() {
        let view = EventDetailContentView(state: nil)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
