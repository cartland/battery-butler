import SwiftUI
import Foundation
import shared

struct SettingsScreen: View {
    @StateObject private var wrapper: SettingsViewModelWrapper
    @State private var isShareSheetPresented = false

    init(viewModel: SettingsViewModel) {
        _wrapper = StateObject(wrappedValue: SettingsViewModelWrapper(viewModel))
    }

    var body: some View {
        SettingsContentView(
            exportData: wrapper.exportData,
            isShareSheetPresented: $isShareSheetPresented,
            onExportData: { wrapper.onExportData() },
            onExportDataConsumed: { wrapper.onExportDataConsumed() },
            appVersion: wrapper.appVersion
        )
        .onChange(of: wrapper.exportData) { _, newData in
            if newData != nil {
                isShareSheetPresented = true
            }
        }
    }
}

struct SettingsContentView: View {
    let exportData: String?
    @Binding var isShareSheetPresented: Bool
    let onExportData: () -> Void
    let onExportDataConsumed: () -> Void
    let appVersion: String

    var body: some View {
        Form {
            Section(header: Text("Data Management")) {
                Button("Export Data") {
                    onExportData()
                }
            }

            Section {
                Text(appVersion)
                    .foregroundStyle(Color.butlerOnSurfaceVariant)
            }
        }
        .navigationTitle("Settings")
        .sheet(isPresented: $isShareSheetPresented, onDismiss: {
            onExportDataConsumed()
        }) {
            if let data = exportData {
                if let fileUrl = saveToTempFile(content: data) {
                    ShareSheet(activityItems: [fileUrl])
                } else {
                    ShareSheet(activityItems: [data])
                }
            }
        }
    }

    private func saveToTempFile(content: String) -> URL? {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy_MM_dd_HH_mm_ss"
        let timestamp = formatter.string(from: Date())
        let filename = "Battery_Butler_Backup_\(timestamp).json"
        let tempDir = FileManager.default.temporaryDirectory
        let fileUrl = tempDir.appendingPathComponent(filename)

        do {
            try content.write(to: fileUrl, atomically: true, encoding: .utf8)
            return fileUrl
        } catch {
            print("Failed to save file: \(error)")
            return nil
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
