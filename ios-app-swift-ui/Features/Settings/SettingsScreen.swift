import SwiftUI
import Foundation
import UniformTypeIdentifiers
import shared
import KMPObservableViewModelSwiftUI

struct SettingsScreen: View {
    // bb-ovm1: @StateViewModel replaces SettingsViewModelWrapper.
    @StateViewModel private var viewModel: SettingsViewModel
    @State private var isShareSheetPresented = false
    @State private var isFileImporterPresented = false
    @State private var importSnackbarMessage: String? = nil

    init(viewModel: SettingsViewModel) {
        _viewModel = StateViewModel(wrappedValue: viewModel)
    }

    var body: some View {
        SettingsContentView(
            // Account
            currentUser: viewModel.currentUserValue,
            onSignOut: { viewModel.signOut() },
            // Data mode
            dataMode: viewModel.dataModeValue,
            availableDataModes: viewModel.availableDataModes,
            onDataModeSelected: { mode in viewModel.onDataModeSelected(mode: mode) },
            // AI engine
            aiEngineType: viewModel.aiEngineTypeValue,
            availableAiEngines: viewModel.availableAiEngines,
            onAiEngineSelected: { type in viewModel.onAiEngineSelected(type: type) },
            // Export
            exportData: viewModel.exportDataValue,
            isShareSheetPresented: $isShareSheetPresented,
            onExportData: { viewModel.onExportData() },
            onExportDataConsumed: { viewModel.onExportDataConsumed() },
            // Import
            onImportData: { isFileImporterPresented = true },
            importInProgress: viewModel.importInProgressValue,
            // Version
            appVersion: viewModel.appVersionDisplay
        )
        .onChange(of: viewModel.exportDataValue) { _, newData in
            if newData != nil {
                isShareSheetPresented = true
            }
        }
        .onChange(of: viewModel.importResultValue) { _, result in
            if let result = result {
                importSnackbarMessage = "Imported \(result.devicesImported) devices, \(result.deviceTypesImported) types, \(result.eventsImported) events"
                viewModel.onImportResultConsumed()
            }
        }
        .onChange(of: viewModel.importErrorValue) { _, error in
            if let error = error {
                importSnackbarMessage = "Import failed: \(error)"
                viewModel.onImportResultConsumed()
            }
        }
        .fileImporter(
            isPresented: $isFileImporterPresented,
            allowedContentTypes: [UTType.json],
            allowsMultipleSelection: false
        ) { result in
            switch result {
            case .success(let urls):
                guard let url = urls.first else { return }
                guard url.startAccessingSecurityScopedResource() else { return }
                defer { url.stopAccessingSecurityScopedResource() }
                if let data = try? Data(contentsOf: url),
                   let jsonString = String(data: data, encoding: .utf8) {
                    viewModel.onImportData(jsonString: jsonString)
                }
            case .failure:
                break
            }
        }
        .overlay(alignment: .bottom) {
            if let message = importSnackbarMessage {
                Text(message)
                    .padding()
                    .background(Color(.systemGray5))
                    .cornerRadius(8)
                    .padding(.bottom, 20)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                            importSnackbarMessage = nil
                        }
                    }
            }
        }
    }
}

// bb-ovm1: Option A manual state accessors (no NativeCoroutines).
extension SettingsViewModel {
    var currentUserValue: User? { currentUser.value }
    var dataModeValue: DataMode { dataMode.value }
    var aiEngineTypeValue: AiEngineType { aiEngineType.value }
    var exportDataValue: String? { exportData.value }
    var importResultValue: ImportResult? { importResult.value }
    var importErrorValue: String? { importError.value }
    var importInProgressValue: Bool { (importInProgress.value as? Bool) ?? false }
    var appVersionDisplay: String {
        if let ios = appVersion.value as? AppVersionIos {
            return "\(ios.versionName)-\(ios.buildNumber)"
        }
        return "Version"
    }
}

struct SettingsContentView: View {
    // Account
    let currentUser: User?
    let onSignOut: () -> Void
    // Data mode
    let dataMode: DataMode
    let availableDataModes: [DataMode]
    let onDataModeSelected: (DataMode) -> Void
    // AI engine
    let aiEngineType: AiEngineType
    let availableAiEngines: [AiEngineType]
    let onAiEngineSelected: (AiEngineType) -> Void
    // Export
    let exportData: String?
    @Binding var isShareSheetPresented: Bool
    let onExportData: () -> Void
    let onExportDataConsumed: () -> Void
    // Import
    let onImportData: () -> Void
    let importInProgress: Bool
    // Version
    let appVersion: String

    @State private var isDataModeExpanded = false
    @State private var isAiEngineExpanded = false

