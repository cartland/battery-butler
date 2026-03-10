import SwiftUI
import shared

struct EventDetailScreen: View {
    @StateObject private var wrapper: EventDetailViewModelWrapper
    @Environment(\.presentationMode) private var presentationMode
    private let eventId: String
    private let component: NativeComponent
    @State private var showingEditEvent = false

    init(eventId: String, component: NativeComponent) {
        self.eventId = eventId
        self.component = component
        _wrapper = StateObject(wrappedValue: EventDetailViewModelWrapper(eventId: eventId, component: component))
    }

    var body: some View {
        EventDetailContentView(state: wrapper.state)
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

struct EventDetailContentView: View {
    let state: EventDetailUiState?

    var body: some View {
        Group {
            if let success = state as? EventDetailUiStateSuccess {
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
                            HStack {
                                Text("event_detail.field.name")
                                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                                Spacer()
                                Text(device.name)
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
            } else if state is EventDetailUiStateNotFound {
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
