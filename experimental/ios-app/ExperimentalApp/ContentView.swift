import SwiftUI
import ExperimentalShared

enum ExperimentalRoute: Hashable {
    case counter
}

struct ContentView: View {
    let component: ExperimentalAppComponent
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            List {
                Section("Experimental Features") {
                    Button(action: { path.append(ExperimentalRoute.counter) }) {
                        VStack(alignment: .leading) {
                            Text("Counter")
                                .font(.headline)
                            Text("Observe and get a counter value")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Experimental")
            .navigationDestination(for: ExperimentalRoute.self) { route in
                switch route {
                case .counter:
                    CounterScreen(viewModel: component.counterViewModel)
                }
            }
        }
    }
}
