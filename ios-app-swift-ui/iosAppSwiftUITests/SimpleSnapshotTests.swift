import XCTest
import SwiftUI
import SnapshotTesting
@testable import BatteryButler

final class SimpleSnapshotTests: XCTestCase {
    func testSimpleView() {
        let view = Text("Hello, Snapshot Testing!")
            .padding()
            .background(Color.blue)
            .foregroundColor(.white)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
