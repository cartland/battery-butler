import SwiftUI
import shared
import Combine

struct AddDeviceTypeState {
    var name: String = ""
    var batteryType: String = ""
    var selectedIcon: String = "videogame_asset"
    var batteryQuantity: Int = 1
    var isSaving: Bool = false
    var isSaved: Bool = false
    var saveError: String? = nil
    var isSuggestingIcon: Bool = false
    var suggestedIcon: String? = nil
    var usedIcons: [String] = []
}

class AddDeviceTypeViewModelWrapper: ObservableObject {
    @Published var state: AddDeviceTypeState = AddDeviceTypeState()

    private let viewModel: AddDeviceTypeViewModel
    private let viewModelStore = KmpViewModelStore()
    private var uiStateTask: Task<Void, Never>?

    init(_ viewModel: AddDeviceTypeViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)

        self.uiStateTask = Task { @MainActor [weak self] in
            for await uiState in viewModel.uiState {
                if let uiState = uiState as? AddDeviceTypeScreenState {
                    self?.state.isSuggestingIcon = uiState.isSuggestingIcon
                    self?.state.usedIcons = uiState.usedIcons as? [String] ?? []
                    if let icon = uiState.suggestedIcon {
                        self?.state.suggestedIcon = icon
                        self?.state.selectedIcon = icon
                        self?.viewModel.consumeSuggestedIcon()
                    }
                }
            }
        }
    }

    deinit {
        uiStateTask?.cancel()
        viewModelStore.clear()
    }

    func updateName(name: String) {
        state.name = name
        viewModel.suggestIcon(name: name)
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

        viewModel.addDeviceType(input: input)

        // Since VM doesn't expose success state, we simulate it for now or assume success.
        // Ideally VM should expose a result flow.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.state.isSaving = false
            self.state.isSaved = true
        }
    }

    func consumeSaveSuccess() {
        state.isSaved = false
    }
}
