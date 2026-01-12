import SwiftUI
import shared
import Combine

class HistoryListViewModelWrapper: ObservableObject {
    @Published var state: HistoryListUiState
    
    private let viewModel: HistoryListViewModel
    private var collector: FlowCollector<HistoryListUiState>?
    
    init(_ viewModel: HistoryListViewModel) {
        self.viewModel = viewModel
        self.state = viewModel.uiState.value as! HistoryListUiState
        
        let col = FlowCollector<HistoryListUiState> { [weak self] newState in
            DispatchQueue.main.async {
                self?.state = newState
            }
        }
        self.collector = col
        viewModel.uiState.collect(collector: col) { _ in }
    }
}
