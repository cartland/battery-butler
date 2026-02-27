import SwiftUI
import shared

struct EditDeviceScreen: View {
    @StateObject private var wrapper: EditDeviceViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    @State private var name: String = ""
    @State private var location: String = ""
    @State private var selectedTypeId: String = ""
    @State private var hasInitializedFields = false

    init(deviceId: String, component: NativeComponent) {
        _wrapper = StateObject(wrappedValue: EditDeviceViewModelWrapper(deviceId: deviceId, component: component))
    }

    var body: some View {
        EditDeviceContentView(
            state: wrapper.state,
            name: $name,
            location: $location,
            selectedTypeId: $selectedTypeId,
            hasInitializedFields: $hasInitializedFields,
            onSave: {
                wrapper.updateDevice(
                    name: name,
                    location: location.isEmpty ? nil : location,
                    typeId: selectedTypeId,
                    imagePath: nil
                )
                dismiss()
            },
            onDelete: {
                wrapper.deleteDevice()
                dismiss()
            },
            onCancel: {
                dismiss()
            }
        )
    }
}

struct EditDeviceContentView: View {
    let state: EditDeviceUiState
    @Binding var name: String
    @Binding var location: String
    @Binding var selectedTypeId: String
    @Binding var hasInitializedFields: Bool
    
    let onSave: () -> Void
    let onDelete: () -> Void
    let onCancel: () -> Void
    
    var body: some View {
        NavigationStack {
            Group {
                if let successState = state as? EditDeviceUiStateSuccess {
                    Form {
                        Section(header: Text("Device Details")) {
                            TextField("Device Name", text: $name)
                            TextField("Location (Optional)", text: $location)

                            Picker("Type", selection: $selectedTypeId) {
                                Text("Select Type").tag("")
                                ForEach(successState.deviceTypes, id: \.id) { type in
                                    Text(type.name).tag(type.id)
                                }
                            }
                        }

                        Section {
                            Button("Save Changes", action: onSave)
                            .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || selectedTypeId.isEmpty)
                        }
                        
                        Section {
                            Button("Delete Device", role: .destructive, action: onDelete)
                        }
                    }
                    .onAppear {
                        if !hasInitializedFields {
                            let device = successState.device
                            name = device.name
                            location = device.location ?? ""
                            selectedTypeId = device.typeId
                            hasInitializedFields = true
                        }
                    }
                } else if state is EditDeviceUiStateLoading {
                    ProgressView("Loading device...")
                } else {
                    Text("Device not found")
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("Edit Device")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onCancel)
                }
            }
        }
    }
}
