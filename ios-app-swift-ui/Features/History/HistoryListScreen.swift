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
                        Text(item.event.date.description) // Format nicely later
                            .font(.caption)
                        Text("Replaced Battery")
                            .font(.headline)
                    }
                }
            } else if wrapper.state is HistoryListUiStateLoading {
                ProgressView()
            }
        }
        .navigationTitle("History")
    }
}
