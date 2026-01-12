import SwiftUI
import shared
import Combine

class SettingsViewModelWrapper: ObservableObject {
    @Published var exportData: String? = nil
    
    private let viewModel: SettingsViewModel
    private var collector: FlowCollector<String?>?
    
    init(_ viewModel: SettingsViewModel) {
        self.viewModel = viewModel
        
        let col = FlowCollector<String?> { [weak self] data in
            DispatchQueue.main.async {
                self?.exportData = data
            }
        }
        self.collector = col
        viewModel.exportData.collect(collector: col) { _ in }
    }
    
    func onExportData() {
        viewModel.onExportData()
    }
    
    func onExportDataConsumed() {
        viewModel.onExportDataConsumed()
    }
}
