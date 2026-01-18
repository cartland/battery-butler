import SwiftUI
import shared
import Combine

// This file is referenced by Xcode project.
// Ideally should be renamed/moved to Features/Home/HomeViewModelWrapper.swift.

class HomeViewModelWrapper: ObservableObject {
    @Published var state: HomeUiState
    
    private let viewModel: HomeViewModel
    private let viewModelStore = KmpViewModelStore()
    private var task: Task<Void, Never>?
    
    init(_ viewModel: HomeViewModel) {
        self.viewModel = viewModel
        self.state = viewModel.uiState.value as! HomeUiState
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        self.task = Task { @MainActor [weak self] in
            for await newState in viewModel.uiState {
                if let state = newState as? HomeUiState {
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
