import SwiftUI
import shared
import Combine

class SettingsViewModelWrapper: ObservableObject {
    @Published var exportData: String? = nil
    @Published var appVersion: String = "Version"

    private let viewModel: SettingsViewModel
    private let viewModelStore = KmpViewModelStore()
    private var exportTask: Task<Void, Never>?
    private var versionTask: Task<Void, Never>?

    init(_ viewModel: SettingsViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)

        self.exportTask = Task { @MainActor [weak self] in
            for await data in viewModel.exportData {
                self?.exportData = data
            }
        }

        self.versionTask = Task { @MainActor [weak self] in
            for await version in viewModel.appVersion {
                if let iosVersion = version as? AppVersion.Ios {
                    self?.appVersion = "\(iosVersion.versionName)-\(iosVersion.buildNumber)"
                } else {
                    self?.appVersion = "Version"
                }
            }
        }
    }

    deinit {
        exportTask?.cancel()
        versionTask?.cancel()
        viewModelStore.clear()
    }

    func onExportData() {
        viewModel.onExportData()
    }

    func onExportDataConsumed() {
        viewModel.onExportDataConsumed()
    }
}
