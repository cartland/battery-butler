# Battery Butler

[![CI](https://github.com/cartland/battery-butler/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/cartland/battery-butler/actions/workflows/ci.yml?query=branch%3Amain)
[![Release](https://github.com/cartland/battery-butler/actions/workflows/release-android.yml/badge.svg)](https://github.com/cartland/battery-butler/actions/workflows/release-android.yml)
<!-- Server badge (uncomment when AWS infra is re-enabled):
[![Server Deploy](https://github.com/cartland/battery-butler/actions/workflows/server-build.yml/badge.svg?branch=main)](https://github.com/cartland/battery-butler/actions/workflows/server-build.yml?query=branch%3Amain)
-->
> **Server**: AWS infrastructure is hibernated. Server workflows are disabled. See [`server/README.md`](server/README.md) for the re-enable checklist.

**Battery Butler** is a Kotlin Multiplatform (KMP) application designed to help users track battery usage and replacements for their household devices. It leverages modern Android and KMP technologies including Compose Multiplatform, Room, and on-device AI integration.

## 🚀 Features

*   **Device Management**: Add, edit, and delete devices with custom attributes (name, location).
*   **Battery History**: Track battery replacement events for each device.
*   **Device Types**: Manage reusable device types (e.g., "TV Remote" uses 2x AAA batteries).
*   **AI Integration**:
    *   **Magic AI Button**: Automatically suggests device types and icons based on descriptions.
    *   **Batch Import**: Parse natural language notes (e.g., "Replaced smoke detector battery yesterday") to batch create events.
*   **Cross-Platform**: Runs on Android, iOS, and Desktop (JVM).

## 🏗 Architecture

The project follows **Clean Architecture** principles adapted for Kotlin Multiplatform.

For a detailed deep-dive into the module structure, dependency graph, and strict layer rules, please read the **[Architecture Documentation](docs/Architecture.md)**.

### Module Structure

<!-- GENERATED:BEGIN full_system_structure.mmd -->
```mermaid
%% GENERATED FILE - DO NOT EDIT
graph TD
    subgraph "iOS Apps"
        IosSwiftDi[":ios-swift-di"]
        IosAppComposeUi.xcodeproj["ios-app-compose-ui.xcodeproj"]
        IosAppSwiftUi.xcodeproj["ios-app-swift-ui.xcodeproj"]
    end

    subgraph "Compose Apps"
        ComposeApp[":compose-app"]
    end

    subgraph "Server"
        ServerApp[":server:app"]
        ServerData[":server:data"]
        ServerDomain[":server:domain"]
    end

    subgraph "Presentation"
        ComposeResources[":compose-resources"]
        PresentationCore[":presentation-core"]
        PresentationFeature[":presentation-feature"]
        PresentationModel[":presentation-model"]
        Viewmodel[":viewmodel"]
    end

    subgraph "Domain Layer"
        Ai[":ai"]
        Domain[":domain"]
        Usecase[":usecase"]
    end

    subgraph "Data Layer"
        Data[":data"]
        DataLocal[":data-local"]
        DataNetwork[":data-network"]
    end

    subgraph "Screenshot Tests"
        AndroidScreenshotTests[":android-screenshot-tests"]
    end

    subgraph "Test Infrastructure"
        E2eTests[":e2e-tests"]
        Fixtures[":fixtures"]
        TestCommon[":test-common"]
    end

    %% Dependencies
    Ai --> Domain
    Ai --> PresentationModel

    AndroidScreenshotTests --> ComposeResources
    AndroidScreenshotTests --> Domain
    AndroidScreenshotTests --> PresentationCore
    AndroidScreenshotTests --> PresentationFeature
    AndroidScreenshotTests --> PresentationModel

    ComposeApp --> Ai
    ComposeApp --> ComposeResources
    ComposeApp --> Data
    ComposeApp --> PresentationCore
    ComposeApp --> PresentationFeature
    ComposeApp --> Usecase
    ComposeApp --> Viewmodel

    Data --> DataLocal
    Data --> DataNetwork
    Data --> Domain
    Data --> TestCommon

    DataLocal --> Domain

    DataNetwork --> Domain
    DataNetwork --> Fixtures

    Fixtures --> Domain

    IosSwiftDi --> Ai
    IosSwiftDi --> Data
    IosSwiftDi --> PresentationModel
    IosSwiftDi --> Usecase
    IosSwiftDi --> Viewmodel

    PresentationCore --> ComposeResources
    PresentationCore --> Domain
    PresentationCore --> PresentationModel

    PresentationFeature --> ComposeResources
    PresentationFeature --> PresentationCore
    PresentationFeature --> PresentationModel

    PresentationModel --> Domain

    ServerApp --> Domain
    ServerApp --> ServerData
    ServerApp --> ServerDomain

    ServerData --> Domain
    ServerData --> Fixtures
    ServerData --> ServerDomain

    ServerDomain --> Domain

    TestCommon --> DataLocal
    TestCommon --> DataNetwork
    TestCommon --> Domain

    Usecase --> Domain
    Usecase --> PresentationModel
    Usecase --> TestCommon

    Viewmodel --> Domain
    Viewmodel --> PresentationModel
    Viewmodel --> TestCommon
    Viewmodel --> Usecase

    IosAppComposeUi.xcodeproj --> ComposeApp

    IosAppSwiftUi.xcodeproj --> IosSwiftDi

```
<!-- GENERATED:END full_system_structure.mmd -->

### Code Distribution

The project spans **Kotlin**, **Swift**, and **Java** across multiple modules.
See [Code Analysis](docs/CODE_ANALYSIS.md) for the full breakdown.

<!-- GENERATED:BEGIN code_distribution.mmd -->
```mermaid
---
config:
  sankey:
    showValues: true
    width: 800
    height: 1000
    nodeAlignment: justify
    linkColor: gradient
---
%% GENERATED FILE - DO NOT EDIT
sankey-beta

Codebase,Shared Code,25095
Codebase,iOS Swift App,4402
Codebase,Other,3290
Codebase,CMP Apps,2172
Codebase,Server,1528
Codebase,Screenshot Tests,1129

Shared Code,presentation-feature,5987
Shared Code,viewmodel,4627
Shared Code,usecase,4170
Shared Code,data-network,2429
Shared Code,domain,2333
Shared Code,presentation-core,2095
Shared Code,data,1832
Shared Code,data-local,1059
Shared Code,ai,493
Shared Code,compose-resources,70

iOS Swift App,iosAppSwiftUI,4402

Other,buildSrc,1759
Other,test-common,620
Other,e2e-tests,271
Other,presentation-model,218
Other,fixtures,209
Other,ios-swift-di,182
Other,iosAppComposeUI,31

CMP Apps,compose-app,2172

Server,server:app,1342
Server,server:data,152
Server,server:domain,34

Screenshot Tests,android-screenshot-tests,1129

presentation-feature,Kotlin,5987
viewmodel,Kotlin,4627
iosAppSwiftUI,Swift,4402
usecase,Kotlin,4170
data-network,Kotlin,2429
domain,Kotlin,2333
compose-app,Kotlin,2172
presentation-core,Kotlin,2095
data,Kotlin,1832
buildSrc,Kotlin,1759
server:app,Kotlin,1342
android-screenshot-tests,Kotlin,1129
data-local,Kotlin,1059
test-common,Kotlin,620
ai,Kotlin,493
e2e-tests,Kotlin,271
presentation-model,Kotlin,218
fixtures,Kotlin,209
ios-swift-di,Kotlin,182
server:data,Kotlin,152
compose-resources,Kotlin,70
server:domain,Kotlin,34
iosAppComposeUI,Swift,31
```
<!-- GENERATED:END code_distribution.mmd -->

### Tech Stack

*   **Language**: Kotlin 2.0+
*   **UI**: [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) (Android, Desktop), SwiftUI (iOS).
*   **Dependency Injection**: [Kotlin Inject](https://github.com/evant/kotlin-inject) (Koin was previously used but migrated).
*   **Persistence**: [Room for KMP](https://developer.android.com/kotlin/multiplatform/room).
*   **Concurrency**: Kotlin Coroutines & Flow.
*   **AI**: Google AI Client SDK (Gemini) / ML Kit.
*   **Networking**: Ktor & Wire (gRPC).
*   **Date/Time**: `kotlinx-datetime`.
*   **Build Systems**: Gradle (Kotlin/Android), Bazel (Proto generation).

## 📋 Prerequisites

Before building the project, ensure you have the following installed:

| Tool | Version | Notes |
| :--- | :--- | :--- |
| **JDK** | 21+ | Required for Kotlin compilation |
| **Android Studio** | Latest stable | For Android development |
| **Xcode** | 15+ | For iOS development (macOS only) |
| **Bazel** | Latest | For proto generation (`brew install bazelisk`) |
| **Gradle** | 8.12+ | Wrapper included, no manual install needed |

## 🛠 Building and Running
This project uses Gradle for build and test orchestration.

### Common Commands
*   **Format Code**: `./scripts/format.sh` (runs `spotlessApply`)
*   **Run Tests**: `./gradlew test` (Unit) or `./gradlew pixel5api34Check` (Android Instrumented)
*   **Debug Flow**: `./scripts/run-e2e-debug-flow.sh` (Starts Server, App, and monitors logs)
*   **Update Diagram**: `./gradlew generateMermaidGraph`

### Android
*   Open the project in **Android Studio**.
*   Select the `composeApp` configuration.
*   Run on an Emulator or connected Device.

### Desktop (JVM)
*   Run the Gradle task: `./gradlew :compose-app:run`

### iOS
*   **Prerequisite**: Install Bazel for proto generation: `brew install bazelisk`
*   Open `ios-app-swift-ui/iosAppSwiftUI.xcodeproj` in **Xcode**.
*   Ensure you have built the KMP framework at least once (`./gradlew :compose-app:embedAndSignAppleFrameworkForXcode`).
*   Run on an iPhone Simulator or Device.

### Server (gRPC)
*   Run locally: `./gradlew :server:app:run` (listens on port `50051`)
*   Deploy to AWS: Push to main auto-deploys to dev. See `server/README.md` for multi-environment deployment (dev/staging/prod).

## 🔑 AI Configuration (Optional)

To enable the AI features (Gemini), you need an API Key.
1.  Obtain an API Key from [Google AI Studio](https://aistudio.google.com/).
2.  Add it to your `local.properties` file:
    ```properties
    GEMINI_API_KEY=your_api_key_here
    ```

### Server URL (Optional)

Local builds use the `PRODUCTION_SERVER_URL` value from `gradle.properties` by default. CI builds override this via the `PRODUCTION_SERVER_URL` GitHub secret (auto-synced from terraform after each deploy). To override locally:

```properties
# In gradle.properties (or pass via -P flag)
PRODUCTION_SERVER_URL=http://your-server-url:port
```

## 🔐 Google Sign-In Configuration (Optional)

Google Sign-In is disabled by default. To enable:

### 1. Google Cloud Console Setup
1. Go to [APIs & Services > Credentials](https://console.cloud.google.com/apis/credentials)
2. Create OAuth 2.0 Client ID (Web application type)
3. Copy the Client ID

### 2. Local Development
Add to `local.properties`:
```properties
GOOGLE_WEB_CLIENT_ID=123456789-abc.apps.googleusercontent.com
```

### 3. CI/CD (GitHub Actions)
Add repository secret:
- Settings > Secrets and variables > Actions > New repository secret
- Name: `GOOGLE_WEB_CLIENT_ID`
- Value: Your Web Client ID

### 4. Android: Register SHA-1 Fingerprints
```bash
# Debug fingerprint
./gradlew signingReport
```
Add the SHA-1 to your OAuth client in Google Cloud Console under "Android" application type.

## 🤝 Contributing

This project uses `Spotless` for code formatting.
Run `./scripts/format.sh` before committing to ensure your code follows the style guidelines.
Use `./scripts/prepare-for-commit.sh` to validate your changes before pushing.

For AI agents (Claude Code, etc.), see the **[Agent Guidelines](.agent/AGENTS.md)** for workflow requirements.
