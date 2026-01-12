import SwiftUI
import shared

@main
struct BatteryButlerApp: App {
    // Initialize the KMP Component
    // IosNativeHelper is in shared module
    let component: NativeComponent
    
    init() {
        self.component = IosNativeHelper().createComponent()
    }
    
    var body: some Scene {
        WindowGroup {
            // Inject the Component into MainScreen
            MainScreen(component: component)
        }
    }
}
