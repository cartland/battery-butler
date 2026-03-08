import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class AiChatScreenTests: XCTestCase {
    func testAiChatContentView_Empty() {
        let view = AiChatContentView(
            messages: [],
            isProcessing: false,
            inputText: .constant(""),
            onSend: {},
            onClear: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testAiChatContentView_WithMessages() {
        let messages = [
            AiMessage(
                id: "m1",
                role: .user,
                text: "What batteries does my remote use?",
                isPartial: false,
                hints: [:]
            ),
            AiMessage(
                id: "m2",
                role: .model,
                text: "Your Living Room Remote uses AAA batteries (quantity: 2).",
                isPartial: false,
                hints: [:]
            )
        ]

        let view = AiChatContentView(
            messages: messages,
            isProcessing: false,
            inputText: .constant(""),
            onSend: {},
            onClear: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
