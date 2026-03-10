import SwiftUI
import shared

struct HistoryListScreen: View {
    @StateObject var wrapper: HistoryListViewModelWrapper
    private let component: NativeComponent
    @State private var isAddEventPresented = false

    init(component: NativeComponent) {
        self.component = component
        _wrapper = StateObject(wrappedValue: HistoryListViewModelWrapper(component.historyListViewModel))
    }

    var body: some View {
        HistoryListContentView(
            state: wrapper.state,
            onAddEventTapped: { isAddEventPresented = true },
            onEventTapped: { _, _ in },
            eventDestination: { eventId in
                EventDetailScreen(eventId: eventId, component: component)
            }
        )
        .sheet(isPresented: $isAddEventPresented) {
            AddBatteryEventScreen(viewModel: component.addBatteryEventViewModel)
        }
    }
}

private let historyMonthFormatter: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "MMM"
    return f
}()

private let historyDayFormatter: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "d"
    return f
}()

struct HistoryListContentView<EventDestination: View>: View {
    let state: HistoryListUiState
    let onAddEventTapped: () -> Void
    let onEventTapped: (String, String) -> Void
    let eventDestination: (String) -> EventDestination

    var body: some View {
        Group {
            if let success = state as? HistoryListUiStateSuccess {
                if success.items.isEmpty {
                    VStack(spacing: ButlerSpacing.standard) {
                        Image(systemName: "clock.arrow.circlepath")
                            .font(.system(size: 48))
                            .foregroundStyle(Color.butlerOnSurfaceVariant)
                        Text("history.empty.title")
                            .font(.headline)
                            .foregroundStyle(Color.butlerOnSurface)
                        Text("history.empty.message")
                            .font(.subheadline)
                            .foregroundStyle(Color.butlerOnSurfaceVariant)
                            .multilineTextAlignment(.center)
                    }
                    .padding(ButlerSpacing.large)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        // Add battery event card
                        Button(action: onAddEventTapped) {
                            HStack(spacing: ButlerSpacing.standard) {
                                ButlerIconBox(systemName: "plus")
                                Text("history.add_event")
                                    .font(.headline)
                                    .fontWeight(.semibold)
                                    .foregroundStyle(Color.butlerOnPrimaryContainer)
                            }
                        }
                        .listRowBackground(Color.butlerPrimaryContainer)
                        .accessibilityLabel("history.add_event")

                        ForEach(success.items, id: \.self) { item in
                            let eventDate = Date(
                                timeIntervalSince1970: TimeInterval(item.event.date.toEpochMilliseconds()) / 1000.0
                            )
                            let daysAgo = Calendar.current.dateComponents([.day], from: eventDate, to: Date()).day

                            NavigationLink(destination: eventDestination(item.event.id)) {
                                HStack(spacing: ButlerSpacing.standard) {
                                    // Calendar badge
                                    VStack(spacing: 2) {
                                        Text(historyMonthFormatter.string(from: eventDate).uppercased())
                                            .font(.caption)
                                            .fontWeight(.bold)
                                            .foregroundStyle(Color.butlerOnSurfaceVariant)
                                        Text(historyDayFormatter.string(from: eventDate))
                                            .font(.headline)
                                            .fontWeight(.bold)
                                            .foregroundStyle(Color.butlerOnSurface)
                                    }
                                    .frame(width: 50, height: 50)
                                    .background(Color.butlerSurfaceVariant, in: RoundedRectangle(cornerRadius: 10))

                                    // Center info
                                    VStack(alignment: .leading, spacing: ButlerSpacing.extraSmall) {
                                        Text(item.deviceName)
                                            .font(.headline)
                                            .fontWeight(.semibold)
                                            .lineLimit(1)

                                        let subtitle = [item.deviceTypeName, item.deviceLocation]
                                            .compactMap { $0?.isEmpty == true ? nil : $0 }
                                            .joined(separator: " \u{2022} ")
                                        if !subtitle.isEmpty {
                                            Text(subtitle)
                                                .font(.subheadline)
                                                .foregroundStyle(Color.butlerOnSurfaceVariant)
                                                .lineLimit(1)
                                        }
                                    }

                                    Spacer()

                                    // Trailing days ago
                                    VStack(spacing: ButlerSpacing.extraSmall) {
                                        Image(systemName: "battery.100")
                                            .font(.system(size: ButlerIconSize.small))
                                            .foregroundStyle(Color.butlerOnSurfaceVariant)
                                            .accessibilityHidden(true)
                                        if let days = daysAgo {
                                            Text("\(days)d ago")
                                                .font(.caption)
                                                .foregroundStyle(Color.butlerOnSurfaceVariant)
                                        }
                                    }
                                    .frame(width: 60)
                                }
                                .accessibilityElement(children: .combine)
                                .accessibilityLabel("Battery replaced on \(eventDate.formatted(date: .abbreviated, time: .omitted)) for \(item.deviceName)")
                            }
                        }
                    }
                }
            } else if state is HistoryListUiStateLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .accessibilityLabel("history.accessibility.loading")
            } else if let error = state as? HistoryListUiStateError {
                VStack(spacing: ButlerSpacing.standard) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 48))
                        .foregroundStyle(Color.butlerError)
                    Text("history.error.title")
                        .font(.headline)
                        .foregroundStyle(Color.butlerOnSurface)
                    Text(error.message)
                        .font(.subheadline)
                        .foregroundStyle(Color.butlerOnSurfaceVariant)
                        .multilineTextAlignment(.center)
                }
                .padding(ButlerSpacing.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationTitle("history.title")
    }
}
