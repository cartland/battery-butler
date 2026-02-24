import SwiftUI
import shared

struct EventDetailScreen: View {
    @StateObject private var wrapper: EventDetailViewModelWrapper
    @Environment(\.presentationMode) private var presentationMode
    
    @State private var showingDeleteConfirmation = false
    @State private var editedDate: Date? = nil
    
    init(eventId: String, component: NativeComponent) {
        _wrapper = StateObject(wrappedValue: EventDetailViewModelWrapper(eventId: eventId, component: component))
    }
    
    var body: some View {
        Group {
            if let success = wrapper.state as? EventDetailUiStateSuccess {
                let event = success.event
                Form {
                    Section(header: Text("Event Details")) {
                        HStack {
                            Text("ID")
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(event.id)
                        }
                        
                        DatePicker(
                            "Date & Time",
                            selection: Binding(
                                get: { editedDate ?? Date(timeIntervalSince1970: TimeInterval(event.date.toEpochMilliseconds()) / 1000.0) },
                                set: { newDate in
                                    editedDate = newDate
                                    wrapper.updateDate(newDate: newDate)
                                }
                            )
                        )
                        
                        if let batteryType = event.batteryType {
                            HStack {
                                Text("Battery Type")
                                    .foregroundColor(.secondary)
                                Spacer()
                                Text(batteryType)
                            }
                        }
                        
                        if let notes = event.notes {
                            VStack(alignment: .leading, spacing: 4) {
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
            } else if wrapper.state is EventDetailUiStateNotFound {
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
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(role: .destructive, action: {
                    showingDeleteConfirmation = true
                }) {
                    Image(systemName: "trash")
                        .foregroundColor(.red)
                }
                .accessibilityLabel("Delete event")
            }
        }
        .alert("Delete Event?", isPresented: $showingDeleteConfirmation) {
            Button("Cancel", role: .cancel) { }
            Button("Delete", role: .destructive) {
                wrapper.deleteEvent()
                presentationMode.wrappedValue.dismiss()
            }
        }
    }
}
