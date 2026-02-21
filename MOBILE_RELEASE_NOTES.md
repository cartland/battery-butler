# Mobile Release Notes

Release notes for Battery Butler mobile app (Android & iOS). Most recent release first.

Each section covers one release tag range. "What's New" is user-facing language suitable for app store descriptions. "Detailed Changes" links every included PR.

---

## [android/8] — 2026-02-21

From tag `android/3` to tag `android/8` (30 mobile-relevant commits out of 82 total)

### What's New

**New app icon**
- Battery Butler has a brand new custom app icon
- Monochrome icon support for Android 13+ themed icons

**Better empty states**
- Helpful guidance when your device list, types list, or history is empty
- "Add" cards at the bottom of each list for quick access to create items
- Option to preload common battery types (AA, AAA, 9V, etc.) with one tap

**AI improvements**
- Choose between cloud AI (Gemini) and on-device AI (Gemini Nano) in Settings
- On-device AI option renamed from "MediaPipe" to "Gemini Nano" for clarity
- Skip sign-in with Guest Login to try the app immediately

**Visual consistency**
- All list items (Devices, Types, History) now share a consistent card-based design
- Dark theme polish — better colors, smoother navigation transitions
- Improved chat layout with proper keyboard handling

**Offline mode**
- New "Network Mode: None" setting to stay fully offline by default
- Navigate directly to Add Device Type from device creation forms

**Under the hood**
- Migrated build system to modern KMP library plugin across 11 modules
- Improved testable architecture with coroutine DispatcherProvider
- Significantly expanded unit test coverage across all modules

### Detailed Changes

