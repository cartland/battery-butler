import SwiftUI
import shared

struct DeviceDetailScreen: View {
    @StateObject private var wrapper: DeviceDetailViewModelWrapper
    private let component: NativeComponent
    private let deviceId: String
    @State private var showingEditDevice = false

    init(component: NativeComponent, deviceId: String) {
        self.deviceId = deviceId
        self.component = component
        let viewModel = component.deviceDetailViewModelFactory.create(deviceId: deviceId)
        _wrapper = StateObject(wrappedValue: DeviceDetailViewModelWrapper(viewModel))
    }

    var body: some View {
        DeviceDetailContentView(
            state: wrapper.state,
            onRecordReplacement: {
                wrapper.recordReplacement()
            },
            eventDestination: { eventId in
                EventDetailScreen(eventId: eventId, component: component)
            }
        )
        .navigationTitle("device_detail.title")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("common.edit") {
                    showingEditDevice = true
                }
            }
        }
        .sheet(isPresented: $showingEditDevice) {
            EditDeviceScreen(deviceId: deviceId, component: component)
        }
    }
}

struct DeviceDetailContentView<Destination: View>: View {
    let state: DeviceDetailUiState
    let onRecordReplacement: () -> Void
    let eventDestination: (String) -> Destination

    var body: some View {
        Group {
            if let success = state as? DeviceDetailUiStateSuccess {
                let sfSymbol = SFSymbolMapper.sfSymbolName(for: success.deviceType?.defaultIcon)

                ScrollView {
                    VStack(alignment: .leading, spacing: ButlerSpacing.standard) {
                        // Header
                        HStack(spacing: ButlerSpacing.standard) {
                            ButlerIconBox(systemName: sfSymbol)
                                .accessibilityHidden(true)

                            VStack(alignment: .leading) {
                                Text(success.device.name)
                                    .font(.title)
                                    .bold()
                                Text(success.deviceType?.name ?? success.device.typeId)
                                    .font(.headline)
                                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                            }
                            Spacer()
                        }
                        .padding(ButlerSpacing.standard)
                        .background(Color.butlerSurface)
                        .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.medium))
                        .overlay(
                            RoundedRectangle(cornerRadius: ButlerCornerRadius.medium)
                                .stroke(Color.butlerOutline.opacity(0.5), lineWidth: 1)
                        )

                        // Actions
                        Button(action: onRecordReplacement) {
                            Text("device_detail.replaced_battery")
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.butlerPrimary)
                                .foregroundStyle(.white)
                                .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.medium))
                        }
                        .accessibilityHint("device_detail.accessibility.replaced_battery_hint")

                        Divider()

                        // History Section
                        Text("device_detail.battery_history")
                            .font(.title2)
                            .bold()

                        if success.events.isEmpty {
                            Text("device_detail.no_history")
                                .italic()
                                .foregroundStyle(Color.butlerOnSurfaceVariant)
                        } else {
                            ForEach(success.events, id: \.self) { event in
                                NavigationLink(destination: eventDestination(event.id)) {
                                    HStack {
                                        let dateString = Date(
                                            timeIntervalSince1970: TimeInterval(event.date.toEpochMilliseconds()) / 1000.0
                                        ).formatted(date: .abbreviated, time: .shortened)
                                        Text(dateString)
                                            .foregroundStyle(Color.butlerOnSurface)
                                        Spacer()
                                        Text("common.details")
                                            .foregroundStyle(Color.butlerPrimary)
                                    }
                                    .padding(.vertical, ButlerSpacing.extraSmall)
                                }
                            }
                        }
                    }
                    .padding()
                }
            } else if state is DeviceDetailUiStateNotFound {
                Text("device_detail.not_found")
            } else {
                ProgressView()
                    .accessibilityLabel("device_detail.accessibility.loading")
            }
        }
    }
}
