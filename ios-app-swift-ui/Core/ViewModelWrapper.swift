import SwiftUI
import shared
import Combine

// This file is referenced by Xcode project.
// Ideally should be renamed/moved to Features/Home/HomeViewModelWrapper.swift.

class HomeViewModelWrapper: ObservableObject {
    @Published var state: HomeScreenState

    private let viewModel: HomeViewModel
    private let viewModelStore = KmpViewModelStore()
    private var task: Task<Void, Never>?

    init(_ viewModel: HomeViewModel) {
        self.viewModel = viewModel
        guard let initialState = viewModel.uiState.value as? HomeScreenState else {
            fatalError("Expected HomeScreenState but got \(type(of: viewModel.uiState.value))")
        }
        self.state = initialState
        viewModelStore.put(key: "vm", viewModel: viewModel)

        self.task = Task { @MainActor [weak self] in
            for await newState in viewModel.uiState {
                if let state = newState as? HomeScreenState {
                    self?.state = state
                }
            }
        }
    }

    func onSortOptionSelected(_ option: SortOption) {
        viewModel.onSortOptionSelected(option: option)
    }

    func onGroupOptionSelected(_ option: GroupOption) {
        viewModel.onGroupOptionSelected(option: option)
    }

    func toggleSortDirection() {
        viewModel.toggleSortDirection()
    }

    func toggleGroupDirection() {
        viewModel.toggleGroupDirection()
    }

    deinit {
        task?.cancel()
        viewModelStore.clear()
    }
}
