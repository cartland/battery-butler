import SwiftUI
import shared
import Combine

class HistoryListViewModelWrapper: ObservableObject {
    @Published var state: HistoryListUiState
    
    private let viewModel: HistoryListViewModel
    private let viewModelStore = KmpViewModelStore()
    private var task: Task<Void, Never>?
    
    init(_ viewModel: HistoryListViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)
        self.state = viewModel.uiState.value as! HistoryListUiState
        
        self.task = Task { @MainActor [weak self] in
            for await newState in viewModel.uiState {
                if let state = newState as? HistoryListUiState {
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
