import SwiftUI
import shared
import KMPObservableViewModelSwiftUI

struct EditDeviceScreen: View {
    // bb-ovm1: @StateViewModel replaces EditDeviceViewModelWrapper.
    @StateViewModel private var viewModel: EditDeviceViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var name: String = ""
    @State private var location: String = ""
    @State private var selectedTypeId: String = ""
    @State private var hasInitializedFields = false

    init(deviceId: String, component: NativeComponent) {
        _viewModel = StateViewModel(
            wrappedValue: component.editDeviceViewModelFactory.create(deviceId: deviceId)
        )
    }

    var body: some View {
        EditDeviceContentView(
            state: viewModel.uiStateValue,
            name: $name,
            location: $location,
            selectedTypeId: $selectedTypeId,
            hasInitializedFields: $hasInitializedFields,
            onSave: {
                viewModel.updateDevice(input: DeviceInput(
                    name: name,
                    location: location.isEmpty ? nil : location,
                    typeId: selectedTypeId,
                    imagePath: nil
                ))
                dismiss()
            },
            onDelete: {
                viewModel.deleteDevice()
                dismiss()
            },
            onCancel: {
                dismiss()
            }
        )
    }
}

// bb-ovm1: Option A manual state accessor (no NativeCoroutines).
extension EditDeviceViewModel {
    var uiStateValue: EditDeviceScreenState { uiState.value }
}

struct EditDeviceContentView: View {
    let state: EditDeviceScreenState
    @Binding var name: String
    @Binding var location: String
    @Binding var selectedTypeId: String
    @Binding var hasInitializedFields: Bool
    
    let onSave: () -> Void
    let onDelete: () -> Void
    let onCancel: () -> Void

    @State private var showDeleteConfirmation = false

    var body: some View {
        NavigationStack {
            Group {
                if let successState = state as? EditDeviceScreenStateSuccess {
                    Form {
                        Section(header: Text("edit_device.section.details")) {
                            TextField("edit_device.field.name", text: $name)
                            TextField("edit_device.field.location", text: $location)

                            Picker("edit_device.field.type", selection: $selectedTypeId) {
                                Text("edit_device.field.select_type").tag("")
                                ForEach(successState.deviceTypes, id: \.id) { type in
                                    Text(type.name).tag(type.id)
                                }
                            }
                        }

                        Section {
                            Button("edit_device.button.save", action: onSave)
                            .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || selectedTypeId.isEmpty)
                        }

                        Section {
                            Button("edit_device.button.delete", role: .destructive) {
                                showDeleteConfirmation = true
                            }
                        }
                    }
                    .alert("edit_device.alert.delete_title", isPresented: $showDeleteConfirmation) {
                        Button("common.delete", role: .destructive, action: onDelete)
                        Button("common.cancel", role: .cancel) {}
                    } message: {
                        Text("common.action_cannot_be_undone")
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
                } else if state is EditDeviceScreenStateLoading {
                    ProgressView("edit_device.loading")
                } else {
                    Text("edit_device.not_found")
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("edit_device.title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("common.cancel", action: onCancel)
                }
            }
        }
    }
}
