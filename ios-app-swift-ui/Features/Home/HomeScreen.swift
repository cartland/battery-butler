import SwiftUI
import shared

struct HomeScreen: View {
    @StateObject var viewModelWrapper: ViewModelWrapper<HomeUiState>
    private let component: NativeComponent // Using Component to access other VMs
    @State private var isAddDevicePresented = false
    
    init(component: NativeComponent) {
        self.component = component
        let viewModel = component.homeViewModel
        _viewModelWrapper = StateObject(wrappedValue: ViewModelWrapper(
            viewModel,
            viewModel.uiState.value as! HomeUiState,
            viewModel.uiState
        ))
    }
    
    var body: some View {
        List {
            let state = viewModelWrapper.state
            
            if state.groupedDevices.isEmpty {
                 Section(header: Text("Devices")) {
                     Text("No devices found")
                        .foregroundColor(.secondary)
                 }
            } else {
                ForEach(state.groupedDevices.keys.sorted(), id: \.self) { key in
                    Section(header: Text(key)) {
                        ForEach(state.groupedDevices[key] ?? [], id: \.id) { device in
                            NavigationLink(destination: DeviceDetailScreen(
                                factory: component.deviceDetailViewModelFactory,
                                deviceId: device.id
                            )) {
                                DeviceRow(device: device)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Battery Butler Native")
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                NavigationLink(destination: SettingsScreen(viewModel: component.settingsViewModel)) {
                    Image(systemName: "gear")
                }
            }
            
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    isAddDevicePresented = true
                }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $isAddDevicePresented) {
            AddDeviceScreen(viewModel: component.addDeviceViewModel)
        }
    }
}
