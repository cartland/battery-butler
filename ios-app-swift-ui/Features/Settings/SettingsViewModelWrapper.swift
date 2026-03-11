import SwiftUI
import shared
import Combine

class SettingsViewModelWrapper: ObservableObject {
    @Published var exportData: String? = nil
    @Published var appVersion: String = "Version"
    @Published var networkMode: NetworkMode = NetworkModeNone()
    @Published var currentUser: User? = nil
    @Published var isSignedIn: Bool = false
    @Published var aiEngineType: AiEngineType = .cloud

    /// The available network modes from the KMP ViewModel.
    var availableNetworkModes: [NetworkMode] {
        viewModel.availableNetworkModes
    }

    /// The available AI engine types from the KMP ViewModel.
    var availableAiEngines: [AiEngineType] {
        viewModel.availableAiEngines
    }

    private let viewModel: SettingsViewModel
    private let viewModelStore = KmpViewModelStore()
    private var exportTask: Task<Void, Never>?
    private var versionTask: Task<Void, Never>?
    private var networkModeTask: Task<Void, Never>?
    private var userTask: Task<Void, Never>?
    private var signedInTask: Task<Void, Never>?
    private var aiEngineTask: Task<Void, Never>?

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
                if let iosVersion = version as? AppVersionIos {
                    self?.appVersion = "\(iosVersion.versionName)-\(iosVersion.buildNumber)"
                } else {
                    self?.appVersion = "Version"
                }
            }
        }

        self.networkModeTask = Task { @MainActor [weak self] in
            for await mode in viewModel.networkMode {
                self?.networkMode = mode as! NetworkMode
            }
        }

        self.userTask = Task { @MainActor [weak self] in
            for await user in viewModel.currentUser {
                self?.currentUser = user as? User
            }
        }

        self.signedInTask = Task { @MainActor [weak self] in
            for await signedIn in viewModel.isSignedIn {
                self?.isSignedIn = signedIn as! Bool
            }
        }

        self.aiEngineTask = Task { @MainActor [weak self] in
            for await engine in viewModel.aiEngineType {
                self?.aiEngineType = engine as! AiEngineType
            }
        }
    }

    deinit {
        exportTask?.cancel()
        versionTask?.cancel()
        networkModeTask?.cancel()
        userTask?.cancel()
        signedInTask?.cancel()
        aiEngineTask?.cancel()
        viewModelStore.clear()
    }

    func onExportData() {
        viewModel.onExportData()
    }

    func onExportDataConsumed() {
        viewModel.onExportDataConsumed()
    }

    func onNetworkModeSelected(_ mode: NetworkMode) {
        viewModel.onNetworkModeSelected(mode: mode)
    }

    func onAiEngineSelected(_ type: AiEngineType) {
        viewModel.onAiEngineSelected(type: type)
    }

    func signOut() {
        viewModel.signOut()
    }

    /// Returns a user-visible display name for a NetworkMode value.
    static func networkModeDisplayName(_ mode: NetworkMode) -> String {
        switch mode {
        case is NetworkModeNone:
            return String(localized: "settings.network_mode.none")
        case is NetworkModeMock:
            return String(localized: "settings.network_mode.mock")
        case is NetworkModeGrpcLocal:
            return String(localized: "settings.network_mode.grpc_local")
        case is NetworkModeGrpcAws:
            return String(localized: "settings.network_mode.grpc_aws")
        case is NetworkModeGrpcDev:
            return String(localized: "settings.network_mode.grpc_dev")
        default:
            return String(localized: "common.unknown")
        }
    }

    /// Returns a user-visible display name for an AiEngineType value.
    static func aiEngineDisplayName(_ type: AiEngineType) -> String {
        switch type {
        case .cloud:
            return String(localized: "settings.ai_engine.cloud")
        case .onDevice:
            return String(localized: "settings.ai_engine.on_device")
        case .noOp:
            return String(localized: "settings.ai_engine.noop")
        default:
            return String(localized: "common.unknown")
        }
    }
}
