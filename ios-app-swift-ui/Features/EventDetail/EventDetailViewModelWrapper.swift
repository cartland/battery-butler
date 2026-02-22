import SwiftUI
import shared

class EventDetailViewModelWrapper: ObservableObject {
    @Published var state: EventDetailUiState? = nil
    
    private let viewModel: EventDetailViewModel
    private let viewModelStore = KmpViewModelStore()
    private var stateTask: Task<Void, Never>?
    
    init(eventId: String, component: NativeComponent) {
        let factory = component.eventDetailViewModelFactory
        self.viewModel = factory.create(eventId: eventId)
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        // Ensure initial state is captured from StateFlow's value if available
        self.state = viewModel.uiState.value as? EventDetailUiState
        
        self.stateTask = Task { @MainActor [weak self] in
            for await st in viewModel.uiState {
                self?.state = st as? EventDetailUiState
            }
        }
    }
    
    deinit {
        stateTask?.cancel()
        viewModelStore.clear()
    }
    
    func updateDate(newDate: Date) {
        let epochMillis = Int64(newDate.timeIntervalSince1970 * 1000)
        let instant = KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: epochMillis)
        viewModel.updateDate(newDate: instant)
    }
    
    func deleteEvent() {
        viewModel.deleteEvent()
    }
}
