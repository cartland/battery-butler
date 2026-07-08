import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class SettingsScreenTests: XCTestCase {
    func testSettingsContentView_Default() {
        let view = SettingsContentView(
            currentUser: nil,
            onSignOut: {},
            dataMode: DataModeNone(),
            availableDataModes: [DataModeNone(), DataModeMock()],
            onDataModeSelected: { _ in },
            aiEngineType: .cloud,
            availableAiEngines: [.cloud, .onDevice, .noOp],
            onAiEngineSelected: { _ in },
            exportData: nil,
            isShareSheetPresented: .constant(false),
            onExportData: {},
            onExportDataConsumed: {},
            onImportData: {},
            importInProgress: false,
            appVersion: "1.0.0-42"
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testSettingsContentView_SignedIn() {
        let user = User(id: "u1", email: "user@example.com", displayName: "Jane Doe", photoUrl: nil)
        let view = SettingsContentView(
            currentUser: user,
            onSignOut: {},
            dataMode: DataModeMock(),
            availableDataModes: [DataModeNone(), DataModeMock()],
            onDataModeSelected: { _ in },
            aiEngineType: .cloud,
            availableAiEngines: [.cloud, .onDevice, .noOp],
            onAiEngineSelected: { _ in },
            exportData: nil,
            isShareSheetPresented: .constant(false),
            onExportData: {},
            onExportDataConsumed: {},
            onImportData: {},
            importInProgress: false,
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
