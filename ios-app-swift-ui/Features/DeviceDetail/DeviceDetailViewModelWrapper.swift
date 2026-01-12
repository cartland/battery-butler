import SwiftUI
import shared
import Combine

class DeviceDetailViewModelWrapper: ObservableObject {
    @Published var state: DeviceDetailUiState
    
    private let viewModel: DeviceDetailViewModel
    private var collector: FlowCollector<DeviceDetailUiState>?
    
    // Factory method wrapper or direct init if we have the VM
    init(_ viewModel: DeviceDetailViewModel) {
        self.viewModel = viewModel
        // Initial state
        self.state = viewModel.uiState.value as! DeviceDetailUiState
        
        let col = FlowCollector<DeviceDetailUiState> { [weak self] newState in
            DispatchQueue.main.async {
                self?.state = newState
            }
        }
        self.collector = col
        viewModel.uiState.collect(collector: col) { _ in }
    }
    
    // Actions
    func recordReplacement() {
        viewModel.recordReplacement()
    }
}
