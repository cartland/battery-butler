import SwiftUI
import shared

struct HomeScreen: View {
    @StateObject var viewModelWrapper: HomeViewModelWrapper
    private let component: NativeComponent // Using Component to access other VMs
    @State private var isAddDevicePresented = false
    @State private var isAddEventPresented = false
    
    init(component: NativeComponent) {
        self.component = component
        _viewModelWrapper = StateObject(wrappedValue: HomeViewModelWrapper(component.homeViewModel))
    }
    
    var body: some View {
        HomeContentView(
            state: viewModelWrapper.state,
            onAddDeviceTapped: { isAddDevicePresented = true },
            onAddEventTapped: { isAddEventPresented = true },
            deviceDestination: { deviceId in
                DeviceDetailScreen(component: component, deviceId: deviceId)
            },
            settingsDestination: {
                SettingsScreen(viewModel: component.settingsViewModel)
            },
            aiDestination: {
                AiChatScreen(viewModel: component.aiChatViewModel)
            }
        )
        .sheet(isPresented: $isAddDevicePresented) {
            AddDeviceScreen(viewModel: component.addDeviceViewModel)
        }
        .sheet(isPresented: $isAddEventPresented) {
            AddBatteryEventScreen(viewModel: component.addBatteryEventViewModel)
        }
    }
}

struct HomeContentView<DeviceDestination: View, SettingsDestination: View, AiDestination: View>: View {
    let state: HomeUiState
    let onAddDeviceTapped: () -> Void
    let onAddEventTapped: () -> Void
    let deviceDestination: (String) -> DeviceDestination
    let settingsDestination: () -> SettingsDestination
    let aiDestination: () -> AiDestination

    var body: some View {
        List {
            if state.groupedDevices.isEmpty {
                 Section(header: Text("home.devices_section")) {
                     Text("home.no_devices")
                        .foregroundColor(.secondary)
                 }
            } else {
                ForEach(state.groupedDevices.keys.sorted(), id: \.self) { key in
                    Section(header: Text(key)) {
                        ForEach(state.groupedDevices[key] ?? [], id: \.id) { device in
                            NavigationLink(destination: deviceDestination(device.id)) {
                                DeviceRow(device: device, deviceType: state.deviceTypes[device.typeId])
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("home.title")
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                NavigationLink(destination: settingsDestination()) {
                    Image(systemName: "gear")
                        .accessibilityLabel("home.accessibility.settings")
                }
            }

            ToolbarItem(placement: .navigationBarTrailing) {
                HStack(spacing: 16) {
                    NavigationLink(destination: aiDestination()) {
                        Image(systemName: "wand.and.stars")
                            .accessibilityLabel("home.accessibility.ai_chat")
                    }
                    Button(action: onAddEventTapped) {
                        Image(systemName: "bolt.badge.plus")
                            .accessibilityLabel("home.accessibility.add_event")
                    }
                    Button(action: onAddDeviceTapped) {
                        Image(systemName: "plus")
                            .accessibilityLabel("home.accessibility.add_device")
                    }
                }
            }
        }
    }
}
