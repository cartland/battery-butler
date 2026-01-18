import SwiftUI
import shared
import Combine

class SettingsViewModelWrapper: ObservableObject {
    @Published var exportData: String? = nil
    
    private let viewModel: SettingsViewModel
    private let viewModelStore = ViewModelStore()
    private var collector: FlowCollector<String?>?
    
    init(_ viewModel: SettingsViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        let col = FlowCollector<String?> { [weak self] data in
            DispatchQueue.main.async {
                self?.exportData = data
            }
        }
        self.collector = col
        viewModel.exportData.collect(collector: col) { _ in }
    }
    
    deinit {
        viewModelStore.clear()
    }
    
    func onExportData() {
        viewModel.onExportData()
    }
    
    func onExportDataConsumed() {
        viewModel.onExportDataConsumed()
    }
}
