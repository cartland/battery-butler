import SwiftUI
import shared
import KMPObservableViewModelSwiftUI

struct AddBatteryEventScreen: View {
    // bb-ovm1: @StateViewModel replaces AddBatteryEventViewModelWrapper.
    @StateViewModel private var viewModel: AddBatteryEventViewModel
    @Environment(\.presentationMode) private var presentationMode

    @State private var selectedDeviceId: String?
    @State private var eventDate = Date()
    @State private var batteryType: String = ""
    @State private var notes: String = ""
    @State private var showMoreDetails = false

    @State private var batchInput: String = ""

    init(viewModel: AddBatteryEventViewModel) {
        _viewModel = StateViewModel(wrappedValue: viewModel)
    }

    var body: some View {
        AddBatteryEventContentView(
            devices: viewModel.devicesValue,
            selectedDeviceId: $selectedDeviceId,
            eventDate: $eventDate,
            batteryType: $batteryType,
            notes: $notes,
            showMoreDetails: $showMoreDetails,
            isAiBatchImportEnabled: viewModel.isAiBatchImportEnabledValue,
            aiMessages: viewModel.aiMessagesValue,
            batchInput: $batchInput,
            onSaveEvent: {
                if let deviceId = selectedDeviceId {
                    let epochMillis = Int64(eventDate.timeIntervalSince1970 * 1000)
                    let instant = KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: epochMillis)
                    viewModel.addEvent(
                        deviceId: deviceId,
                        date: instant,
                        batteryType: batteryType.isEmpty ? nil : batteryType,
                        notes: notes.isEmpty ? nil : notes
                    )
                    presentationMode.wrappedValue.dismiss()
                }
            },
            onProcessBatch: {
                viewModel.batchAddEvents(input: batchInput)
                batchInput = ""
            },
            onClearAiMessages: { viewModel.clearAiMessages() }
        )
    }
}

// bb-ovm1: Option A manual state accessors (no NativeCoroutines).
extension AddBatteryEventViewModel {
    var devicesValue: [Device] { devices.value }
    var aiMessagesValue: [BatchOperationResult] { aiMessages.value }
    var isAiBatchImportEnabledValue: Bool { (isAiBatchImportEnabled.value as? Bool) ?? false }
}

struct AddBatteryEventContentView: View {
    let devices: [Device]
    @Binding var selectedDeviceId: String?
    @Binding var eventDate: Date
    @Binding var batteryType: String
    @Binding var notes: String
    @Binding var showMoreDetails: Bool
    let isAiBatchImportEnabled: Bool
    let aiMessages: [BatchOperationResult]
    @Binding var batchInput: String
    let onSaveEvent: () -> Void
    let onProcessBatch: () -> Void
    let onClearAiMessages: () -> Void

    var body: some View {
        Form {
            Section(header: Text("add_event.section.manual")) {
                if devices.isEmpty {
                    Text("add_event.no_devices")
                        .foregroundStyle(.secondary)
                } else {
                    Picker("add_event.field.device", selection: $selectedDeviceId) {
                        Text("add_event.field.select_device").tag(String?.none)
                        ForEach(devices, id: \.id) { device in
                            Text(device.name).tag(String?.some(device.id))
                        }
                    }

                    DatePicker("add_event.field.date_time", selection: $eventDate)

                    DisclosureGroup("add_event.field.more_details", isExpanded: $showMoreDetails) {
                        TextField("add_event.field.battery_type", text: $batteryType)

                        TextField("add_event.field.notes", text: $notes)
                    }

                    Button("add_event.button.save") {
                        onSaveEvent()
                    }
                    .disabled(selectedDeviceId == nil)
                }
            }

            if isAiBatchImportEnabled {
                Section(header: Text("add_event.section.batch")) {
                    TextEditor(text: $batchInput)
                        .frame(minHeight: 100)

                    Button("add_event.button.process_batch") {
                        onProcessBatch()
                    }
                    .disabled(batchInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                    if !aiMessages.isEmpty {
                        ForEach(Array(aiMessages.enumerated()), id: \.offset) { _, msg in
                            Text(getMessageText(msg))
                                .font(.caption)
                                .foregroundStyle(getColor(for: msg))
                        }

                        Button("add_event.button.clear_messages") {
                            onClearAiMessages()
                        }
                        .foregroundStyle(Color.butlerError)
                    }
                }
            }
        }
        .navigationTitle("add_event.title")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func getMessageText(_ result: BatchOperationResult) -> String {
        if let progress = result as? BatchOperationResultProgress {
            return progress.message
        } else if let success = result as? BatchOperationResultSuccess {
            return success.message
        } else if result is BatchOperationResultError {
            return String(localized: "add_event.batch.error")
        }
        return String(localized: "add_event.batch.unknown")
    }

    private func getColor(for result: BatchOperationResult) -> Color {
        if result is BatchOperationResultSuccess {
            return Color.butlerPrimary
        } else if result is BatchOperationResultError {
            return Color.butlerError
        }
        return .primary
    }
}
