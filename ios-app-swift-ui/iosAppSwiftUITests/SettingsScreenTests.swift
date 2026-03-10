import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class SettingsScreenTests: XCTestCase {
    func testSettingsContentView_Default() {
        let view = SettingsContentView(
            exportData: nil,
            isShareSheetPresented: .constant(false),
            onExportData: {},
            onExportDataConsumed: {},
            appVersion: "1.0.0-42"
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
