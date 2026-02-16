# Changelog

This changelog summarizes the history of changes to the Battery Butler repository, explaining what changed and why.

## How to Maintain This File

> **For Human and AI Authors:** This changelog is manually maintained, not auto-generated. Please update it when making significant changes.

### When to Update

- After merging PRs that add features, fix bugs, or make architectural changes
- When the changelog is missing context about why changes were made
- When grouping related changes that span multiple PRs

### Format Guidelines

- **Most recent changes at the top**
- Group changes by date or release
- Include PR links: `[#123](https://github.com/user/repo/pull/123)`
- Explain the *why*, not just the *what*
- Use categories: Features, Fixes, CI/CD, Refactoring, Documentation, Performance

### Example Entry

```markdown
## 2026-02-01

### Features
- **Device sync**: Added cloud sync for devices ([#100](link)) - Enables users to access their device inventory across multiple devices.
```

---

## 2026-02-15

### Fixes

- **P0: Release builds using stale server URL**: `release-android.yml` was missing the `ORG_GRADLE_PROJECT_PRODUCTION_SERVER_URL` env var, causing every release build to use the `gradle.properties` fallback instead of the GitHub secret. Added the env var to match `ci.yml`.

- **Hardcoded NLB hostnames**: Eliminated literal NLB hostnames from `SettingsViewModel` and `RemoteConnectivityTest`. Introduced `ProductionServerUrl` value class in the domain module for type-safe DI injection, provided by `AppComponent` via `BuildConfig`.

### CI/CD Improvements

- **Auto-sync terraform output to GitHub secrets**: Deploy workflows now capture the NLB DNS name after `terraform apply` and sync it to GitHub secrets (`PRODUCTION_SERVER_URL` for prod, `DEV_SERVER_URL` for dev). Uses `BOT_PAT` with `continue-on-error` so failed syncs don't fail deploys.

- **Server connectivity validation before release**: Added a health check step to `release-android.yml` that verifies the production server is reachable before uploading to Play Store. Catches stale URLs before they reach users.

### Documentation

- **Server URL management docs**: Added comprehensive documentation to `.agent/project.md` explaining how the server URL flows from terraform through GitHub secrets to BuildConfig. Updated `AGENTS.md` configuration rules, `server/README.md` secrets table, and module READMEs.

---

## 2026-02-07

### Features

