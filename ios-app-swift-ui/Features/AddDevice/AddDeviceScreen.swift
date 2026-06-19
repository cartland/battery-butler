import SwiftUI
import shared
import KMPObservableViewModelSwiftUI

struct AddDeviceScreen: View {
    // bb-ovm1: @StateViewModel replaces AddDeviceViewModelWrapper.
    @StateViewModel private var viewModel: AddDeviceViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var name: String = ""
    @State private var location: String = ""
    @State private var selectedTypeId: String = ""

    init(viewModel: AddDeviceViewModel) {
        _viewModel = StateViewModel(wrappedValue: viewModel)
    }

    var body: some View {
        AddDeviceContentView(
            name: $name,
            location: $location,
            selectedTypeId: $selectedTypeId,
            deviceTypes: viewModel.deviceTypesValue,
            isLoading: viewModel.isLoadingValue,
            onAdd: {
                viewModel.addDevice(input: DeviceInput(
                    name: name,
                    location: location.isEmpty ? nil : location,
                    typeId: selectedTypeId,
                    imagePath: nil
                ))
                dismiss()
            },
            onCancel: {
                dismiss()
            }
        )
    }
}

// bb-ovm1: Option A manual state accessors (no NativeCoroutines).
extension AddDeviceViewModel {
    var deviceTypesValue: [DeviceType] { deviceTypes.value }
    var isLoadingValue: Bool { (isLoading.value as? Bool) ?? false }
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
