import XCTest
import SwiftUI
@testable import BatteryButler

final class BatteryAgeHelperTests: XCTestCase {

    // MARK: - batteryAgeColor

    func testBatteryAgeColorNilReturnsDefault() {
        XCTAssertEqual(BatteryAgeHelper.batteryAgeColor(days: nil), .butlerOnSurfaceVariant)
    }

    func testBatteryAgeColorZeroDaysReturnsDefault() {
        XCTAssertEqual(BatteryAgeHelper.batteryAgeColor(days: 0), .butlerOnSurfaceVariant)
    }

    func testBatteryAgeColorUnder180ReturnsDefault() {
        XCTAssertEqual(BatteryAgeHelper.batteryAgeColor(days: 179), .butlerOnSurfaceVariant)
    }

    func testBatteryAgeColor180ReturnsWarning() {
        XCTAssertEqual(BatteryAgeHelper.batteryAgeColor(days: 180), .butlerBatteryWarning)
    }

    func testBatteryAgeColor364ReturnsWarning() {
        XCTAssertEqual(BatteryAgeHelper.batteryAgeColor(days: 364), .butlerBatteryWarning)
    }

    func testBatteryAgeColor365ReturnsError() {
        XCTAssertEqual(BatteryAgeHelper.batteryAgeColor(days: 365), .butlerError)
    }

    func testBatteryAgeColor1000ReturnsError() {
        XCTAssertEqual(BatteryAgeHelper.batteryAgeColor(days: 1000), .butlerError)
    }

    // MARK: - batteryAgeFontWeight

    func testFontWeightNilReturnsRegular() {
        XCTAssertEqual(BatteryAgeHelper.batteryAgeFontWeight(days: nil), .regular)
    }

    func testFontWeight364ReturnsRegular() {
        XCTAssertEqual(BatteryAgeHelper.batteryAgeFontWeight(days: 364), .regular)
    }

    func testFontWeight365ReturnsBold() {
        XCTAssertEqual(BatteryAgeHelper.batteryAgeFontWeight(days: 365), .bold)
    }

    func testFontWeight1000ReturnsBold() {
        XCTAssertEqual(BatteryAgeHelper.batteryAgeFontWeight(days: 1000), .bold)
    }

    // MARK: - daysSince

    func testDaysSinceKnownPastDateReturnsPositive() {
        // 2020-01-01 00:00:00 UTC in milliseconds
        let epoch: Int64 = 1_577_836_800_000
        let days = BatteryAgeHelper.daysSince(epochMilliseconds: epoch)
        XCTAssertNotNil(days)
        XCTAssertGreaterThan(days!, 0)
    }

    func testDaysSinceNowReturnsZero() {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let days = BatteryAgeHelper.daysSince(epochMilliseconds: nowMs)
        XCTAssertNotNil(days)
        XCTAssertEqual(days!, 0)
    }
}
