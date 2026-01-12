import SwiftUI
import shared

@main
struct iOSApp: App {
    let component: NativeComponent
    
    init() {
        self.component = IosNativeHelper().createComponent()
    }
    
    var body: some Scene {
        WindowGroup {
            MainScreen(component: component)
        }
    }
}