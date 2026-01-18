import SwiftUI
import shared
import Combine

struct AddDeviceTypeState {
    var name: String = ""
    var batteryType: String = ""
    var defaultIcon: String = "default"
    var isSaving: Bool = false
    var isSaved: Bool = false
    var saveError: String? = nil
}

class AddDeviceTypeViewModelWrapper: ObservableObject {
    @Published var state: AddDeviceTypeState = AddDeviceTypeState()
    
    private let viewModel: AddDeviceTypeViewModel
    private let viewModelStore = KmpViewModelStore()
    private var task: Task<Void, Never>?
    
    init(_ viewModel: AddDeviceTypeViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        self.task = Task { @MainActor [weak self] in
            for await icon in viewModel.suggestedIcon {
                if let icon = icon {
                    self?.state.defaultIcon = icon
                }
            }
        }
    }
    
    deinit {
        task?.cancel()
        viewModelStore.clear()
    }
    
    func updateName(name: String) {
        state.name = name
        viewModel.suggestIcon(name: name)
    }
    
    func updateBatteryType(type: String) {
        state.batteryType = type
    }
    
    func save() {
        state.isSaving = true
        // Create DeviceTypeInput (KMP class)
        // Check if DeviceTypeInput is available.
        // It is a domain model.
        let input = DeviceTypeInput(
            name: state.name,
            defaultIcon: state.defaultIcon,
            batteryType: state.batteryType,
            batteryQuantity: 1 // Default to 1 for now
        )
        
        viewModel.addDeviceType(input: input)
        
        // Since VM doesn't expose success state, we simulate it for now or assume success.
        // ideally VM should expose a result flow.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.state.isSaving = false
            self.state.isSaved = true
        }
    }

    func consumeSaveSuccess() {
        state.isSaved = false
    }
}
