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
                Section(header: Text("add_device_type.section.details")) {
                    TextField("add_device_type.field.name", text: Binding(
                        get: { state.name },
                        set: { onUpdateName($0) }
                    ))

                    TextField("add_device_type.field.battery_type", text: Binding(
                        get: { state.batteryType },
                        set: { onUpdateBatteryType($0) }
                    ))
                }

                if let error = state.saveError {
                    Section {
                        Text(error)
                            .foregroundStyle(Color.butlerError)
                    }
                }
            }
            .navigationTitle("add_device_type.title")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("common.cancel") {
                        onCancel()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("common.save") {
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
