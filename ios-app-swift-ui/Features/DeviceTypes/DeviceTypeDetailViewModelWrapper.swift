import SwiftUI
import shared
import Combine

class DeviceTypeDetailViewModelWrapper: ObservableObject {
    @Published var state: DeviceTypeDetailUiState

    private let viewModel: DeviceTypeDetailViewModel
    private let viewModelStore = KmpViewModelStore()
    private var task: Task<Void, Never>?

    init(_ viewModel: DeviceTypeDetailViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)

        guard let initialState = viewModel.uiState.value as? DeviceTypeDetailUiState else {
            fatalError("Expected DeviceTypeDetailUiState but got \(type(of: viewModel.uiState.value))")
        }
        self.state = initialState

        self.task = Task { @MainActor [weak self] in
            for await newState in viewModel.uiState {
                if let state = newState as? DeviceTypeDetailUiState {
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
