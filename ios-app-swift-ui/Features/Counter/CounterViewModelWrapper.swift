import SwiftUI
import shared

class CounterViewModelWrapper: ObservableObject {
    @Published var observeState: CounterState = CounterState.Idle.shared
    @Published var getState: CounterState = CounterState.Idle.shared

    private let viewModel: CounterViewModel
    private let viewModelStore = KmpViewModelStore()
    private var observeTask: Task<Void, Never>?
    private var getTask: Task<Void, Never>?

    init(_ viewModel: CounterViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "counter-vm", viewModel: viewModel)

        self.observeTask = Task { @MainActor [weak self] in
            for await newState in viewModel.observeState {
                self?.observeState = newState
            }
        }

        self.getTask = Task { @MainActor [weak self] in
            for await newState in viewModel.getState {
                self?.getState = newState
            }
        }
    }

    deinit {
        observeTask?.cancel()
        getTask?.cancel()
        viewModelStore.clear()
    }

    func start() {
        viewModel.start()
    }

    func stop() {
        viewModel.stop()
    }

    func get() {
        viewModel.get()
    }
}
