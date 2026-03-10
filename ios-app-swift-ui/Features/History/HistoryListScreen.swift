import SwiftUI
import shared

struct HistoryListScreen: View {
    @StateObject var wrapper: HistoryListViewModelWrapper

    init(viewModel: HistoryListViewModel) {
        _wrapper = StateObject(wrappedValue: HistoryListViewModelWrapper(viewModel))
    }

    var body: some View {
        HistoryListContentView(state: wrapper.state)
    }
}

struct HistoryListContentView: View {
    let state: HistoryListUiState

    private static let monthFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "MMM"
        return f
    }()

    private static let dayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "d"
        return f
    }()

    var body: some View {
        List {
            if let success = state as? HistoryListUiStateSuccess {
                ForEach(success.items, id: \.self) { item in
                    let eventDate = Date(
                        timeIntervalSince1970: TimeInterval(item.event.date.toEpochMilliseconds()) / 1000.0
                    )
                    let daysAgo = Calendar.current.dateComponents([.day], from: eventDate, to: Date()).day

                    HStack(spacing: ButlerSpacing.standard) {
                        // Calendar badge
                        VStack(spacing: 2) {
                            Text(Self.monthFormatter.string(from: eventDate).uppercased())
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundStyle(Color.butlerOnSurfaceVariant)
                            Text(Self.dayFormatter.string(from: eventDate))
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
            } else if state is HistoryListUiStateLoading {
                ProgressView()
                    .accessibilityLabel("history.accessibility.loading")
            }
        }
        .navigationTitle("history.title")
    }
}
