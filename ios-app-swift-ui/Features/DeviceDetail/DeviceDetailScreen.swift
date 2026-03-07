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
        .navigationTitle("Device Details")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Edit") {
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
                            Text("Replaced Battery")
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.butlerPrimary)
                                .foregroundStyle(.white)
                                .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.medium))
                        }
                        .accessibilityHint("Records a battery replacement for today")

                        Divider()

                        // History Section
                        Text("Battery History")
                            .font(.title2)
                            .bold()

                        if success.events.isEmpty {
                            Text("No history recorded")
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
                                        Text("Details")
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
                Text("Device not found")
            } else {
                ProgressView()
                    .accessibilityLabel("Loading device details")
            }
        }
    }
}
