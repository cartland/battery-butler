import shared
import Foundation

@MainActor
class EditBatteryEventViewModelWrapper: ObservableObject {
    private let viewModel: EditBatteryEventViewModel
    private let store: KmpViewModelStore

    @Published var state: EditBatteryEventScreenState

    init(eventId: String, component: NativeComponent) {
        self.store = KmpViewModelStore()
        self.viewModel = component.editBatteryEventViewModelFactory.create(eventId: eventId)

        // Initialize state to avoid optional
        self.state = self.viewModel.uiState.value

        // Start observing state flow
        Task {
            for await gs in viewModel.uiState {
                self.state = gs
            }
        }
    }

    deinit {
        store.clear()
    }

    func updateEvent(date: Date, batteryType: String?, notes: String?) {
        let epochMillis = Int64(date.timeIntervalSince1970 * 1000)
        let instant = KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: epochMillis)
        viewModel.updateEvent(date: instant, batteryType: batteryType, notes: notes)
    }

    func deleteEvent() {
        viewModel.deleteEvent()
    }
}
