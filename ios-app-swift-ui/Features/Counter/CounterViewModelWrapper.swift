import SwiftUI
import shared

class CounterViewModelWrapper: ObservableObject {
    @Published var counterRunning: Bool = false
    @Published var appCounterRunning: Bool = false
    @Published var observeState: CounterState = CounterState.Idle.shared
    @Published var getState: CounterState = CounterState.Idle.shared

    private let viewModel: CounterViewModel
    private let viewModelStore = KmpViewModelStore()
    private var counterRunningTask: Task<Void, Never>?
    private var appCounterRunningTask: Task<Void, Never>?
    private var observeTask: Task<Void, Never>?
    private var getTask: Task<Void, Never>?

    init(_ viewModel: CounterViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "counter-vm", viewModel: viewModel)

        self.counterRunningTask = Task { @MainActor [weak self] in
            for await newValue in viewModel.counterRunning {
                self?.counterRunning = newValue as! Bool
            }
        }

        self.appCounterRunningTask = Task { @MainActor [weak self] in
            for await newValue in viewModel.appCounterRunning {
                self?.appCounterRunning = newValue as! Bool
            }
        }

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
        counterRunningTask?.cancel()
        appCounterRunningTask?.cancel()
        observeTask?.cancel()
        getTask?.cancel()
        viewModelStore.clear()
    }

    func startCounter() {
        viewModel.startCounter()
    }

    func stopCounter() {
        viewModel.stopCounter()
    }

    func startAppCounter() {
        viewModel.startAppCounter()
    }

    func stopAppCounter() {
        viewModel.stopAppCounter()
    }

    func startObserving() {
        viewModel.startObserving()
    }

    func stopObserving() {
        viewModel.stopObserving()
    }

    func getOnce() {
        viewModel.getOnce()
    }
}
