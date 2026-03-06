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
                Text("Device Type not found")
            } else {
                Section(header: Text("Details")) {
                    TextField("Name", text: Binding(
                        get: { state.name },
                        set: { onUpdateName($0) }
                    ))

                    TextField("Battery Type", text: Binding(
                        get: { state.batteryType },
                        set: { onUpdateBatteryType($0) }
                    ))
                }

                Section {
                    Button("Delete Type") {
                        showDeleteConfirmation = true
                    }
                    .foregroundColor(.red)
                }

                if let error = state.saveError {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                    }
                }
            }
        }
        .navigationTitle("Edit Device Type")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") {
                    onSave()
                }
                .disabled(state.isLoading || state.isNotFound)
            }
        }
        .alert("Delete Device Type?", isPresented: $showDeleteConfirmation) {
            Button("Delete", role: .destructive) {
                onDelete()
            }
            Button("Cancel", role: .cancel) { }
        } message: {
            Text("This action cannot be undone.")
        }
    }
}
