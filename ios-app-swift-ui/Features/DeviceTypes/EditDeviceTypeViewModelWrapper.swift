import SwiftUI
import shared
import Combine

struct EditDeviceTypeState {
    var name: String = ""
    var batteryType: String = ""
    var selectedIcon: String = "videogame_asset"
    var batteryQuantity: Int = 1
    var isSaving: Bool = false
    var isSaved: Bool = false
    var saveError: String? = nil
    var usedIcons: [String] = []

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
            if state.originalId != success.deviceType.id {
                state.originalId = success.deviceType.id
                state.name = success.deviceType.name
                state.batteryType = success.deviceType.batteryType
                state.selectedIcon = success.deviceType.defaultIcon ?? "videogame_asset"
                state.batteryQuantity = Int(success.deviceType.batteryQuantity)
                state.usedIcons = success.usedIcons as? [String] ?? []
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

    func selectIcon(icon: String) {
        state.selectedIcon = icon
    }

    func incrementBatteryQuantity() {
        state.batteryQuantity += 1
    }

    func decrementBatteryQuantity() {
        if state.batteryQuantity > 1 {
            state.batteryQuantity -= 1
        }
    }

    func save() {
        state.isSaving = true
        let input = DeviceTypeInput(
            name: state.name,
            defaultIcon: state.selectedIcon,
            batteryType: state.batteryType,
            batteryQuantity: Int32(state.batteryQuantity)
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
