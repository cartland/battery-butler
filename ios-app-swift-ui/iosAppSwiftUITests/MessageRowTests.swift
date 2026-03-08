import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class MessageRowTests: XCTestCase {
    func testMessageRow_UserMessage() {
        let message = AiMessage(
            id: "m1",
            role: .user,
            text: "When was the last time I replaced batteries in the living room remote?",
            isPartial: false,
            hints: [:]
        )
        let view = MessageRow(message: message)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testMessageRow_ModelMessage() {
        let message = AiMessage(
            id: "m2",
            role: .model,
            text: "The last battery replacement for your Living Room Remote was on January 1, 2024. That was about 14 months ago.",
            isPartial: false,
            hints: [:]
        )
        let view = MessageRow(message: message)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
