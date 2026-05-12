import SwiftUI
import shared
import Combine

class DeviceDetailViewModelWrapper: ObservableObject {
    @Published var state: DeviceDetailScreenState
    
    private let viewModel: DeviceDetailViewModel
    private let viewModelStore = KmpViewModelStore()
    private var task: Task<Void, Never>?
    
    // Factory method wrapper or direct init if we have the VM
    init(_ viewModel: DeviceDetailViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)

        self.state = viewModel.uiState.value

        self.task = Task { @MainActor [weak self] in
            for await newState in viewModel.uiState {
                self?.state = newState
            }
        }
    }
    
    deinit {
        task?.cancel()
        viewModelStore.clear()
    }
    
    // Actions
    func recordReplacement() {
        viewModel.recordReplacement()
    }
}
