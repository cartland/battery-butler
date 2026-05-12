import SwiftUI
import shared

class EventDetailViewModelWrapper: ObservableObject {
    @Published var state: EventDetailScreenState? = nil

    private let viewModel: EventDetailViewModel
    private let viewModelStore = KmpViewModelStore()
    private var stateTask: Task<Void, Never>?

    init(eventId: String, component: NativeComponent) {
        let factory = component.eventDetailViewModelFactory
        let viewModel = factory.create(eventId: eventId)
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)

        // Ensure initial state is captured from StateFlow's value if available
        self.state = viewModel.uiState.value

        self.stateTask = Task { @MainActor [weak self] in
            for await st in viewModel.uiState {
                self?.state = st
            }
        }
    }

    deinit {
        stateTask?.cancel()
        viewModelStore.clear()
    }
}
