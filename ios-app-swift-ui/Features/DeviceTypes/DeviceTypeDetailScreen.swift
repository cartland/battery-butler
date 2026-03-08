import SwiftUI
import shared

struct DeviceTypeDetailScreen: View {
    @StateObject private var wrapper: DeviceTypeDetailViewModelWrapper
    private let component: NativeComponent
    private let typeId: String
    @State private var showingEditDeviceType = false

    init(component: NativeComponent, typeId: String) {
        self.typeId = typeId
        self.component = component
        let viewModel = component.deviceTypeDetailViewModelFactory.create(typeId: typeId)
        _wrapper = StateObject(wrappedValue: DeviceTypeDetailViewModelWrapper(viewModel))
    }

    var body: some View {
        DeviceTypeDetailContentView(
            state: wrapper.state,
            deviceDestination: { deviceId in
                DeviceDetailScreen(component: component, deviceId: deviceId)
            }
        )
        .navigationTitle("Device Type")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Edit") {
                    showingEditDeviceType = true
                }
            }
        }
        .sheet(isPresented: $showingEditDeviceType) {
            EditDeviceTypeScreen(
                factory: component.editDeviceTypeViewModelFactory,
                typeId: typeId
            )
        }
    }
}

struct DeviceTypeDetailContentView<DeviceDestination: View>: View {
    let state: DeviceTypeDetailUiState
    let deviceDestination: (String) -> DeviceDestination

    var body: some View {
        Group {
            if let success = state as? DeviceTypeDetailUiStateSuccess {
                let sfSymbol = SFSymbolMapper.sfSymbolName(for: success.deviceType.defaultIcon)

                ScrollView {
                    VStack(alignment: .leading, spacing: ButlerSpacing.standard) {
                        // Header
                        VStack(spacing: ButlerSpacing.small) {
                            ButlerIconBox(systemName: sfSymbol)
                            .accessibilityHidden(true)

                            Text(success.deviceType.name)
                                .font(.title)
                                .bold()
                        }
                        .frame(maxWidth: .infinity)
                        .padding(ButlerSpacing.standard)
                        .background(Color.butlerSurface)
                        .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.medium))
                        .overlay(
                            RoundedRectangle(cornerRadius: ButlerCornerRadius.medium)
                                .stroke(Color.butlerOutline.opacity(0.5), lineWidth: 1)
                        )

                        // Info rows
                        VStack(spacing: 0) {
                            HStack {
                                Text("Battery Type")
                                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                                Spacer()
                                Text(success.deviceType.batteryType.isEmpty
                                    ? "Not specified"
                                    : success.deviceType.batteryType)
                            }
                            .padding(ButlerSpacing.standard)

                            Divider()

                            HStack {
                                Text("Battery Quantity")
                                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                                Spacer()
                                Text("\(success.deviceType.batteryQuantity)")
                            }
                            .padding(ButlerSpacing.standard)
                        }
                        .background(Color.butlerSurface)
                        .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.medium))
                        .overlay(
                            RoundedRectangle(cornerRadius: ButlerCornerRadius.medium)
                                .stroke(Color.butlerOutline.opacity(0.5), lineWidth: 1)
                        )

                        Divider()

                        // Devices section
                        HStack {
                            Text("Devices")
                                .font(.title2)
                                .bold()
                            Text("\(success.devices.count)")
                                .font(.caption)
                                .padding(.horizontal, ButlerSpacing.small)
                                .padding(.vertical, ButlerSpacing.extraSmall)
                                .background(Color.butlerPrimary.opacity(0.12))
                                .foregroundStyle(Color.butlerPrimary)
                                .clipShape(Capsule())
                        }

                        if success.devices.isEmpty {
                            Text("No devices of this type")
                                .italic()
                                .foregroundStyle(Color.butlerOnSurfaceVariant)
                        } else {
                            ForEach(success.devices, id: \.id) { device in
                                NavigationLink(destination: deviceDestination(device.id)) {
                                    HStack {
                                        Text(device.name)
                                            .foregroundStyle(Color.butlerOnSurface)
                                        Spacer()
                                        Image(systemName: "chevron.right")
                                            .font(.caption)
                                            .foregroundStyle(Color.butlerOnSurfaceVariant)
                                    }
                                    .padding(ButlerSpacing.standard)
                                    .background(Color.butlerSurface)
                                    .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.medium))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: ButlerCornerRadius.medium)
                                            .stroke(Color.butlerOutline.opacity(0.5), lineWidth: 1)
                                    )
                                }
                            }
                        }
                    }
                    .padding()
                }
            } else if state is DeviceTypeDetailUiStateNotFound {
                Text("Device type not found")
            } else {
                ProgressView("Loading device type...")
            }
        }
    }
}
