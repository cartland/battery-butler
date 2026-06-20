import SwiftUI
import shared
import KMPObservableViewModelSwiftUI

struct EventDetailScreen: View {
    // bb-ovm1: @StateViewModel replaces EventDetailViewModelWrapper.
    @StateViewModel private var viewModel: EventDetailViewModel
    @Environment(\.presentationMode) private var presentationMode
    private let eventId: String
    private let component: NativeComponent
    @State private var showingEditEvent = false

    init(eventId: String, component: NativeComponent) {
        self.eventId = eventId
        self.component = component
        _viewModel = StateViewModel(
            wrappedValue: component.eventDetailViewModelFactory.create(eventId: eventId)
        )
    }

    var body: some View {
        EventDetailContentView(state: viewModel.uiStateValue, component: component)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("common.edit") {
                        showingEditEvent = true
                    }
                }
            }
            .sheet(isPresented: $showingEditEvent) {
                EditBatteryEventScreen(eventId: eventId, component: component)
            }
    }
}

// bb-ovm1: Option A manual state accessor (no NativeCoroutines).
extension EventDetailViewModel {
    var uiStateValue: EventDetailScreenState { uiState.value }
}

struct EventDetailContentView: View {
    let state: EventDetailScreenState?
    var component: NativeComponent?

    var body: some View {
        Group {
            if let success = state as? EventDetailScreenStateSuccess {
                let event = success.event
                Form {
                    Section(header: Text("event_detail.section.details")) {
                        HStack {
                            Text("event_detail.field.date")
                                .foregroundStyle(Color.butlerOnSurfaceVariant)
                            Spacer()
                            let date = Date(timeIntervalSince1970: TimeInterval(event.date.toEpochMilliseconds()) / 1000.0)
                            Text(date, style: .date)
                        }

                        if let batteryType = event.batteryType {
                            HStack {
                                Text("event_detail.field.battery_type")
                                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                                Spacer()
                                Text(batteryType)
                            }
                        }

                        if let notes = event.notes {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("event_detail.field.notes")
                                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                                Text(notes)
                            }
                        }
                    }

                    if let device = success.device {
                        Section(header: Text("event_detail.section.device")) {
                            if let component = component {
                                NavigationLink {
                                    DeviceDetailScreen(component: component, deviceId: device.id)
                                } label: {
                                    HStack {
                                        Text("event_detail.field.name")
                                            .foregroundStyle(Color.butlerOnSurfaceVariant)
                                        Spacer()
                                        Text(device.name)
                                    }
                                }
                            } else {
                                HStack {
                                    Text("event_detail.field.name")
                                        .foregroundStyle(Color.butlerOnSurfaceVariant)
                                    Spacer()
                                    Text(device.name)
                                }
                            }

                            HStack {
                                Text("event_detail.field.location")
                                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                                Spacer()
                                Text(device.location ?? String(localized: "common.unknown"))
                            }

                            if let deviceType = success.deviceType {
                                HStack {
                                    Text("event_detail.field.type")
                                        .foregroundStyle(Color.butlerOnSurfaceVariant)
                                    Spacer()
                                    Text(deviceType.name)
                                }
                            }
                        }
                    }
                }
            } else if state is EventDetailScreenStateNotFound {
                VStack {
                    Text("event_detail.not_found")
                        .font(.headline)
                        .foregroundStyle(Color.butlerOnSurfaceVariant)
                }
            } else {
                ProgressView("common.loading")
            }
        }
        .navigationTitle("event_detail.title")
        .navigationBarTitleDisplayMode(.inline)
    }
}
