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
            onSortOptionSelected: { viewModelWrapper.onSortOptionSelected($0) },
            onGroupOptionSelected: { viewModelWrapper.onGroupOptionSelected($0) },
            onSortDirectionToggle: { viewModelWrapper.toggleSortDirection() },
            onGroupDirectionToggle: { viewModelWrapper.toggleGroupDirection() },
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
    let state: HomeScreenState
    let onAddDeviceTapped: () -> Void
    let onAddEventTapped: () -> Void
    let onSortOptionSelected: (SortOption) -> Void
    let onGroupOptionSelected: (GroupOption) -> Void
    let onSortDirectionToggle: () -> Void
    let onGroupDirectionToggle: () -> Void
    let deviceDestination: (String) -> DeviceDestination
    let settingsDestination: () -> SettingsDestination
    let aiDestination: () -> AiDestination

    @State private var showSortMenu = false
    @State private var showGroupMenu = false

    private var isSyncing: Bool {
        state.syncStatus is SyncStatusSyncing
    }

    var body: some View {
        ZStack(alignment: .top) {
            List {
                // Sort/Group filter row
                if !state.groupedDevices.isEmpty {
                    Section {
                        HomeFilterRow(
                            sortOption: state.sortOption,
                            groupOption: state.groupOption,
                            isSortAscending: state.isSortAscending,
                            isGroupAscending: state.isGroupAscending,
                            onSortOptionSelected: onSortOptionSelected,
                            onGroupOptionSelected: onGroupOptionSelected,
                            onSortDirectionToggle: onSortDirectionToggle,
                            onGroupDirectionToggle: onGroupDirectionToggle
                        )
                    }
                    .listRowInsets(EdgeInsets(
                        top: ButlerSpacing.small,
                        leading: ButlerSpacing.standard,
                        bottom: ButlerSpacing.small,
                        trailing: ButlerSpacing.standard
                    ))
                    .listRowBackground(Color.clear)
                }

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

            // Sync status indicator overlay
            if isSyncing {
                HStack(spacing: ButlerSpacing.small) {
                    ProgressView()
                        .scaleEffect(0.7)
                    Text("home.syncing")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundStyle(Color.butlerOnSurfaceVariant)
                }
                .padding(.horizontal, ButlerSpacing.medium)
                .padding(.vertical, ButlerSpacing.small)
                .background(Color.butlerSurfaceVariant.opacity(0.9))
                .clipShape(Capsule())
                .shadow(radius: 4)
                .padding(.top, ButlerSpacing.small)
                .transition(.opacity)
                .animation(.easeInOut(duration: 0.3), value: isSyncing)
                .accessibilityLabel("home.syncing")
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

// MARK: - Home Filter Row

struct HomeFilterRow: View {
    let sortOption: SortOption
    let groupOption: GroupOption
    let isSortAscending: Bool
    let isGroupAscending: Bool
    let onSortOptionSelected: (SortOption) -> Void
    let onGroupOptionSelected: (GroupOption) -> Void
    let onSortDirectionToggle: () -> Void
    let onGroupDirectionToggle: () -> Void

    var body: some View {
        HStack(spacing: ButlerSpacing.small) {
            // Sort control
            Menu {
                ForEach(sortOptions, id: \.self) { option in
                    Button(sortOptionLabel(option)) {
                        onSortOptionSelected(option)
                    }
                }
            } label: {
                SortGroupControlView(
                    label: "\(String(localized: "sort.label")): \(sortOptionLabel(sortOption))",
                    isActive: true,
                    isAscending: isSortAscending,
                    onTapped: {},
                    onDirectionToggle: onSortDirectionToggle
                )
            }

            // Group control
            Menu {
                ForEach(groupOptions, id: \.self) { option in
                    Button(groupOptionLabel(option)) {
                        onGroupOptionSelected(option)
                    }
                }
            } label: {
                SortGroupControlView(
                    label: "\(String(localized: "group.label")): \(groupOptionLabel(groupOption))",
                    isActive: groupOption != .none,
                    isAscending: isGroupAscending,
                    onTapped: {},
                    onDirectionToggle: onGroupDirectionToggle
                )
            }

            Spacer()
        }
    }

    private var sortOptions: [SortOption] {
        [.name, .location, .batteryAge, .type]
    }

    private var groupOptions: [GroupOption] {
        [.none, .type, .location]
    }

    private func sortOptionLabel(_ option: SortOption) -> String {
        switch option {
        case .name: return String(localized: "sort.name")
        case .location: return String(localized: "sort.location")
        case .batteryAge: return String(localized: "sort.battery_age")
        case .type: return String(localized: "sort.type")
        }
    }

    private func groupOptionLabel(_ option: GroupOption) -> String {
        switch option {
        case .none: return String(localized: "group.none")
        case .type: return String(localized: "group.type")
        case .location: return String(localized: "group.location")
        }
    }
}
