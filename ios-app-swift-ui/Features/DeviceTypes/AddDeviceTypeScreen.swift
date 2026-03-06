import SwiftUI
import shared

struct AddDeviceTypeScreen: View {
    @StateObject var viewModelWrapper: AddDeviceTypeViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    init(viewModel: AddDeviceTypeViewModel) {
        _viewModelWrapper = StateObject(wrappedValue: AddDeviceTypeViewModelWrapper(viewModel))
    }

    var body: some View {
        AddDeviceTypeContentView(
            state: viewModelWrapper.state,
            onUpdateName: { viewModelWrapper.updateName(name: $0) },
            onUpdateBatteryType: { viewModelWrapper.updateBatteryType(type: $0) },
            onSave: { viewModelWrapper.save() },
            onCancel: { dismiss() }
        )
        .onChange(of: viewModelWrapper.state.isSaved) { _, isSaved in
            if isSaved {
                viewModelWrapper.consumeSaveSuccess()
                dismiss()
            }
        }
    }
}

struct AddDeviceTypeContentView: View {
    let state: AddDeviceTypeState
    let onUpdateName: (String) -> Void
    let onUpdateBatteryType: (String) -> Void
    let onSave: () -> Void
    let onCancel: () -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Device Type Details")) {
                    TextField("Name", text: Binding(
                        get: { state.name },
                        set: { onUpdateName($0) }
                    ))

                    TextField("Battery Type", text: Binding(
                        get: { state.batteryType },
                        set: { onUpdateBatteryType($0) }
                    ))
                }

                if let error = state.saveError {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle("Add Device Type")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onCancel()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onSave()
                    }
                    .disabled(state.isSaving || state.name.isEmpty)
                }
            }
            .disabled(state.isSaving)
            .overlay {
                if state.isSaving {
                    ProgressView()
                }
            }
        }
    }
}
