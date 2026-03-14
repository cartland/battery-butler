import Foundation
import shared

/// Shared test data factory for iOS snapshot tests.
/// Provides reusable KMP data objects to reduce duplication across test files.
struct TestData {
    static let device = Device(
        id: "d1",
        name: "Living Room Remote",
        typeId: "t1",
        batteryLastReplaced: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000),
        lastUpdated: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000),
        location: "Living Room",
        imagePath: nil
    )

    static let device2 = Device(
        id: "d2",
        name: "Kitchen Smoke Alarm",
        typeId: "t2",
        batteryLastReplaced: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1701388800000),
        lastUpdated: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1701388800000),
        location: "Kitchen",
        imagePath: nil
    )

    static let device3 = Device(
        id: "d3",
        name: "Garage Door Sensor",
        typeId: "t1",
        batteryLastReplaced: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1706745600000),
        lastUpdated: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1706745600000),
        location: nil,
        imagePath: nil
    )

    static let deviceType = DeviceType(
        id: "t1",
        name: "TV Remote",
        defaultIcon: "tv",
        batteryType: "AAA",
        batteryQuantity: 2
    )

    static let deviceType2 = DeviceType(
        id: "t2",
        name: "Smoke Alarm",
        defaultIcon: "detector_smoke",
        batteryType: "9V",
        batteryQuantity: 1
    )

    static let deviceTypes = [deviceType, deviceType2]

    static let batteryEvent = BatteryEvent(
        id: "e1",
        deviceId: "d1",
        date: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000),
        batteryType: "AAA",
        notes: "Replaced both batteries"
    )

    static let batteryEvent2 = BatteryEvent(
        id: "e2",
        deviceId: "d1",
        date: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1696118400000),
        batteryType: nil,
        notes: nil
    )

    static let historyItem = HistoryItemModel(
        event: batteryEvent,
        deviceName: "Living Room Remote",
        deviceTypeName: "TV Remote",
        deviceLocation: "Living Room"
    )

    static let historyItem2 = HistoryItemModel(
        event: batteryEvent2,
        deviceName: "Living Room Remote",
        deviceTypeName: "TV Remote",
        deviceLocation: "Living Room"
    )

    // MARK: - AI Messages

    static let aiMessage = AiMessage(
        id: "m1",
        role: .user,
        text: "What batteries does my remote use?",
        isPartial: false,
        hints: [:]
    )

    static let aiModelMessage = AiMessage(
        id: "m2",
        role: .model,
        text: "Your TV remote uses AAA batteries.",
        isPartial: false,
        hints: [:]
    )

    // MARK: - Input Types

    static let deviceInput = DeviceInput(
        name: "Living Room Remote",
        location: "Living Room",
        typeId: "t1",
        imagePath: nil
    )

    static let deviceTypeInput = DeviceTypeInput(
        name: "TV Remote",
        defaultIcon: "tv",
        batteryType: "AAA",
        batteryQuantity: 2
    )

    // MARK: - User

    static let user = User(
        id: "u1",
        email: "test@example.com",
        displayName: "Test User",
        photoUrl: nil
    )
}
