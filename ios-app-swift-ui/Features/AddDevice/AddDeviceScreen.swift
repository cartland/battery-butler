import SwiftUI
import shared

struct AddDeviceScreen: View {
    @StateObject private var wrapper: AddDeviceViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    @State private var name: String = ""
    @State private var location: String = ""
    @State private var selectedTypeId: String = ""

    init(viewModel: AddDeviceViewModel) {
        _wrapper = StateObject(wrappedValue: AddDeviceViewModelWrapper(viewModel))
    }

    var body: some View {
        AddDeviceContentView(
            name: $name,
            location: $location,
            selectedTypeId: $selectedTypeId,
            deviceTypes: wrapper.deviceTypes,
            isLoading: wrapper.isLoading,
            onAdd: {
                wrapper.addDevice(name: name, location: location, typeId: selectedTypeId)
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
    @Binding var location: String
    @Binding var selectedTypeId: String
    let deviceTypes: [DeviceType]
    let isLoading: Bool
    let onAdd: () -> Void
    let onCancel: () -> Void

    var body: some View {
        // NavigationStack used for proper toolbar support in sheets
        NavigationStack {
            Form {
                Section(header: Text("add_device.section.details")) {
                    TextField("add_device.field.name", text: $name)
                    TextField("add_device.field.location", text: $location)

                    Picker("add_device.field.type", selection: $selectedTypeId) {
                        Text("add_device.field.select_type").tag("")
                        ForEach(deviceTypes, id: \.id) { type in
                            Text(type.name).tag(type.id)
                        }
                    }
                }

                Section {
                    Button("add_device.button.add", action: onAdd)
                        .disabled(name.isEmpty || selectedTypeId.isEmpty || isLoading)
                }
            }
            .disabled(isLoading)
            .overlay {
                if isLoading {
                    ProgressView()
                }
            }
            .navigationTitle("add_device.title")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("common.cancel", action: onCancel)
                }
            }
        }
    }
}