    var body: some View {
        Form {
            // Account section
            if let user = currentUser {
                Section {
                    HStack(spacing: ButlerSpacing.medium) {
                        Image(systemName: "person.circle.fill")
                            .font(.title2)
                            .foregroundStyle(Color.butlerPrimary)
                        VStack(alignment: .leading, spacing: ButlerSpacing.extraSmall) {
                            Text(user.displayName ?? String(localized: "settings.account.signed_in"))
                                .font(.headline)
                                .foregroundStyle(Color.butlerOnSurface)
                            if let email = user.email {
                                Text(email)
                                    .font(.subheadline)
                                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                            }
                        }
                    }
                    .padding(.vertical, ButlerSpacing.extraSmall)

                    Button(role: .destructive, action: onSignOut) {
                        HStack {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                            Text("settings.account.sign_out")
                        }
                    }
                } header: {
                    Text("settings.section.account")
                }
            }

            // Data Mode section
            Section {
                DisclosureGroup(isExpanded: $isDataModeExpanded) {
                    ForEach(Array(availableDataModes.enumerated()), id: \.offset) { _, mode in
                        Button {
                            onDataModeSelected(mode)
                            isDataModeExpanded = false
                        } label: {
                            HStack {
                                Text(SettingsDisplay.dataModeDisplayName(mode))
                                    .foregroundStyle(Color.butlerOnSurface)
                                Spacer()
                                if dataModesEqual(dataMode, mode) {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(Color.butlerPrimary)
                                }
                            }
                        }
                    }
                } label: {
                    HStack(spacing: ButlerSpacing.medium) {
                        Image(systemName: "wifi")
                            .foregroundStyle(Color.butlerPrimary)
                        VStack(alignment: .leading, spacing: ButlerSpacing.extraSmall) {
                            Text("settings.data_mode.title")
                                .foregroundStyle(Color.butlerOnSurface)
                            Text(SettingsDisplay.dataModeDisplayName(dataMode))
                                .font(.caption)
                                .foregroundStyle(Color.butlerOnSurfaceVariant)
                        }
                    }
                }
            } header: {
                Text("settings.section.data")
            }

            // AI Engine section
            Section {
                DisclosureGroup(isExpanded: $isAiEngineExpanded) {
                    ForEach(Array(availableAiEngines.enumerated()), id: \.offset) { _, engine in
                        Button {
                            onAiEngineSelected(engine)
                            isAiEngineExpanded = false
                        } label: {
                            HStack {
                                Text(SettingsDisplay.aiEngineDisplayName(engine))
                                    .foregroundStyle(Color.butlerOnSurface)
                                Spacer()
                                if aiEngineType == engine {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(Color.butlerPrimary)
                                }
                            }
                        }
                    }
                } label: {
                    HStack(spacing: ButlerSpacing.medium) {
                        Image(systemName: "wand.and.stars")
                            .foregroundStyle(Color.butlerPrimary)
                        VStack(alignment: .leading, spacing: ButlerSpacing.extraSmall) {
                            Text("settings.ai_engine.title")
                                .foregroundStyle(Color.butlerOnSurface)
                            Text(SettingsDisplay.aiEngineDisplayName(aiEngineType))
                                .font(.caption)
                                .foregroundStyle(Color.butlerOnSurfaceVariant)
                        }
                    }
                }
            } header: {
                Text("settings.section.ai")
            }

            // Data Management section
            Section {
                Button("settings.button.export") {
                    onExportData()
                }
                Button {
                    onImportData()
                } label: {
                    HStack(spacing: ButlerSpacing.medium) {
                        Image(systemName: "square.and.arrow.down")
                            .foregroundStyle(Color.butlerPrimary)
                        Text("settings.button.import")
                            .foregroundStyle(Color.butlerOnSurface)
                    }
                }
                .disabled(importInProgress)
            } header: {
                Text("settings.section.data")
            }

            // Check for Updates section
            Section {
                Link(destination: URL(string: "https://apps.apple.com/app/battery-butler/id0000000000")!) {
                    HStack(spacing: ButlerSpacing.medium) {
                        Image(systemName: "arrow.up.circle")
                            .foregroundStyle(Color.butlerPrimary)
                        VStack(alignment: .leading, spacing: ButlerSpacing.extraSmall) {
                            Text("settings.button.check_updates")
                                .foregroundStyle(Color.butlerOnSurface)
                            Text("settings.check_updates.subtitle")
                                .font(.caption)
                                .foregroundStyle(Color.butlerOnSurfaceVariant)
                        }
                    }
                }
            } header: {
                Text("settings.section.updates")
            }

            // App Version section
            Section {
                HStack(spacing: ButlerSpacing.medium) {
                    Image(systemName: "info.circle")
                        .foregroundStyle(Color.butlerPrimary)
                    VStack(alignment: .leading, spacing: ButlerSpacing.extraSmall) {
                        Text("settings.version.title")
                            .foregroundStyle(Color.butlerOnSurface)
                        Text(appVersion)
                            .font(.caption)
                            .foregroundStyle(Color.butlerOnSurfaceVariant)
                    }
                }
            } header: {
                Text("settings.section.about")
            }
        }
        .navigationTitle("settings.title")
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

    /// Compares two DataMode instances for equality by type and URL.
    /// DataMode is a sealed interface in Kotlin, so we compare by concrete type.
    private func dataModesEqual(_ a: DataMode, _ b: DataMode) -> Bool {
        switch (a, b) {
        case (is DataModeNone, is DataModeNone):
            return true
        case (is DataModeMock, is DataModeMock):
            return true
        case (let aLocal as DataModeGrpcLocal, let bLocal as DataModeGrpcLocal):
            return aLocal.url == bLocal.url
        case (let aAws as DataModeGrpcAws, let bAws as DataModeGrpcAws):
            return aAws.url == bAws.url
        case (let aDev as DataModeGrpcDev, let bDev as DataModeGrpcDev):
            return aDev.url == bDev.url
        default:
            return false
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
