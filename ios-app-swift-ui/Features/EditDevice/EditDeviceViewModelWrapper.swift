import shared
import Foundation

@MainActor
class EditDeviceViewModelWrapper: ObservableObject {
    private let viewModel: EditDeviceViewModel
    private let store: KmpViewModelStore
    
    @Published var state: EditDeviceScreenState
    
    init(deviceId: String, component: NativeComponent) {
        self.store = KmpViewModelStore()
        self.viewModel = component.editDeviceViewModelFactory.create(deviceId: deviceId)
        
        // Initialize state to avoid optional
        self.state = self.viewModel.uiState.value
        
        // Start observing state flow
        Task {
            for await gs in viewModel.uiState {
                self.state = gs
            }
        }
    }
    
    deinit {
        store.clear()
    }
    
    func updateDevice(name: String, location: String?, typeId: String, imagePath: String?) {
        viewModel.updateDevice(input: DeviceInput(name: name, location: location, typeId: typeId, imagePath: imagePath))
    }
    
    func deleteDevice() {
        viewModel.deleteDevice()
    }
}
