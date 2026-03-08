import SwiftUI
import shared

struct EditBatteryEventScreen: View {
    @StateObject private var wrapper: EditBatteryEventViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    @State private var eventDate: Date = Date()
    @State private var batteryType: String = ""
    @State private var notes: String = ""
    @State private var hasInitializedFields = false

    init(eventId: String, component: NativeComponent) {
        _wrapper = StateObject(wrappedValue: EditBatteryEventViewModelWrapper(eventId: eventId, component: component))
    }

    var body: some View {
        EditBatteryEventContentView(
            state: wrapper.state,
            eventDate: $eventDate,
            batteryType: $batteryType,
            notes: $notes,
            hasInitializedFields: $hasInitializedFields,
            onSave: {
                wrapper.updateEvent(
                    date: eventDate,
                    batteryType: batteryType.isEmpty ? nil : batteryType,
                    notes: notes.isEmpty ? nil : notes
                )
                dismiss()
            },
            onDelete: {
                wrapper.deleteEvent()
                dismiss()
            },
            onCancel: {
                dismiss()
            }
        )
    }
}

struct EditBatteryEventContentView: View {
    let state: EditBatteryEventUiState
    @Binding var eventDate: Date
    @Binding var batteryType: String
    @Binding var notes: String
    @Binding var hasInitializedFields: Bool

    let onSave: () -> Void
    let onDelete: () -> Void
    let onCancel: () -> Void

    @State private var showDeleteConfirmation = false

    var body: some View {
        NavigationStack {
            Group {
                if let successState = state as? EditBatteryEventUiStateSuccess {
                    Form {
                        // Device context section (read-only)
                        if let device = successState.device {
                            Section(header: Text("Device")) {
                                HStack(spacing: ButlerSpacing.standard) {
                                    let sfSymbol = SFSymbolMapper.sfSymbolName(for: successState.deviceType?.defaultIcon)
                                    ButlerIconBox(systemName: sfSymbol)
                                        .accessibilityHidden(true)
                                    VStack(alignment: .leading) {
                                        Text(device.name)
                                            .font(.headline)
                                        if let deviceType = successState.deviceType {
                                            Text(deviceType.name)
                                                .font(.subheadline)
                                                .foregroundStyle(Color.butlerOnSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        // Event details section
                        Section(header: Text("Event Details")) {
                            DatePicker("Date", selection: $eventDate, displayedComponents: .date)

                            TextField("Battery Type (Optional)", text: $batteryType)

                            TextField("Notes (Optional)", text: $notes)
                        }

                        Section {
                            Button("Save Changes", action: onSave)
                        }

                        Section {
                            Button("Delete Event", role: .destructive) {
                                showDeleteConfirmation = true
                            }
                        }
                    }
                    .alert("Delete Event?", isPresented: $showDeleteConfirmation) {
                        Button("Delete", role: .destructive, action: onDelete)
                        Button("Cancel", role: .cancel) {}
                    } message: {
                        Text("This action cannot be undone.")
                    }
                    .onAppear {
                        if !hasInitializedFields {
                            let event = successState.event
                            eventDate = Date(timeIntervalSince1970: TimeInterval(event.date.toEpochMilliseconds()) / 1000.0)
                            batteryType = event.batteryType ?? ""
                            notes = event.notes ?? ""
                            hasInitializedFields = true
                        }
                    }
                } else if state is EditBatteryEventUiStateLoading {
                    ProgressView("Loading event...")
                } else {
                    Text("Event not found")
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("Edit Event")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onCancel)
                }
            }
        }
    }
}
