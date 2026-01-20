import SwiftUI
import shared

struct AddDeviceScreen: View {
    @StateObject private var wrapper: AddDeviceViewModelWrapper
    @Environment(\.presentationMode) var presentationMode
    
    @State private var name: String = ""
    @State private var selectedTypeId: String = ""
    
    init(viewModel: AddDeviceViewModel) {
        _wrapper = StateObject(wrappedValue: AddDeviceViewModelWrapper(viewModel))
    }
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Device Details")) {
                    TextField("Device Name", text: $name)
                    
                    Picker("Type", selection: $selectedTypeId) {
                        Text("Select Type").tag("")
                        ForEach(wrapper.deviceTypes, id: \.id) { type in
                            Text(type.name).tag(type.id)
                        }
                    }
                }
                
                Section {
                    Button("Add Device") {
                        wrapper.addDevice(name: name, typeId: selectedTypeId)
                        presentationMode.wrappedValue.dismiss()
                    }
                    .disabled(name.isEmpty || selectedTypeId.isEmpty)
                }
            }
            .navigationTitle("Add Device")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
            .onAppear {
                // No-op or equivalent if needed, but seedDeviceTypes is removed.
            }
        }
    }
}
