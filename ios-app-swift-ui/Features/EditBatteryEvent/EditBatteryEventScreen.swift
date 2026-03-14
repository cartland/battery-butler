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
    let state: EditBatteryEventScreenState
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
                if let successState = state as? EditBatteryEventScreenStateSuccess {
                    Form {
                        // Device context section (read-only)
                        if let device = successState.device {
                            Section(header: Text("edit_event.section.device")) {
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
                        Section(header: Text("edit_event.section.details")) {
                            DatePicker("edit_event.field.date", selection: $eventDate, displayedComponents: .date)

                            TextField("edit_event.field.battery_type", text: $batteryType)

                            TextField("edit_event.field.notes", text: $notes)
                        }

                        Section {
                            Button("edit_event.button.save", action: onSave)
                        }

                        Section {
                            Button("edit_event.button.delete", role: .destructive) {
                                showDeleteConfirmation = true
                            }
                        }
                    }
                    .alert("edit_event.alert.delete_title", isPresented: $showDeleteConfirmation) {
                        Button("common.delete", role: .destructive, action: onDelete)
                        Button("common.cancel", role: .cancel) {}
                    } message: {
                        Text("common.action_cannot_be_undone")
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
                } else if state is EditBatteryEventScreenStateLoading {
                    ProgressView("edit_event.loading")
                } else {
                    Text("edit_event.not_found")
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("edit_event.title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("common.cancel", action: onCancel)
                }
            }
        }
    }
}
