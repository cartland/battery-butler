import SwiftUI
import shared

struct EditDeviceTypeScreen: View {
    @StateObject var viewModelWrapper: EditDeviceTypeViewModelWrapper
    @Environment(\.presentationMode) var presentationMode
    @State private var showDeleteConfirmation = false
    
    init(factory: EditDeviceTypeViewModelFactory, typeId: String) {
        _viewModelWrapper = StateObject(wrappedValue: EditDeviceTypeViewModelWrapper(factory.create(typeId: typeId)))
    }
    
    var body: some View {
        Form {
            let state = viewModelWrapper.state
            
            if state.isLoading {
                ProgressView()
            } else if state.isNotFound {
                Text("Device Type not found")
            } else {
                Section(header: Text("Details")) {
                    TextField("Name", text: Binding(
                        get: { state.name },
                        set: { viewModelWrapper.updateName(name: $0) }
                    ))
                    
                    TextField("Battery Type", text: Binding(
                        get: { state.batteryType },
                        set: { viewModelWrapper.updateBatteryType(type: $0) }
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
                
                if state.isSaved {
                    Text("Saved!")
                        .onAppear {
                            viewModelWrapper.consumeSaveSuccess()
                            presentationMode.wrappedValue.dismiss()
                        }
                }
            }
        }
        .navigationTitle("Edit Device Type")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") {
                    viewModelWrapper.save()
                }
                // Disable if loading or not found
                .disabled(viewModelWrapper.state.isLoading || viewModelWrapper.state.isNotFound)
            }
        }
        .alert("Delete Device Type?", isPresented: $showDeleteConfirmation) {
            Button("Delete", role: .destructive) {
                viewModelWrapper.delete()
                presentationMode.wrappedValue.dismiss()
            }
            Button("Cancel", role: .cancel) { }
        } message: {
            Text("This action cannot be undone.")
        }
    }
}
