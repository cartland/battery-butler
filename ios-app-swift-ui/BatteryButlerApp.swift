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
            // Inject the HomeViewModel into the ContentView
            ContentView(viewModel: component.homeViewModel)
        }
    }
}
