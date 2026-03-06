import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class EventDetailScreenTests: XCTestCase {
    func testEventDetailContentView_Success() {
        let successState = EventDetailUiStateSuccess(
            event: TestData.batteryEvent,
            device: TestData.device,
            deviceType: TestData.deviceType
        )

        let view = EventDetailContentView(state: successState)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }

    func testEventDetailContentView_NotFound() {
        let notFoundState = EventDetailUiStateNotFound()

        let view = EventDetailContentView(state: notFoundState)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }

    func testEventDetailContentView_Loading() {
        let view = EventDetailContentView(state: nil)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }
}
