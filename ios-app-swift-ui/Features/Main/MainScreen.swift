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
                    .navigationTitle(MainTab.devices.rawValue)
            }
            .tabItem {
                Label(MainTab.devices.rawValue, systemImage: MainTab.devices.iconName)
            }
            .tag(MainTab.devices)
            
            // Types Tab
            NavigationStack {
                Text("Types Coming Soon")
                    .navigationTitle(MainTab.types.rawValue)
            }
            .tabItem {
                Label(MainTab.types.rawValue, systemImage: MainTab.types.iconName)
            }
            .tag(MainTab.types)
            
            // History Tab
            NavigationStack {
                HistoryListScreen(viewModel: component.historyListViewModel)
                    .navigationTitle(MainTab.history.rawValue)
            }
            .tabItem {
                Label(MainTab.history.rawValue, systemImage: MainTab.history.iconName)
            }
            .tag(MainTab.history)
        }
    }
}
