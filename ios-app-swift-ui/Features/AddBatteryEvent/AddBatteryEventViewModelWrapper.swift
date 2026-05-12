import SwiftUI
import shared

class AddBatteryEventViewModelWrapper: ObservableObject {
    @Published var devices: [shared.Device] = []
    @Published var aiMessages: [BatchOperationResult] = []
    @Published var isAiBatchImportEnabled: Bool = false
    
    private let viewModel: AddBatteryEventViewModel
    private let viewModelStore = KmpViewModelStore()
    
    private var devicesTask: Task<Void, Never>?
    private var aiMessagesTask: Task<Void, Never>?
    private var isAiBatchTask: Task<Void, Never>?
    
    init(_ viewModel: AddBatteryEventViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        self.devices = viewModel.devices.value
        self.aiMessages = viewModel.aiMessages.value
        self.isAiBatchImportEnabled = (viewModel.isAiBatchImportEnabled.value as? Bool) ?? false

        self.devicesTask = Task { @MainActor [weak self] in
            for await items in viewModel.devices {
                self?.devices = items
            }
        }

        self.aiMessagesTask = Task { @MainActor [weak self] in
            for await msgs in viewModel.aiMessages {
                self?.aiMessages = msgs
            }
        }
        
        self.isAiBatchTask = Task { @MainActor [weak self] in
            for await isEnabled in viewModel.isAiBatchImportEnabled {
                self?.isAiBatchImportEnabled = isEnabled.boolValue
            }
        }
    }
    
    deinit {
        devicesTask?.cancel()
        aiMessagesTask?.cancel()
        isAiBatchTask?.cancel()
        viewModelStore.clear()
    }
    
    func addEvent(deviceId: String, date: Date, batteryType: String?, notes: String?) {
        let epochMillis = Int64(date.timeIntervalSince1970 * 1000)
        let instant = KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: epochMillis)
        viewModel.addEvent(deviceId: deviceId, date: instant, batteryType: batteryType, notes: notes)
    }
    
    func batchAddEvents(input: String) {
        viewModel.batchAddEvents(input: input)
    }
    
    func clearAiMessages() {
        viewModel.clearAiMessages()
    }
}
