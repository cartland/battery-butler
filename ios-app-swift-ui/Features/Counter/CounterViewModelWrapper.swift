import SwiftUI
import shared

class CounterViewModelWrapper: ObservableObject {
    @Published var state: CounterState = CounterStateIdle()

    private let viewModel: CounterViewModel
    private let viewModelStore = KmpViewModelStore()
    private var stateTask: Task<Void, Never>?

    init(_ viewModel: CounterViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "counter-vm", viewModel: viewModel)

        self.stateTask = Task { @MainActor [weak self] in
            for await newState in viewModel.state {
                self?.state = newState
            }
        }
    }

    deinit {
        stateTask?.cancel()
        viewModelStore.clear()
    }

    func start() {
        viewModel.start()
    }

    func get() {
        viewModel.get()
    }
}
