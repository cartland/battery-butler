import SwiftUI
import shared

struct EditDeviceTypeScreen: View {
    @StateObject var viewModelWrapper: EditDeviceTypeViewModelWrapper
    @Environment(\.dismiss) private var dismiss
    @State private var showDeleteConfirmation = false

    init(factory: EditDeviceTypeViewModelFactory, typeId: String) {
        _viewModelWrapper = StateObject(wrappedValue: EditDeviceTypeViewModelWrapper(factory.create(typeId: typeId)))
    }

    var body: some View {
        EditDeviceTypeContentView(
            state: viewModelWrapper.state,
            onUpdateName: { viewModelWrapper.updateName(name: $0) },
            onUpdateBatteryType: { viewModelWrapper.updateBatteryType(type: $0) },
            onSave: { viewModelWrapper.save() },
            onDelete: {
                viewModelWrapper.delete()
                dismiss()
            }
        )
        .onChange(of: viewModelWrapper.state.isSaved) { _, isSaved in
            if isSaved {
                viewModelWrapper.consumeSaveSuccess()
                dismiss()
            }
        }
    }
}

struct EditDeviceTypeContentView: View {
    let state: EditDeviceTypeState
    let onUpdateName: (String) -> Void
    let onUpdateBatteryType: (String) -> Void
    let onSave: () -> Void
    let onDelete: () -> Void
    @State private var showDeleteConfirmation = false

    var body: some View {
        Form {
            if state.isLoading {
                ProgressView()
            } else if state.isNotFound {
                Text("edit_device_type.not_found")
            } else {
                Section(header: Text("edit_device_type.section.details")) {
                    TextField("edit_device_type.field.name", text: Binding(
                        get: { state.name },
                        set: { onUpdateName($0) }
                    ))

                    TextField("edit_device_type.field.battery_type", text: Binding(
                        get: { state.batteryType },
                        set: { onUpdateBatteryType($0) }
                    ))
                }

                Section {
                    Button("edit_device_type.button.delete") {
                        showDeleteConfirmation = true
                    }
                    .foregroundStyle(Color.butlerError)
                }

                if let error = state.saveError {
                    Section {
                        Text(error)
                            .foregroundStyle(Color.butlerError)
                    }
                }
            }
        }
        .navigationTitle("edit_device_type.title")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("common.save") {
                    onSave()
                }
                .disabled(state.isLoading || state.isNotFound)
            }
        }
        .alert("edit_device_type.alert.delete_title", isPresented: $showDeleteConfirmation) {
            Button("common.delete", role: .destructive) {
                onDelete()
            }
            Button("common.cancel", role: .cancel) {}
        } message: {
            Text("common.action_cannot_be_undone")
        }
    }
}
