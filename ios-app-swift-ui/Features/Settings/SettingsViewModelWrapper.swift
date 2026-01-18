import SwiftUI
import shared
import Combine

class SettingsViewModelWrapper: ObservableObject {
    @Published var exportData: String? = nil
    
    private let viewModel: SettingsViewModel
    private let viewModelStore = KmpViewModelStore()
    private var task: Task<Void, Never>?
    
    init(_ viewModel: SettingsViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        self.task = Task { @MainActor [weak self] in
            for await data in viewModel.exportData {
                self?.exportData = data
            }
        }
    }
    
    deinit {
        task?.cancel()
        viewModelStore.clear()
    }
    
    func onExportData() {
        viewModel.onExportData()
    }
    
    func onExportDataConsumed() {
        viewModel.onExportDataConsumed()
    }
}
