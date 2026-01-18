import Foundation
import SwiftUI
import shared
import Combine

/// A Generic Wrapper that observes a KMP StateFlow and publishes changes to SwiftUI
class ViewModelWrapper<State>: ObservableObject {
    @Published var state: State
    
    private let viewModelStore = ViewModelStore()
    
    init(_ viewModel: ViewModel, _ initialState: State, _ flow: Kotlinx_coroutines_coreFlow) {
        self.state = initialState
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        // Subscribe to the flow
        let collector = FlowCollector<State> { [weak self] newState in
            DispatchQueue.main.async {
                self?.state = newState
            }
        }
        
        // Launch collection
        flow.collect(collector: collector) { error in
            if let error = error {
                print("Error collecting flow: \(error)")
            }
        }
    }
    
    deinit {
        viewModelStore.clear()
    }
}
