import SwiftUI
import shared
import Combine

struct EditDeviceTypeState {
    var name: String = ""
    var batteryType: String = ""
    var defaultIcon: String = "default"
    var isSaving: Bool = false
    var isSaved: Bool = false
    var saveError: String? = nil
    
    var isLoading: Bool = true
    var isNotFound: Bool = false
    var originalId: String = "" // To track which one we are editing
}

class EditDeviceTypeViewModelWrapper: ObservableObject {
    @Published var state: EditDeviceTypeState = EditDeviceTypeState()
    
    private let viewModel: EditDeviceTypeViewModel
    private let viewModelStore = KmpViewModelStore()
    private var task: Task<Void, Never>?
    
    init(_ viewModel: EditDeviceTypeViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        self.task = Task { @MainActor [weak self] in
            for await newState in viewModel.uiState {
                self?.updateFromState(newState)
            }
        }
    }
    
    deinit {
        task?.cancel()
        viewModelStore.clear()
    }
    
    private func updateFromState(_ newState: EditDeviceTypeUiState) {
        if newState is EditDeviceTypeUiStateLoading {
            state.isLoading = true
            state.isNotFound = false
        } else if let success = newState as? EditDeviceTypeUiStateSuccess {
            state.isLoading = false
            state.isNotFound = false
            // Only update fields if we just loaded (or implement smarter dirty check)
            // For simplicity, we overwrite if we are receiving an update that matches our ID but has different data?
            // Actually, if user is typing, we shouldn't overwrite.
            // But initial load is critical.
            if state.originalId != success.deviceType.id {
                state.originalId = success.deviceType.id
                state.name = success.deviceType.name
                state.batteryType = success.deviceType.batteryType
                state.defaultIcon = success.deviceType.defaultIcon ?? "default"
            }
        } else if newState is EditDeviceTypeUiStateNotFound {
            state.isLoading = false
            state.isNotFound = true
        }
    }
    
    func updateName(name: String) {
        state.name = name
    }
    
    func updateBatteryType(type: String) {
        state.batteryType = type
    }
    
    func save() {
        state.isSaving = true
        let input = DeviceTypeInput(
            name: state.name,
            defaultIcon: state.defaultIcon,
            batteryType: state.batteryType,
            batteryQuantity: 1 // Default
        )
        
        viewModel.updateDeviceType(input: input)
        
        // Simulate save success - VM doesn't expose it yet
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.state.isSaving = false
            self.state.isSaved = true
        }
    }
    
    func delete() {
        viewModel.deleteDeviceType()
    }

    func consumeSaveSuccess() {
        state.isSaved = false
    }
}
