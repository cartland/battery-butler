import SwiftUI
import shared
import Combine

class DeviceTypeDetailViewModelWrapper: ObservableObject {
    @Published var state: DeviceTypeDetailScreenState

    private let viewModel: DeviceTypeDetailViewModel
    private let viewModelStore = KmpViewModelStore()
    private var task: Task<Void, Never>?

    init(_ viewModel: DeviceTypeDetailViewModel) {
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
}
