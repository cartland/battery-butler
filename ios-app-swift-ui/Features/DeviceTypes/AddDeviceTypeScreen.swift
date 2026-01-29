import SwiftUI
import shared

struct AddDeviceTypeScreen: View {
    @StateObject var viewModelWrapper: AddDeviceTypeViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    init(viewModel: AddDeviceTypeViewModel) {
        _viewModelWrapper = StateObject(wrappedValue: AddDeviceTypeViewModelWrapper(viewModel))
    }
    
    var body: some View {
        NavigationView {
            Form {
                let state = viewModelWrapper.state
                
                Section(header: Text("Device Type Details")) {
                    TextField("Name", text: Binding(
                        get: { state.name },
                        set: { viewModelWrapper.updateName(name: $0) }
                    ))
                    
                    TextField("Battery Type", text: Binding(
                        get: { state.batteryType },
                        set: { viewModelWrapper.updateBatteryType(type: $0) }
                    ))
                }
                
                if let error = state.saveError {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                    }
                }
                
                if state.isSaved {
                    Text("Saved!")
                        .onAppear {
                            viewModelWrapper.consumeSaveSuccess()
                            dismiss()
                        }
                }
            }
            .navigationTitle("Add Device Type")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        viewModelWrapper.save()
                    }
                    .disabled(viewModelWrapper.state.isSaving || viewModelWrapper.state.name.isEmpty)
                }
            }
            .disabled(viewModelWrapper.state.isSaving)
            .overlay {
                if viewModelWrapper.state.isSaving {
                    ProgressView()
                }
            }
        }
    }
}
