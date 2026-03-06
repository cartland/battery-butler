import SwiftUI
import shared

struct DeviceTypeListScreen: View {
    @StateObject var viewModelWrapper: DeviceTypeListViewModelWrapper
    private let component: NativeComponent
    @State private var isAddTypePresented = false
    
    init(component: NativeComponent) {
        self.component = component
        _viewModelWrapper = StateObject(wrappedValue: DeviceTypeListViewModelWrapper(component.deviceTypeListViewModel))
    }
    
    var body: some View {
        DeviceTypeListContentView(
            state: viewModelWrapper.state,
            onAddTypeTapped: { isAddTypePresented = true },
            editDestination: { typeId in
                EditDeviceTypeScreen(
                    factory: component.editDeviceTypeViewModelFactory,
                    typeId: typeId
                )
            }
        )
        .sheet(isPresented: $isAddTypePresented) {
            AddDeviceTypeScreen(viewModel: component.addDeviceTypeViewModel)
        }
    }
}

struct DeviceTypeListContentView<EditDestination: View>: View {
    let state: DeviceTypeListUiState
    let onAddTypeTapped: () -> Void
    let editDestination: (String) -> EditDestination

    var body: some View {
        List {
            if state is DeviceTypeListUiStateLoading {
                ProgressView()
                    .accessibilityLabel("Loading device types")
            } else if let successState = state as? DeviceTypeListUiStateSuccess {
                if successState.groupedTypes.isEmpty {
                    Text("No device types found")
                        .foregroundColor(.secondary)
                } else {
                    ForEach(successState.groupedTypes.keys.sorted(), id: \.self) { key in
                        Section(header: Text(key)) {
                            ForEach(successState.groupedTypes[key] ?? [], id: \.id) { type in
                                NavigationLink(destination: editDestination(type.id)) {
                                    DeviceTypeRow(deviceType: type)
                                }
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Device Types")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: onAddTypeTapped) {
                    Image(systemName: "plus")
                        .accessibilityLabel("Add device type")
                }
            }
        }
    }
}
