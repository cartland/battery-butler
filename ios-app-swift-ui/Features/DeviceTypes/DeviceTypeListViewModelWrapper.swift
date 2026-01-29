import SwiftUI
import shared
import Combine

class DeviceTypeListViewModelWrapper: ObservableObject {
    @Published var state: DeviceTypeListUiState
    
    private let viewModel: DeviceTypeListViewModel
    private let viewModelStore = KmpViewModelStore()
    private var task: Task<Void, Never>?
    
    init(_ viewModel: DeviceTypeListViewModel) {
        self.viewModel = viewModel
        guard let initialState = viewModel.uiState.value as? DeviceTypeListUiState else {
            fatalError("Expected DeviceTypeListUiState but got \(type(of: viewModel.uiState.value))")
        }
        self.state = initialState
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        self.task = Task { @MainActor [weak self] in
            for await newState in viewModel.uiState {
                if let state = newState as? DeviceTypeListUiState {
                    self?.state = state
                }
            }
        }
    }
    
    deinit {
        task?.cancel()
        viewModelStore.clear()
    }
}
