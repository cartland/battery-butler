import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class HistoryListScreenTests: XCTestCase {
    private func makeView(state: HistoryListScreenState) -> some View {
        HistoryListContentView(
            state: state,
            onAddEventTapped: {},
            onEventTapped: { _, _ in },
            eventDestination: { _ in Text("Event Detail") }
        )
    }

    func testHistoryListContentView_Success() {
        let successState = HistoryListScreenStateSuccess(
            items: [TestData.historyItem, TestData.historyItem2]
        )

        let view = makeView(state: successState)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testHistoryListContentView_Loading() {
        let view = makeView(state: HistoryListScreenStateLoading())

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testHistoryListContentView_Empty() {
        let emptyState = HistoryListScreenStateSuccess(items: [])

        let view = makeView(state: emptyState)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
