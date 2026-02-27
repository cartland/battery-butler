import SwiftUI
import shared

struct AddDeviceScreen: View {
    @StateObject private var wrapper: AddDeviceViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    @State private var name: String = ""
    @State private var selectedTypeId: String = ""

    init(viewModel: AddDeviceViewModel) {
        _wrapper = StateObject(wrappedValue: AddDeviceViewModelWrapper(viewModel))
    }

    var body: some View {
        AddDeviceContentView(
            name: $name,
            selectedTypeId: $selectedTypeId,
            deviceTypes: wrapper.deviceTypes,
            onAdd: {
                wrapper.addDevice(name: name, typeId: selectedTypeId)
                dismiss()
            },
            onCancel: {
                dismiss()
            }
        )
    }
}

struct AddDeviceContentView: View {
    @Binding var name: String
    @Binding var selectedTypeId: String
    let deviceTypes: [DeviceType]
    let onAdd: () -> Void
    let onCancel: () -> Void
    
    var body: some View {
        // NavigationStack used for proper toolbar support in sheets
        NavigationStack {
            Form {
                Section(header: Text("Device Details")) {
                    TextField("Device Name", text: $name)

                    Picker("Type", selection: $selectedTypeId) {
                        Text("Select Type").tag("")
                        ForEach(deviceTypes, id: \.id) { type in
                            Text(type.name).tag(type.id)
                        }
                    }
                }

                Section {
                    Button("Add Device", action: onAdd)
                    .disabled(name.isEmpty || selectedTypeId.isEmpty)
                }
            }
            .navigationTitle("Add Device")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onCancel)
                }
            }
        }
    }
}