- **Multi-environment server deployment**: Implemented build-once, deploy-many pipeline with dev (auto), staging (manual), and prod (manual + approval gate) environments ([#405](https://github.com/cartland/battery-butler/pull/405), [#407](https://github.com/cartland/battery-butler/pull/407)) - Same Docker image SHA is promoted through environments unchanged.

- **Server destroy workflow**: Added `server-destroy.yml` to tear down staging or dev infrastructure on demand, with production explicitly blocked ([#411](https://github.com/cartland/battery-butler/pull/411)) - Needed for managing AWS free-tier RDS instance limits.

### CI/CD Improvements

- **CI path filtering for non-code files**: Excluded `server/*.json` and `server/*.md` from the CI code filter so beads-only and docs-only PRs skip expensive builds ([#413](https://github.com/cartland/battery-butler/pull/413)) - Reduces CI time from ~15min to ~30s for documentation changes.

- **Terraform state lock prevention**: Removed ECR from Terraform management (now a `data` source), added concurrency groups to all deploy workflows, and removed the error-prone import step ([#407](https://github.com/cartland/battery-butler/pull/407)) - Eliminates the root cause of repeated DynamoDB state lock issues.

- **Free-tier compatible infrastructure**: Downgraded staging and prod Terraform configs to `db.t3.micro` for AWS free-tier compatibility ([#409](https://github.com/cartland/battery-butler/pull/409), [#410](https://github.com/cartland/battery-butler/pull/410)).

### Fixes

- **Comprehensive IAM permissions**: Audited all Terraform resources and added missing IAM permissions for tag management, inline policies, and ECS operations ([#408](https://github.com/cartland/battery-butler/pull/408)) - Previous deploys failed iteratively due to missing permissions.

- **Terraform import hanging**: Fixed `terraform import` hanging by passing `-var-file` to prevent interactive prompts ([#402](https://github.com/cartland/battery-butler/pull/402)).

### Documentation

- **Updated CLAUDE.md**: Added server deployment documentation, CI path filtering notes, and updated session resume points ([#413](https://github.com/cartland/battery-butler/pull/413)).

- **Agent priority P0.5**: Added priority level for instruction/beads PRs that should be fast-tracked ([#404](https://github.com/cartland/battery-butler/pull/404)).

---

## 2026-02-01

### CI/CD Improvements

- **Auto-PR cleanup strategy**: Workflows now automatically close stale auto-generated PRs before creating new ones ([#180](https://github.com/cartland/battery-butler/pull/180)) - Prevents accumulation of outdated screenshot/diagram PRs that can have merge conflicts.

- **Strict screenshot validation**: The validation script now fails (not just warns) when screenshots are broken 1x1 pixel images ([#177](https://github.com/cartland/battery-butler/pull/177)) - Previously broken screenshots could slip through CI undetected.

- **OIDC authentication for AWS**: Server deployments now support OIDC authentication with fallback to access keys ([#176](https://github.com/cartland/battery-butler/pull/176)) - More secure than long-lived access keys, follows AWS best practices.

- **Scheduled diagram updates**: Added daily cron job to keep architecture diagrams fresh ([#173](https://github.com/cartland/battery-butler/pull/173)) - Prevents large diagram diffs from accumulating over time.

### Fixes

- **Fixed screenshot test stability**: Added `nowInstant` parameter to history-related composables so screenshot tests use fixed dates ([#179](https://github.com/cartland/battery-butler/pull/179)) - Screenshots previously showed "X days ago" that changed daily, causing CI failures.

- **Fixed string resources in previews**: Changed imports to use project's `composeStringResource()` wrapper that uses `LocalAppStrings` ([#174](https://github.com/cartland/battery-butler/pull/174)) - Fixes 6 broken screenshots that were rendering as 1x1 pixels because string resources couldn't resolve in screenshot test context.

### Refactoring

- **Runtime API key injection**: Moved GEMINI_API_KEY from compile-time BuildConfig to runtime `AiConfig` interface ([#175](https://github.com/cartland/battery-butler/pull/175)) - Better separation of concerns, allows different configurations per environment.

---

## 2026-01-31

### CI/CD Improvements

- **Screenshot baseline cleanup**: Workflows now delete old baselines before regenerating ([#168](https://github.com/cartland/battery-butler/pull/168)) - Prevents orphaned screenshots when tests are renamed or deleted.

- **iOS build caching**: Added Xcode DerivedData caching ([#167](https://github.com/cartland/battery-butler/pull/167)) - Significantly speeds up iOS CI builds.

- **Ubuntu for update workflows**: Switched diagram/screenshot update workflows from macOS to Ubuntu ([#166](https://github.com/cartland/battery-butler/pull/166)) - Faster and cheaper CI runs.

- **Build timeouts**: Added `timeout-minutes` to all CI jobs ([#164](https://github.com/cartland/battery-butler/pull/164)) - Prevents hung builds from consuming CI minutes indefinitely.

### Documentation

- **CI architecture docs**: Added comprehensive documentation of CI/CD architecture and improvement plan ([#163](https://github.com/cartland/battery-butler/pull/163))

- **PR merge workflow**: Documented rules for PR merge priorities and task tracking ([#160](https://github.com/cartland/battery-butler/pull/160))

- **Agent self-improvement**: Added instructions for AI agents to update CLAUDE.md with learned best practices ([#159](https://github.com/cartland/battery-butler/pull/159))

### Fixes

- **Bazel disk cache**: Fixed issue where Bazel outputs weren't materialized when called from Xcode scripts ([#157](https://github.com/cartland/battery-butler/pull/157)) - Use `--disk_cache=""` to ensure files are created locally.

- **Duplicate Gradle module**: Removed duplicate `:server:app` entry in settings.gradle.kts ([#156](https://github.com/cartland/battery-butler/pull/156))

---

## 2026-01-30

### Performance

- **LazyColumn optimization**: Added `key` parameter to LazyColumn items for stable recomposition ([#151](https://github.com/cartland/battery-butler/pull/151)) - Improves scroll performance and prevents unnecessary recompositions.

- **Memory leak detection**: Added LeakCanary for debug builds ([#150](https://github.com/cartland/battery-butler/pull/150)) - Helps identify memory leaks during development.

### Accessibility

- **iOS decorative icons**: Hide decorative device icon from VoiceOver ([#146](https://github.com/cartland/battery-butler/pull/146))

- **Content descriptions**: Improved content descriptions for icons across the app ([#144](https://github.com/cartland/battery-butler/pull/144))

### Refactoring

- **Scoped dependency injection**: Introduced `AppDataModule` for better DI organization ([#143](https://github.com/cartland/battery-butler/pull/143))

### Fixes

- **Sort null handling**: Added null fallback for TYPE sort in HomeViewModel ([#140](https://github.com/cartland/battery-butler/pull/140))

- **JVM database migration**: Added missing MIGRATION_4_5 to JVM DatabaseFactory ([#139](https://github.com/cartland/battery-butler/pull/139))

- **Lifecycle-aware state**: Use `collectAsStateWithLifecycle` for proper lifecycle awareness ([#138](https://github.com/cartland/battery-butler/pull/138))

---

## 2026-01-29

### Features

- **Network permissions**: Added explicit INTERNET and ACCESS_NETWORK_STATE permissions for Play Store compliance ([#125](https://github.com/cartland/battery-butler/pull/125))

- **EmptyState preview**: Added preview for EmptyStateContent component ([#137](https://github.com/cartland/battery-butler/pull/137))

### Fixes

- **UI text overflow**: Added text overflow handling to EmptyStateContent ([#136](https://github.com/cartland/battery-butler/pull/136))

- **Keyboard handling**: Fixed keyboard behavior in AddBatteryEventContent ([#135](https://github.com/cartland/battery-butler/pull/135))

- **Navigation issues**: Modernized iOS navigation patterns and added rapid-click protection ([#134](https://github.com/cartland/battery-butler/pull/134), [#133](https://github.com/cartland/battery-butler/pull/133))

- **Icon suggestion debounce**: Added debouncing to prevent rapid API calls ([#132](https://github.com/cartland/battery-butler/pull/132))

- **Server sync**: Fixed missing DeviceType fields and timestamp preservation in ServerSyncMapper ([#131](https://github.com/cartland/battery-butler/pull/131), [#129](https://github.com/cartland/battery-butler/pull/129))

- **Day pluralization**: Fixed "1 days" → "1 day" in device list and history ([#130](https://github.com/cartland/battery-butler/pull/130))

### Refactoring

- **iOS modernization**: Updated Swift patterns and fixed deprecations ([#127](https://github.com/cartland/battery-butler/pull/127))

- **Kotlin data objects**: Converted singleton objects to data objects (Kotlin 2.0+) ([#126](https://github.com/cartland/battery-butler/pull/126))

---

## Earlier History

For changes before January 29, 2026, see the [commit history](https://github.com/cartland/battery-butler/commits/main) and [closed PRs](https://github.com/cartland/battery-butler/pulls?q=is%3Apr+is%3Aclosed).
