import Foundation
import shared

// bb-ovm1: Relocated from SettingsViewModelWrapper (deleted in the KMP-ObservableViewModel
// migration). Pure display-name mappings for sealed DataMode / enum AiEngineType.
enum SettingsDisplay {
    /// Returns a user-visible display name for a DataMode value.
    static func dataModeDisplayName(_ mode: DataMode) -> String {
        switch mode {
        case is DataModeNone:
            return String(localized: "settings.data_mode.none")
        case is DataModeMock:
            return String(localized: "settings.data_mode.mock")
        case is DataModeGrpcLocal:
            return String(localized: "settings.data_mode.grpc_local")
        case is DataModeGrpcAws:
            return String(localized: "settings.data_mode.grpc_aws")
        case is DataModeGrpcDev:
            return String(localized: "settings.data_mode.grpc_dev")
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
