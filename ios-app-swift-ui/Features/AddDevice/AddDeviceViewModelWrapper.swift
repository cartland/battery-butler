import SwiftUI
import shared
import Combine

class AddDeviceViewModelWrapper: ObservableObject {
    @Published var deviceTypes: [DeviceType] = []
    @Published var aiMessages: [AiMessage] = []
    
    private let viewModel: AddDeviceViewModel
    private let viewModelStore = ViewModelStore()
    
    // Holders for our FlowCollectors to prevent deallocation
    private var typesCollector: FlowCollector<[DeviceType]>?
    private var aiCollector: FlowCollector<[AiMessage]>?
    
    init(_ viewModel: AddDeviceViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        // Subscribe to deviceTypes
        // We need to cast the initial value or handle it. 
        // StateFlow usually exposes .value, but verify type casting.
        // Assuming [DeviceType] comes through.
        
        // Types Subscription
        let tCollector = FlowCollector<[DeviceType]> { [weak self] types in
            DispatchQueue.main.async {
                self?.deviceTypes = types
            }
        }
        self.typesCollector = tCollector
        viewModel.deviceTypes.collect(collector: tCollector) { _ in }
        
        // AI Messages Subscription
        let aCollector = FlowCollector<[AiMessage]> { [weak self] msgs in
            DispatchQueue.main.async {
                self?.aiMessages = msgs
            }
        }
        self.aiCollector = aCollector
        viewModel.aiMessages.collect(collector: aCollector) { _ in }
    }
    
    deinit {
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
    
    func seedDeviceTypes() {
        viewModel.seedDeviceTypes()
    }
}
