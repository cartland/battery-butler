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

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }

    func testHistoryListContentView_Loading() {
        let view = HistoryListContentView(state: HistoryListUiStateLoading())

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }
}
