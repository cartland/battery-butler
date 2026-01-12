import SwiftUI
import shared

struct SettingsScreen: View {
    @StateObject private var wrapper: SettingsViewModelWrapper
    @Environment(\.presentationMode) var presentationMode
    @State private var isShareSheetPresented = false
    
    init(viewModel: SettingsViewModel) {
        _wrapper = StateObject(wrappedValue: SettingsViewModelWrapper(viewModel))
    }
    
    var body: some View {
        Form {
            Section(header: Text("Data Management")) {
                Button("Export Data") {
                    wrapper.onExportData()
                }
            }
            
            Section {
                Text("Version 1.0.0")
                    .foregroundColor(.secondary)
            }
        }
        .navigationTitle("Settings")
        .onChange(of: wrapper.exportData) { newData in
            if newData != nil {
                isShareSheetPresented = true
            }
        }
        .sheet(isPresented: $isShareSheetPresented, onDismiss: {
            wrapper.onExportDataConsumed()
        }) {
            if let data = wrapper.exportData {
                ShareSheet(activityItems: [data])
            }
        }
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]
    let applicationActivities: [UIActivity]? = nil

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(
            activityItems: activityItems,
            applicationActivities: applicationActivities
        )
        return controller
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
