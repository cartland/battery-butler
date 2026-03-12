import SwiftUI
import ExperimentalShared

@main
struct ExperimentalIOSApp: App {
    let component: ExperimentalAppComponent

    init() {
        self.component = IosExperimentalHelper().createComponent()
    }

    var body: some Scene {
        WindowGroup {
            ContentView(component: component)
        }
    }
}
