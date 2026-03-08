import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class HistoryListScreenTests: XCTestCase {
    func testHistoryListContentView_Success() {
        let successState = HistoryListUiStateSuccess(
            items: [TestData.historyItem, TestData.historyItem2]
        )

        let view = HistoryListContentView(state: successState)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testHistoryListContentView_Loading() {
        let view = HistoryListContentView(state: HistoryListUiStateLoading())

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testHistoryListContentView_Empty() {
        let emptyState = HistoryListUiStateSuccess(items: [])

        let view = HistoryListContentView(state: emptyState)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
