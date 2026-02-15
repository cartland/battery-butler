# Mobile Release Notes

Release notes for Battery Butler mobile app (Android & iOS). Most recent release first.

Each section covers one release tag range. "What's New" is user-facing language suitable for app store descriptions. "Detailed Changes" links every included PR.

---

## [Unreleased] — 2026-02-15

Range: `android/2` (fc2dede) .. `7cff3a9` (26 mobile-relevant commits out of 135 total)

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
