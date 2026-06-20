import Foundation
import shared

// bb-ovm1: Relocated from SettingsViewModelWrapper (deleted in the KMP-ObservableViewModel
// migration). Pure display-name mappings for sealed NetworkMode / enum AiEngineType.
enum SettingsDisplay {
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
        }
    }
}
