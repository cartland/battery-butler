import SwiftUI
import ComposeApp

struct ContentView: View {
    // Basic example of consuming a shared VM
    // Ideally this would be observed, but for a simple "Hello World" verification
    // we just want to prove we can import ComposeApp (shared) and compile.
    
    var body: some View {
        VStack {
            Image(systemName: "swift")
                .font(.system(size: 80))
                .foregroundStyle(.orange)
            
            Text("Battery Butler (SwiftUI)")
                .font(.largeTitle)
                .padding()
            
            Text("Powered by KMP Shared ViewModel")
                .foregroundStyle(.secondary)
            
            // This proves access to shared code (compiles if module is linked)
            Text("Module: Shared Code Linked")
                .padding(.top)
                .padding(.top)
        }
    }
}
