import SwiftUI
import shared

struct HistoryListScreen: View {
    @StateObject var wrapper: HistoryListViewModelWrapper
    
    init(viewModel: HistoryListViewModel) {
        _wrapper = StateObject(wrappedValue: HistoryListViewModelWrapper(viewModel))
    }
    
    var body: some View {
        List {
            if let success = wrapper.state as? HistoryListUiStateSuccess {
                ForEach(success.items, id: \.self) { item in
                    VStack(alignment: .leading) {
                        Text(item.event.date.description)
                            .font(.caption)
                        Text("Replaced Battery")
                            .font(.headline)
                    }
                    .accessibilityElement(children: .combine)
                    .accessibilityLabel("Battery replaced on \(item.event.date.description)")
                }
            } else if wrapper.state is HistoryListUiStateLoading {
                ProgressView()
                    .accessibilityLabel("Loading history")
            }
        }
        .navigationTitle("History")
    }
}
