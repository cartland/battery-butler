import SwiftUI
import shared

@main
struct iOSApp: App {
    let component: NativeComponent
    @State private var isAuthenticated = false
    
    init() {
        self.component = IosNativeHelper().createComponent()
    }
    
    var body: some Scene {
        WindowGroup {
            if isAuthenticated {
                MainScreen(component: component)
            } else {
                LoginScreen(viewModel: component.loginViewModel, onLoginSuccess: {
                    isAuthenticated = true
                }, onSkipLogin: {
                    isAuthenticated = true
                })
            }
        }
    }
}