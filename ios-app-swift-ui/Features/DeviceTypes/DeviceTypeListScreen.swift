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
        List {
            let state = viewModelWrapper.state
            
            if state is DeviceTypeListUiStateLoading {
                ProgressView()
            } else if let successState = state as? DeviceTypeListUiStateSuccess {
                if successState.groupedTypes.isEmpty {
                    Text("No device types found")
                        .foregroundColor(.secondary)
                } else {
                    ForEach(successState.groupedTypes.keys.sorted(), id: \.self) { key in
                        Section(header: Text(key)) {
                            ForEach(successState.groupedTypes[key] ?? [], id: \.id) { type in
                                NavigationLink(destination: EditDeviceTypeScreen(
                                    factory: component.editDeviceTypeViewModelFactory,
                                    typeId: type.id
                                )) {
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
                Button(action: {
                    isAddTypePresented = true
                }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $isAddTypePresented) {
            AddDeviceTypeScreen(viewModel: component.addDeviceTypeViewModel)
        }
    }
}