| PR | Description |
|----|-------------|
| [#541](https://github.com/cartland/battery-butler/pull/541) | Add shared ButlerListItemCard and ButlerIconBox components for visual consistency |
| [#529](https://github.com/cartland/battery-butler/pull/529) | Add empty states for Types & History tabs with preload common types |
| [#535](https://github.com/cartland/battery-butler/pull/535) | Replace FAB with AI button, add "Add" cards to lists |
| [#534](https://github.com/cartland/battery-butler/pull/534) | Navigate to Add Device Type directly from device forms |
| [#532](https://github.com/cartland/battery-butler/pull/532) | Add empty state previews and screenshot coverage Gradle task |
| [#530](https://github.com/cartland/battery-butler/pull/530) | Scale down launcher icon foreground to fit adaptive icon safe zone |
| [#527](https://github.com/cartland/battery-butler/pull/527) | Rename on-device AI label from MediaPipe to Gemini Nano |
| [#517](https://github.com/cartland/battery-butler/pull/517) | Maximize unit test coverage and remove server dependency from instrumented tests |
| [#516](https://github.com/cartland/battery-butler/pull/516) | Maximize unit test coverage across all modules |
| [#511](https://github.com/cartland/battery-butler/pull/511) | Implement "Network Mode: None" (Offline Default) |
| [#510](https://github.com/cartland/battery-butler/pull/510) | Add monochrome icon layer for Android 13+ themed icons |
| [#507](https://github.com/cartland/battery-butler/pull/507) | Create custom Battery Butler app icon |
| [#495](https://github.com/cartland/battery-butler/pull/495) | Implement local AI with ML Kit Beta 1 |
| [#500](https://github.com/cartland/battery-butler/pull/500) | Fix dark theme visuals and navigation transitions |
| [#499](https://github.com/cartland/battery-butler/pull/499) | Parameterize screenshot tests for dark theme |
| [#493](https://github.com/cartland/battery-butler/pull/493) | Add AI engine selection and persistence in Settings |
| [#496](https://github.com/cartland/battery-butler/pull/496) | Enable guest login bypass |
| [#497](https://github.com/cartland/battery-butler/pull/497) | Sync screenshot test theme with production colors |
| [#494](https://github.com/cartland/battery-butler/pull/494) | Improve chat layout — window insets and IME handling |
| [#469](https://github.com/cartland/battery-butler/pull/469) | Improve UX with enhanced empty states and loading feedback |
| [#485](https://github.com/cartland/battery-butler/pull/485) | Migrate 11 Gradle modules to com.android.kotlin.multiplatform.library plugin |
| [#476](https://github.com/cartland/battery-butler/pull/476) | Add DispatcherProvider for testable coroutine dispatching |
| [#470](https://github.com/cartland/battery-butler/pull/470) | Move AI vocabulary types to :domain module |
| [#472](https://github.com/cartland/battery-butler/pull/472) | Resolve deprecations in buildSrc |
| [#471](https://github.com/cartland/battery-butler/pull/471) | Remove redundant Elvis operator on non-nullable batteryType |
| [#465](https://github.com/cartland/battery-butler/pull/465) | Deterministic screenshot tests and CI path filter improvements |
| [#473](https://github.com/cartland/battery-butler/pull/473) | Configure test modules for architecture diagrams |

#### Dependencies

| PR | Description |
|----|-------------|
| [#482](https://github.com/cartland/battery-butler/pull/482) | Bump okhttp from 4.12.0 to 5.3.2 |
| [#484](https://github.com/cartland/battery-butler/pull/484) | Bump kotlin-inject from 0.8.0 to 0.9.0 |
| [#487](https://github.com/cartland/battery-butler/pull/487) | Clean up root build.gradle.kts |

---

## [android/3] — 2026-02-15

From tag `android/2` to tag `android/3` (26 mobile-relevant commits out of 135 total)

### What's New

**Sign in with Google**
- Sign in with your Google account on Android, iOS, and Desktop
- Your account info and a sign-out option are now available in Settings
- Sessions stay fresh — expired tokens are handled automatically in the background

**AI Chat**
- Chat with an AI assistant to manage your devices — add devices, create device types, and log battery replacements using natural language

**Sync improvements**
- Real-time sync now automatically reconnects if the connection drops
- Deleted items sync across all your devices instead of reappearing
- Clearer error messages when sync issues occur, with reassurance that your data is safe locally

**Reliability & performance**
- Settings (like network mode) now persist across app restarts
- Fixed a race condition that could cause issues when switching between online and offline modes
- Features unavailable on your platform are now hidden instead of showing non-functional UI

### Detailed Changes

| PR | Description |
|----|-------------|
| [#337](https://github.com/cartland/battery-butler/pull/337) | Add Google Sign-In authentication foundation with login screen, auth state management, and platform stubs |
| [#345](https://github.com/cartland/battery-butler/pull/345) | Implement Android Google Sign-In via Credential Manager API |
| [#356](https://github.com/cartland/battery-butler/pull/356) | Configure GOOGLE_WEB_CLIENT_ID for OAuth across all platforms |
| [#419](https://github.com/cartland/battery-butler/pull/419) | Add server-side auth with Google ID token verification and session tokens |
| [#420](https://github.com/cartland/battery-butler/pull/420) | Add account info and sign-out button to Settings screen |
| [#421](https://github.com/cartland/battery-butler/pull/421) | Add proactive token expiry handling while app is running |
| [#422](https://github.com/cartland/battery-butler/pull/422) | Implement Desktop Google Sign-In with OAuth 2.0 PKCE flow |
| [#427](https://github.com/cartland/battery-butler/pull/427) | Implement iOS Google Sign-In with OAuth PKCE via ASWebAuthenticationSession |
| [#423](https://github.com/cartland/battery-butler/pull/423) | Add AI Chat screen for conversational device management |
| [#418](https://github.com/cartland/battery-butler/pull/418) | Implement server sync foundation — getUpdates streaming, upsert, half-close |
| [#424](https://github.com/cartland/battery-butler/pull/424) | Add subscribe retry with exponential backoff (1s–30s) |
| [#280](https://github.com/cartland/battery-butler/pull/280) | Support remote delete in sync protocol |
| [#379](https://github.com/cartland/battery-butler/pull/379) | Add user-friendly sync error messages |
| [#302](https://github.com/cartland/battery-butler/pull/302) | Add Jetpack DataStore for persistent preferences |
| [#290](https://github.com/cartland/battery-butler/pull/290) | Implement platform-dependent feature flags system |
| [#254](https://github.com/cartland/battery-butler/pull/254) | Add typed error handling with DataResult and DataError |
| [#333](https://github.com/cartland/battery-butler/pull/333) | Add generic Result type with map/flatMap/getOrElse |
| [#383](https://github.com/cartland/battery-butler/pull/383) | Add input validation to domain models |
| [#391](https://github.com/cartland/battery-butler/pull/391) | Fix TOCTOU race condition in icon suggestion |
| [#386](https://github.com/cartland/battery-butler/pull/386) | Fix race condition in database switching during network mode transitions |

#### Dependencies

| PR | Description |
|----|-------------|
| [#263](https://github.com/cartland/battery-butler/pull/263) | Kotlin 2.2.20 → 2.3.0, kotlinx-serialization-json 1.8.0 → 1.10.0 |
| [#264](https://github.com/cartland/battery-butler/pull/264), [#434](https://github.com/cartland/battery-butler/pull/434) | Compose Hot Reload 1.0.0-alpha11 → 1.1.0-alpha05 |
| [#266](https://github.com/cartland/battery-butler/pull/266) | Protobuf 4.26.1 → 4.33.5 |
| [#437](https://github.com/cartland/battery-butler/pull/437) | AWS SDK 2.25.16 → 2.41.24 |
