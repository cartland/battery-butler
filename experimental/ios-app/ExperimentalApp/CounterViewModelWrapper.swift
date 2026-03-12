import Combine
import ExperimentalShared
import SwiftUI

class CounterViewModelWrapper: ObservableObject {
    @Published var observeState: CounterState = CounterState.Idle.shared
    @Published var getState: CounterState = CounterState.Idle.shared

    private let viewModel: CounterViewModel
    private var timer: AnyCancellable?

    init(_ viewModel: CounterViewModel) {
        self.viewModel = viewModel

        // Poll ViewModel state flows at ~60Hz to bridge Kotlin StateFlow → SwiftUI.
        // Intentional simplification: the main app uses SKIE for zero-copy Flow interop,
        // but polling is sufficient for this teaching reference and avoids the SKIE dependency.
        self.timer = Timer.publish(every: 1.0 / 60.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self else { return }
                self.observeState = viewModel.observeState.value as! CounterState
                self.getState = viewModel.getState.value as! CounterState
            }
    }

    deinit {
        timer?.cancel()
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
