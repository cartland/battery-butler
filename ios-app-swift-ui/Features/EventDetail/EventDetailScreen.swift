import SwiftUI
import shared

struct EventDetailScreen: View {
    @StateObject private var wrapper: EventDetailViewModelWrapper
    @Environment(\.presentationMode) private var presentationMode

    init(eventId: String, component: NativeComponent) {
        _wrapper = StateObject(wrappedValue: EventDetailViewModelWrapper(eventId: eventId, component: component))
    }

    var body: some View {
        EventDetailContentView(state: wrapper.state)
    }
}

struct EventDetailContentView: View {
    let state: EventDetailUiState?

    var body: some View {
        Group {
            if let success = state as? EventDetailUiStateSuccess {
                let event = success.event
                Form {
                    Section(header: Text("Event Details")) {
                        HStack {
                            Text("Date")
                                .foregroundStyle(Color.butlerOnSurfaceVariant)
                            Spacer()
                            let date = Date(timeIntervalSince1970: TimeInterval(event.date.toEpochMilliseconds()) / 1000.0)
                            Text(date, style: .date)
                        }

                        if let batteryType = event.batteryType {
                            HStack {
                                Text("Battery Type")
                                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                                Spacer()
                                Text(batteryType)
                            }
                        }

                        if let notes = event.notes {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Notes")
                                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                                Text(notes)
                            }
                        }
                    }

                    if let device = success.device {
                        Section(header: Text("Device Information")) {
                            HStack {
                                Text("Name")
                                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                                Spacer()
                                Text(device.name)
                            }

                            HStack {
                                Text("Location")
                                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                                Spacer()
                                Text(device.location ?? "Unknown")
                            }

                            if let deviceType = success.deviceType {
                                HStack {
                                    Text("Type")
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
                    Text("Event Not Found")
                        .font(.headline)
                        .foregroundStyle(Color.butlerOnSurfaceVariant)
                }
            } else {
                ProgressView("Loading...")
            }
        }
        .navigationTitle("Event Detail")
        .navigationBarTitleDisplayMode(.inline)
    }
}
