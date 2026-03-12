import SwiftUI
import shared
import Combine

class AddDeviceViewModelWrapper: ObservableObject {
    @Published var deviceTypes: [DeviceType] = []
    @Published var aiMessages: [BatchOperationResult] = []
    @Published var isLoading: Bool = false

    private let viewModel: AddDeviceViewModel
    private let viewModelStore = KmpViewModelStore()

    // Holders for our Tasks to prevent deallocation
    private var typesTask: Task<Void, Never>?
    private var aiTask: Task<Void, Never>?
    private var loadingTask: Task<Void, Never>?

    init(_ viewModel: AddDeviceViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)

        // Types Subscription
        self.typesTask = Task { @MainActor [weak self] in
            for await types in viewModel.deviceTypes {
                self?.deviceTypes = types
            }
        }

        // AI Messages Subscription
        self.aiTask = Task { @MainActor [weak self] in
            for await msgs in viewModel.aiMessages {
                self?.aiMessages = msgs
            }
        }

        // Loading Subscription
        self.loadingTask = Task { @MainActor [weak self] in
            for await loading in viewModel.isLoading {
                self?.isLoading = loading.boolValue
            }
        }
    }

    deinit {
        typesTask?.cancel()
        aiTask?.cancel()
        loadingTask?.cancel()
        viewModelStore.clear()
    }
    
    func addDevice(name: String, location: String, typeId: String) {
        let input = DeviceInput(
            name: name,
            location: location.isEmpty ? nil : location,
            typeId: typeId,
            imagePath: nil
        )
        viewModel.addDevice(input: input)
    }
    
}
