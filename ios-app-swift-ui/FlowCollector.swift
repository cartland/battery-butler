import Foundation
import shared // Import the KMP Framework

// specialized FlowCollector for bridging KMP Flow to Swift closure
class FlowCollector<T>: Kotlinx_coroutines_coreFlowCollector {
    let callback: (T) -> Void

    init(callback: @escaping (T) -> Void) {
        self.callback = callback
    }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        if let v = value as? T {
            callback(v)
        }
        completionHandler(nil)
    }
}
