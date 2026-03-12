import SwiftUI
import shared

struct MainScreen: View {
    let component: NativeComponent
    
    @State private var selectedTab: MainTab = .devices
    
    var body: some View {
        TabView(selection: $selectedTab) {
            // Devices Tab
            NavigationStack {
                HomeScreen(component: component)
            }
            .tabItem {
                Label(MainTab.devices.titleKey, systemImage: MainTab.devices.iconName)
            }
            .tag(MainTab.devices)

            // Types Tab
            NavigationStack {
                DeviceTypeListScreen(component: component)
            }
            .tabItem {
                Label(MainTab.types.titleKey, systemImage: MainTab.types.iconName)
            }
            .tag(MainTab.types)

            // History Tab
            NavigationStack {
                HistoryListScreen(component: component)
            }
            .tabItem {
                Label(MainTab.history.titleKey, systemImage: MainTab.history.iconName)
            }
            .tag(MainTab.history)
        }
        .tint(Color.butlerPrimary)
    }
}
