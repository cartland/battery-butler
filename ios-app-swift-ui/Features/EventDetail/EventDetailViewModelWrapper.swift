import SwiftUI
import shared

class EventDetailViewModelWrapper: ObservableObject {
    @Published var state: EventDetailScreenState? = nil

    private let viewModel: EventDetailViewModel
    private let viewModelStore = KmpViewModelStore()
    private var stateTask: Task<Void, Never>?

    init(eventId: String, component: NativeComponent) {
        let factory = component.eventDetailViewModelFactory
        self.viewModel = factory.create(eventId: eventId)
        viewModelStore.put(key: "vm", viewModel: viewModel)

        // Ensure initial state is captured from StateFlow's value if available
        self.state = viewModel.uiState.value as? EventDetailScreenState

        self.stateTask = Task { @MainActor [weak self] in
            for await st in viewModel.uiState {
                self?.state = st as? EventDetailScreenState
            }
        }
    }

    deinit {
        stateTask?.cancel()
        viewModelStore.clear()
    }
}
