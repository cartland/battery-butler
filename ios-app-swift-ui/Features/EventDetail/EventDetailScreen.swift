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
                                .foregroundColor(.secondary)
                            Spacer()
                            let date = Date(timeIntervalSince1970: TimeInterval(event.date.toEpochMilliseconds()) / 1000.0)
                            Text(date, style: .date)
                        }

                        if let batteryType = event.batteryType {
                            HStack {
                                Text("Battery Type")
                                    .foregroundColor(.secondary)
                                Spacer()
                                Text(batteryType)
                            }
                        }

                        if let notes = event.notes {
                            VStack(alignment: .leading, spacing: Spacing.extraSmall) {
                                Text("Notes")
                                    .foregroundColor(.secondary)
                                Text(notes)
                            }
                        }
                    }

                    if let device = success.device {
                        Section(header: Text("Device Information")) {
                            HStack {
                                Text("Name")
                                    .foregroundColor(.secondary)
                                Spacer()
                                Text(device.name)
                            }

                            HStack {
                                Text("Location")
                                    .foregroundColor(.secondary)
                                Spacer()
                                Text(device.location ?? "Unknown")
                            }

                            if let deviceType = success.deviceType {
                                HStack {
                                    Text("Type")
                                        .foregroundColor(.secondary)
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
                        .foregroundColor(.secondary)
                }
            } else {
                ProgressView("Loading...")
            }
        }
        .navigationTitle("Event Detail")
        .navigationBarTitleDisplayMode(.inline)
    }
}
