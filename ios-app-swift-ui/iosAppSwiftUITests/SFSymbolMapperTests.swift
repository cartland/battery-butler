import XCTest
@testable import BatteryButler

final class SFSymbolMapperTests: XCTestCase {

    func testNilInputReturnsDefault() {
        XCTAssertEqual(SFSymbolMapper.sfSymbolName(for: nil), "desktopcomputer")
    }

    func testUnknownStringReturnsDefault() {
        XCTAssertEqual(SFSymbolMapper.sfSymbolName(for: "unknown_device_xyz"), "desktopcomputer")
    }

    func testSmartphoneMapsToIphone() {
        XCTAssertEqual(SFSymbolMapper.sfSymbolName(for: "smartphone"), "iphone")
    }

    func testDetectorSmokeMapsToSensor() {
        XCTAssertEqual(SFSymbolMapper.sfSymbolName(for: "detector_smoke"), "sensor.fill")
    }

    func testStarMapsToStarFill() {
        XCTAssertEqual(SFSymbolMapper.sfSymbolName(for: "star"), "star.fill")
    }

    func testLaptopMapsToLaptopcomputer() {
        XCTAssertEqual(SFSymbolMapper.sfSymbolName(for: "laptop"), "laptopcomputer")
    }

    func testEmptyStringReturnsDefault() {
        XCTAssertEqual(SFSymbolMapper.sfSymbolName(for: ""), "desktopcomputer")
    }
}
