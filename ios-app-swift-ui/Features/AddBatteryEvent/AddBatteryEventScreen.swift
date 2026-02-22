import SwiftUI
import shared

struct AddBatteryEventScreen: View {
    @StateObject private var wrapper: AddBatteryEventViewModelWrapper
    @Environment(\.presentationMode) private var presentationMode
    
    @State private var selectedDeviceId: String?
    @State private var eventDate = Date()
    @State private var batteryType: String = ""
    @State private var notes: String = ""
    
    @State private var batchInput: String = ""
    
    init(viewModel: AddBatteryEventViewModel) {
        _wrapper = StateObject(wrappedValue: AddBatteryEventViewModelWrapper(viewModel))
    }
    
    var body: some View {
        Form {
            Section(header: Text("Add Event Manually")) {
                if wrapper.devices.isEmpty {
                    Text("No devices available. Please add a device first.")
                        .foregroundColor(.secondary)
                } else {
                    Picker("Device", selection: $selectedDeviceId) {
                        Text("Select a device").tag(String?.none)
                        ForEach(wrapper.devices, id: \.id) { device in
                            Text(device.name).tag(String?.some(device.id))
                        }
                    }
                    
                    DatePicker("Date & Time", selection: $eventDate)
                    
                    TextField("Battery Type (Optional)", text: $batteryType)
                    
                    TextField("Notes (Optional)", text: $notes)
                    
                    Button("Save Event") {
                        if let deviceId = selectedDeviceId {
                            wrapper.addEvent(
                                deviceId: deviceId,
                                date: eventDate,
                                batteryType: batteryType.isEmpty ? nil : batteryType,
                                notes: notes.isEmpty ? nil : notes
                            )
                            presentationMode.wrappedValue.dismiss()
                        }
                    }
                    .disabled(selectedDeviceId == nil)
                }
            }
            
            if wrapper.isAiBatchImportEnabled {
                Section(header: Text("Batch Import with AI")) {
                    TextEditor(text: $batchInput)
                        .frame(minHeight: 100)
                    
                    Button("Process Batch Data") {
                        wrapper.batchAddEvents(input: batchInput)
                        batchInput = ""
                    }
                    .disabled(batchInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    
                    if !wrapper.aiMessages.isEmpty {
                        ForEach(Array(wrapper.aiMessages.enumerated()), id: \.offset) { index, msg in
                            Text(getMessageText(msg))
                                .font(.caption)
                                .foregroundColor(getColor(for: msg))
                        }
                        
                        Button("Clear Messages") {
                            wrapper.clearAiMessages()
                        }
                        .foregroundColor(.red)
                    }
                }
            }
        }
        .navigationTitle("Add Battery Event")
        .navigationBarTitleDisplayMode(.inline)
    }
    
    private func getMessageText(_ result: BatchOperationResult) -> String {
        if let progress = result as? BatchOperationResultProgress {
            return progress.message
        } else if let success = result as? BatchOperationResultSuccess {
            return success.message
        } else if result is BatchOperationResultError {
            return "An error occurred during import."
        }
        return "Unknown status"
    }
    
    private func getColor(for result: BatchOperationResult) -> Color {
        if result is BatchOperationResultSuccess {
            return .green
        } else if result is BatchOperationResultError {
            return .red
        }
        return .primary
    }
}
