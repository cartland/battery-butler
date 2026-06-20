import SwiftUI
import shared
import KMPObservableViewModelSwiftUI

struct DeviceTypeListScreen: View {
    // bb-ovm1: @StateViewModel replaces DeviceTypeListViewModelWrapper.
    @StateViewModel var viewModel: DeviceTypeListViewModel
    private let component: NativeComponent
    @State private var isAddTypePresented = false

    init(component: NativeComponent) {
        self.component = component
        _viewModel = StateViewModel(wrappedValue: component.deviceTypeListViewModel)
    }

    var body: some View {
        DeviceTypeListContentView(
            state: viewModel.uiStateValue,
            onAddTypeTapped: { isAddTypePresented = true },
            onSortOptionSelected: { viewModel.onSortOptionSelected(option: $0) },
            onGroupOptionSelected: { viewModel.onGroupOptionSelected(option: $0) },
            onSortDirectionToggle: { viewModel.toggleSortDirection() },
            onGroupDirectionToggle: { viewModel.toggleGroupDirection() },
            detailDestination: { typeId in
                DeviceTypeDetailScreen(component: component, typeId: typeId)
            }
        )
        .sheet(isPresented: $isAddTypePresented) {
            AddDeviceTypeScreen(viewModel: component.addDeviceTypeViewModel)
        }
    }
}

// bb-ovm1: Option A manual state accessor (no NativeCoroutines).
extension DeviceTypeListViewModel {
    var uiStateValue: DeviceTypeListScreenState { uiState.value }
}

struct DeviceTypeListContentView<DetailDestination: View>: View {
    let state: DeviceTypeListScreenState
    let onAddTypeTapped: () -> Void
    let onSortOptionSelected: (DeviceTypeSortOption) -> Void
    let onGroupOptionSelected: (DeviceTypeGroupOption) -> Void
    let onSortDirectionToggle: () -> Void
    let onGroupDirectionToggle: () -> Void
    let detailDestination: (String) -> DetailDestination

    var body: some View {
        List {
            if state is DeviceTypeListScreenStateLoading {
                ProgressView()
                    .accessibilityLabel("device_types.accessibility.loading")
            } else if let successState = state as? DeviceTypeListScreenStateSuccess {
                // Sort/Group filter row
                if !successState.groupedTypes.isEmpty {
                    Section {
                        DeviceTypeFilterRow(
                            sortOption: successState.sortOption,
                            groupOption: successState.groupOption,
                            isSortAscending: successState.isSortAscending,
                            isGroupAscending: successState.isGroupAscending,
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

                if successState.groupedTypes.isEmpty {
                    Text("device_types.no_types")
                        .foregroundColor(.secondary)
                } else {
                    ForEach(successState.groupedTypes.keys.sorted(), id: \.self) { key in
                        Section(header: Text(key)) {
                            ForEach(successState.groupedTypes[key] ?? [], id: \.id) { type in
                                NavigationLink(destination: detailDestination(type.id)) {
                                    DeviceTypeRow(deviceType: type)
                                }
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("device_types.title")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: onAddTypeTapped) {
                    Image(systemName: "plus")
                        .accessibilityLabel("device_types.accessibility.add")
                }
            }
        }
    }
}

// MARK: - Device Type Filter Row

struct DeviceTypeFilterRow: View {
    let sortOption: DeviceTypeSortOption
    let groupOption: DeviceTypeGroupOption
    let isSortAscending: Bool
    let isGroupAscending: Bool
    let onSortOptionSelected: (DeviceTypeSortOption) -> Void
    let onGroupOptionSelected: (DeviceTypeGroupOption) -> Void
    let onSortDirectionToggle: () -> Void
    let onGroupDirectionToggle: () -> Void

    var body: some View {
        HStack(spacing: ButlerSpacing.small) {
            // Sort control
            Menu {
                ForEach(sortOptions, id: \.self) { option in
                    Button(deviceTypeSortOptionLabel(option)) {
                        onSortOptionSelected(option)
                    }
                }
            } label: {
                SortGroupControlView(
                    label: "\(String(localized: "sort.label")): \(deviceTypeSortOptionLabel(sortOption))",
                    isActive: true,
                    isAscending: isSortAscending,
                    onTapped: {},
                    onDirectionToggle: onSortDirectionToggle
                )
            }

            // Group control
            Menu {
                ForEach(groupOptions, id: \.self) { option in
                    Button(deviceTypeGroupOptionLabel(option)) {
                        onGroupOptionSelected(option)
                    }
                }
            } label: {
                SortGroupControlView(
                    label: "\(String(localized: "group.label")): \(deviceTypeGroupOptionLabel(groupOption))",
                    isActive: groupOption != .none,
                    isAscending: isGroupAscending,
                    onTapped: {},
                    onDirectionToggle: onGroupDirectionToggle
                )
            }

            Spacer()
        }
    }

    private var sortOptions: [DeviceTypeSortOption] {
        [.name, .batteryType]
    }

    private var groupOptions: [DeviceTypeGroupOption] {
        [.none, .batteryType]
    }

    private func deviceTypeSortOptionLabel(_ option: DeviceTypeSortOption) -> String {
        switch option {
        case .name: return String(localized: "sort.name")
        case .batteryType: return String(localized: "sort.battery_type")
        }
    }

    private func deviceTypeGroupOptionLabel(_ option: DeviceTypeGroupOption) -> String {
        switch option {
        case .none: return String(localized: "group.none")
        case .batteryType: return String(localized: "group.battery_type")
        }
    }
}
