import XCTest
@testable import BatteryButler
import shared

/// Regression tests for KMP-Swift interop.
///
/// These tests verify that every KMP type used at the iOS boundary can be
/// constructed from Swift. If a KMP API change breaks a constructor or
/// renames a type, these tests fail immediately at compile time (or runtime
/// for value assertions).
final class KmpInteropTests: XCTestCase {

    // MARK: - Data Class Constructors

    func testDeviceConstruction() {
        let epoch: Int64 = 1704067200000
        let instant = KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: epoch)
        let device = Device(
            id: "d1",
            name: "Test Device",
            typeId: "t1",
            batteryLastReplaced: instant,
            lastUpdated: instant,
            location: "Room",
            imagePath: nil,
            imageEtag: nil
        )
        XCTAssertEqual(device.id, "d1")
        XCTAssertEqual(device.name, "Test Device")
        XCTAssertEqual(device.typeId, "t1")
        XCTAssertEqual(device.location, "Room")
        XCTAssertNil(device.imagePath)
    }

    func testDeviceTypeConstruction() {
        let dt = DeviceType(
            id: "t1",
            name: "Remote",
            defaultIcon: "tv",
            batteryType: "AAA",
            batteryQuantity: 2
        )
        XCTAssertEqual(dt.id, "t1")
        XCTAssertEqual(dt.name, "Remote")
        XCTAssertEqual(dt.defaultIcon, "tv")
        XCTAssertEqual(dt.batteryType, "AAA")
        XCTAssertEqual(dt.batteryQuantity, 2)
    }

    func testBatteryEventConstruction() {
        let instant = KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000)
        let event = BatteryEvent(
            id: "e1",
            deviceId: "d1",
            date: instant,
            batteryType: "AAA",
            notes: "Replaced"
        )
        XCTAssertEqual(event.id, "e1")
        XCTAssertEqual(event.deviceId, "d1")
        XCTAssertEqual(event.batteryType, "AAA")
        XCTAssertEqual(event.notes, "Replaced")
    }

    func testAiMessageConstruction() {
        // Verifies param order: id:role:text:isPartial:hints:
        let msg = AiMessage(
            id: "m1",
            role: .user,
            text: "Hello",
            isPartial: false,
            hints: [:]
        )
        XCTAssertEqual(msg.id, "m1")
        XCTAssertEqual(msg.role, .user)
        XCTAssertEqual(msg.text, "Hello")
        XCTAssertEqual(msg.isPartial, false)
    }

    func testUserConstruction() {
        let user = User(
            id: "u1",
            email: "test@example.com",
            displayName: "Test User",
            photoUrl: nil
        )
        XCTAssertEqual(user.id, "u1")
        XCTAssertEqual(user.email, "test@example.com")
        XCTAssertEqual(user.displayName, "Test User")
        XCTAssertNil(user.photoUrl)
    }

    func testDeviceInputConstruction() {
        let input = DeviceInput(
            name: "My Device",
            location: "Kitchen",
            typeId: "t1",
            imagePath: nil
        )
        XCTAssertEqual(input.name, "My Device")
        XCTAssertEqual(input.location, "Kitchen")
        XCTAssertEqual(input.typeId, "t1")
        XCTAssertNil(input.imagePath)
    }

    func testDeviceTypeInputConstruction() {
        let input = DeviceTypeInput(
            name: "Smoke Alarm",
            defaultIcon: "detector_smoke",
            batteryType: "9V",
            batteryQuantity: 1
        )
        XCTAssertEqual(input.name, "Smoke Alarm")
        XCTAssertEqual(input.defaultIcon, "detector_smoke")
        XCTAssertEqual(input.batteryType, "9V")
        XCTAssertEqual(input.batteryQuantity, 1)
    }

    func testHomeScreenStateConstruction() {
        // HomeScreenState has all-default params in Kotlin; Swift requires full-param init
        let state = HomeScreenState(
            groupedDevices: [:],
            deviceTypes: [:],
            isSortAscending: true,
            isGroupAscending: true,
            sortOption: .name,
            groupOption: .none,
            exportData: nil,
            syncStatus: SyncStatusIdle(),
            error: nil,
            deviceImagesByEtag: [:],
            densityOption: .expanded
        )
        XCTAssertTrue(state.isSortAscending)
        XCTAssertEqual(state.sortOption, .name)
        XCTAssertEqual(state.groupOption, .none)
        XCTAssertNil(state.exportData)
        XCTAssertNil(state.error)
        XCTAssertEqual(state.densityOption, .expanded)
    }

    // MARK: - Sealed Interface Subtypes

    func testAuthStateSubtypes() {
        let unknown: AuthState = AuthStateUnknown()
        // Unauthenticated is a data class with a required `cause` in Swift — Kotlin default
        // arguments don't export through the ObjC bridge, so the cause is always explicit here.
        let unauth: AuthState = AuthStateUnauthenticated(cause: .signedOut)
        let expired: AuthState = AuthStateUnauthenticated(cause: .sessionExpired)
        let authing: AuthState = AuthStateAuthenticating()

        let user = User(id: "u1", email: nil, displayName: nil, photoUrl: nil)
        let authed: AuthState = AuthStateAuthenticated(user: user)

        let error: AuthError = AuthErrorUnknown(message: "fail", cause: nil)
        let failed: AuthState = AuthStateFailed(error: error)

        // Verify type membership via is-checks
        XCTAssertTrue(unknown is AuthStateUnknown)
        XCTAssertTrue(unauth is AuthStateUnauthenticated)
        XCTAssertTrue(expired is AuthStateUnauthenticated)
        XCTAssertTrue(authing is AuthStateAuthenticating)
        XCTAssertTrue(authed is AuthStateAuthenticated)
        XCTAssertTrue(failed is AuthStateFailed)
        XCTAssertEqual((unauth as? AuthStateUnauthenticated)?.cause, .signedOut)
        XCTAssertEqual((expired as? AuthStateUnauthenticated)?.cause, .sessionExpired)
    }

    func testSyncStatusSubtypes() {
        let idle: SyncStatus = SyncStatusIdle()
        let syncing: SyncStatus = SyncStatusSyncing()
        let success: SyncStatus = SyncStatusSuccess()

        XCTAssertTrue(idle is SyncStatusIdle)
        XCTAssertTrue(syncing is SyncStatusSyncing)
        XCTAssertTrue(success is SyncStatusSuccess)
    }

    func testDeviceDetailScreenStateSubtypes() {
        let loading: DeviceDetailScreenState = DeviceDetailScreenStateLoading()
        let notFound: DeviceDetailScreenState = DeviceDetailScreenStateNotFound()

        let device = TestData.device
        let deviceType = TestData.deviceType
        let events = [TestData.batteryEvent]
        let success: DeviceDetailScreenState = DeviceDetailScreenStateSuccess(
            device: device,
            deviceType: deviceType,
            events: events,
            imageBytes: nil
        )

        let error: DeviceDetailScreenState = DeviceDetailScreenStateError(message: "boom")

        XCTAssertTrue(loading is DeviceDetailScreenStateLoading)
        XCTAssertTrue(notFound is DeviceDetailScreenStateNotFound)
        XCTAssertTrue(success is DeviceDetailScreenStateSuccess)
        XCTAssertTrue(error is DeviceDetailScreenStateError)
        XCTAssertEqual((error as? DeviceDetailScreenStateError)?.message, "boom")
    }

    func testBatchOperationResultSubtypes() {
        let progress: BatchOperationResult = BatchOperationResultProgress(message: "Working...")
        let success: BatchOperationResult = BatchOperationResultSuccess(message: "Done")
        let dataError: DataError = DataErrorUnknown(message: "oops", cause: nil)
        let error: BatchOperationResult = BatchOperationResultError(error: dataError)

        XCTAssertTrue(progress is BatchOperationResultProgress)
        XCTAssertTrue(success is BatchOperationResultSuccess)
        XCTAssertTrue(error is BatchOperationResultError)
    }

    // MARK: - Enum Values

    func testAiRoleValues() {
        // Verify .user, .model, .system, .tool exist (NOT .assistant)
        let roles: [AiRole] = [.user, .model, .system, .tool]
        XCTAssertEqual(roles.count, 4)
    }

    func testSortAndGroupOptionValues() {
        let sortOptions: [SortOption] = [.name, .location, .batteryAge, .type]
        XCTAssertEqual(sortOptions.count, 4)

        let groupOptions: [GroupOption] = [.none, .type, .location]
        XCTAssertEqual(groupOptions.count, 3)

        let dtSortOptions: [DeviceTypeSortOption] = [.name, .batteryType]
        XCTAssertEqual(dtSortOptions.count, 2)

        let dtGroupOptions: [DeviceTypeGroupOption] = [.none, .batteryType]
        XCTAssertEqual(dtGroupOptions.count, 2)
    }

    // MARK: - Special Types

    func testKotlinInstantRoundTrip() {
        let epochMs: Int64 = 1704067200000
        let instant = KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: epochMs)
        let roundTripped = instant.toEpochMilliseconds()
        XCTAssertEqual(roundTripped, epochMs)
    }
}
