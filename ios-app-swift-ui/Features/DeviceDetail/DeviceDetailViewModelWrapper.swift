import SwiftUI
import shared
import Combine

class DeviceDetailViewModelWrapper: ObservableObject {
    @Published var state: DeviceDetailUiState
    
    private let viewModel: DeviceDetailViewModel
    private let viewModelStore = KmpViewModelStore()
    private var task: Task<Void, Never>?
    
    // Factory method wrapper or direct init if we have the VM
    init(_ viewModel: DeviceDetailViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        // Initial state
        self.state = viewModel.uiState.value as! DeviceDetailUiState
        
        self.task = Task { @MainActor [weak self] in
            for await newState in viewModel.uiState {
                if let state = newState as? DeviceDetailUiState {
                    self?.state = state
                }
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
