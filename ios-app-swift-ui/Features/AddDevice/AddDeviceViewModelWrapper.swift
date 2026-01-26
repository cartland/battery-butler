import SwiftUI
import shared
import Combine

class AddDeviceViewModelWrapper: ObservableObject {
    @Published var deviceTypes: [DeviceType] = []
    @Published var aiMessages: [BatchOperationResult] = []
    
    private let viewModel: AddDeviceViewModel
    private let viewModelStore = KmpViewModelStore()
    
    // Holders for our Tasks to prevent deallocation
    private var typesTask: Task<Void, Never>?
    private var aiTask: Task<Void, Never>?
    
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
    }
    
    deinit {
        typesTask?.cancel()
        aiTask?.cancel()
        viewModelStore.clear()
    }
    
    func addDevice(name: String, typeId: String) {
        let input = DeviceInput(
            name: name,
            location: "", // Simplified for now
            typeId: typeId,
            imagePath: nil
        )
        viewModel.addDevice(input: input)
    }
    
}
