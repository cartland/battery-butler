import SwiftUI
import shared

struct ContentView: View {
    // We wrap the KMP HomeUiState in our custom wrapper
    @StateObject var viewModelWrapper: ViewModelWrapper<HomeUiState>
    
    // We hold the raw KMP ViewModel to call methods
    private let viewModel: HomeViewModel
    
    init(viewModel: HomeViewModel) {
        self.viewModel = viewModel
        // Initialize the wrapper with initial state and the Flow
        // uiState is a StateFlow<HomeUiState>
        _viewModelWrapper = StateObject(wrappedValue: ViewModelWrapper(
            viewModel.uiState.value as! HomeUiState, // Access initial value
            viewModel.uiState // The flow
        ))
    }
    
    var body: some View {
        NavigationView {
            List {
                // Access state via the wrapper
                let state = viewModelWrapper.state
                
                Section(header: Text("Devices")) {
                    if state.devices.isEmpty {
                        Text("No devices found")
                            .foregroundColor(.secondary)
                    } else {
                        ForEach(state.devices, id: \.id) { device in
                            DeviceRow(device: device)
                        }
                    }
                }
            }
            .navigationTitle("Blanket Native")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        // Action to add device
                        // For demo, we might need to expose an 'add' method in HomeViewModel
                    }) {
                        Image(systemName: "plus")
                    }
                }
            }
        }
    }
}
