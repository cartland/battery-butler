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
            detailDestination: { typeId in
                DeviceTypeDetailScreen(component: component, typeId: typeId)
            }
        )
        .sheet(isPresented: $isAddTypePresented) {
            AddDeviceTypeScreen(viewModel: component.addDeviceTypeViewModel)
        }
    }
}

struct DeviceTypeListContentView<DetailDestination: View>: View {
    let state: DeviceTypeListUiState
    let onAddTypeTapped: () -> Void
    let detailDestination: (String) -> DetailDestination

    var body: some View {
        List {
            if state is DeviceTypeListUiStateLoading {
                ProgressView()
                    .accessibilityLabel("device_types.accessibility.loading")
            } else if let successState = state as? DeviceTypeListUiStateSuccess {
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
