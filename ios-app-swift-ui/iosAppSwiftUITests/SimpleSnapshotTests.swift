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
        
        // Assert snapshot
        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }
}
