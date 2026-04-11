import SwiftUI
import Foundation
import UniformTypeIdentifiers
import shared

struct SettingsScreen: View {
    @StateObject private var wrapper: SettingsViewModelWrapper
    @State private var isShareSheetPresented = false
    @State private var isFileImporterPresented = false
    @State private var importSnackbarMessage: String? = nil

    init(viewModel: SettingsViewModel) {
        _wrapper = StateObject(wrappedValue: SettingsViewModelWrapper(viewModel))
    }

    var body: some View {
        SettingsContentView(
            // Account
            currentUser: wrapper.currentUser,
            onSignOut: { wrapper.signOut() },
            // Network mode
            networkMode: wrapper.networkMode,
            availableNetworkModes: wrapper.availableNetworkModes,
            onNetworkModeSelected: { mode in wrapper.onNetworkModeSelected(mode) },
            // AI engine
            aiEngineType: wrapper.aiEngineType,
            availableAiEngines: wrapper.availableAiEngines,
            onAiEngineSelected: { type in wrapper.onAiEngineSelected(type) },
            // Export
            exportData: wrapper.exportData,
            isShareSheetPresented: $isShareSheetPresented,
            onExportData: { wrapper.onExportData() },
            onExportDataConsumed: { wrapper.onExportDataConsumed() },
            // Import
            onImportData: { isFileImporterPresented = true },
            importInProgress: wrapper.importInProgress,
            // Version
            appVersion: wrapper.appVersion
        )
        .onChange(of: wrapper.exportData) { _, newData in
            if newData != nil {
                isShareSheetPresented = true
            }
        }
        .onChange(of: wrapper.importResult) { _, result in
            if let result = result {
                importSnackbarMessage = "Imported \(result.devicesImported) devices, \(result.deviceTypesImported) types, \(result.eventsImported) events"
                wrapper.onImportResultConsumed()
            }
        }
        .onChange(of: wrapper.importError) { _, error in
            if let error = error {
                importSnackbarMessage = "Import failed: \(error)"
                wrapper.onImportResultConsumed()
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
                    wrapper.onImportData(jsonString)
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

struct SettingsContentView: View {
    // Account
    let currentUser: User?
    let onSignOut: () -> Void
    // Network mode
    let networkMode: NetworkMode
    let availableNetworkModes: [NetworkMode]
    let onNetworkModeSelected: (NetworkMode) -> Void
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

    @State private var isNetworkModeExpanded = false
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

            // Network Mode section
            Section {
                DisclosureGroup(isExpanded: $isNetworkModeExpanded) {
                    ForEach(Array(availableNetworkModes.enumerated()), id: \.offset) { _, mode in
                        Button {
                            onNetworkModeSelected(mode)
                            isNetworkModeExpanded = false
                        } label: {
                            HStack {
                                Text(SettingsViewModelWrapper.networkModeDisplayName(mode))
                                    .foregroundStyle(Color.butlerOnSurface)
                                Spacer()
                                if networkModesEqual(networkMode, mode) {
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
                            Text("settings.network_mode.title")
                                .foregroundStyle(Color.butlerOnSurface)
                            Text(SettingsViewModelWrapper.networkModeDisplayName(networkMode))
                                .font(.caption)
                                .foregroundStyle(Color.butlerOnSurfaceVariant)
                        }
                    }
                }
            } header: {
                Text("settings.section.network")
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
                                Text(SettingsViewModelWrapper.aiEngineDisplayName(engine))
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
                            Text(SettingsViewModelWrapper.aiEngineDisplayName(aiEngineType))
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

    /// Compares two NetworkMode instances for equality by type and URL.
    /// NetworkMode is a sealed interface in Kotlin, so we compare by concrete type.
    private func networkModesEqual(_ a: NetworkMode, _ b: NetworkMode) -> Bool {
        switch (a, b) {
        case (is NetworkModeNone, is NetworkModeNone):
            return true
        case (is NetworkModeMock, is NetworkModeMock):
            return true
        case (let aLocal as NetworkModeGrpcLocal, let bLocal as NetworkModeGrpcLocal):
            return aLocal.url == bLocal.url
        case (let aAws as NetworkModeGrpcAws, let bAws as NetworkModeGrpcAws):
            return aAws.url == bAws.url
        case (let aDev as NetworkModeGrpcDev, let bDev as NetworkModeGrpcDev):
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
