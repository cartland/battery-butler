import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

// SKIPPED: AiChatScreen does not have a separate ContentView.
// The UI body is in AiChatScreen and depends on AiChatViewModelWrapper.
// To enable snapshot tests, extract a stateless AiChatContentView.
//
// However, MessageRow is a stateless subview and CAN be tested.
// See MessageRowTests.swift.

final class AiChatScreenTests: XCTestCase {
    // No testable AiChatContentView available yet.
    // MessageRow tested separately in MessageRowTests.swift.
}
