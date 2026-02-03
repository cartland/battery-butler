# ADR-003: Alpha Dependencies

## Status
Accepted

## Context
This project uses several alpha-stage dependencies. Each was chosen deliberately because stable versions either don't exist yet or lack required functionality. This document tracks why each is used and when we can migrate to stable versions.

## Decision

### Alpha Dependencies in Use

| Dependency | Version | Why Alpha | Exit Criteria |
|------------|---------|-----------|---------------|
| `androidx-room` | 2.7.0-alpha12 | KMP (Kotlin Multiplatform) support is only available in alpha releases. Stable Room only supports Android. | Stable 2.7.0 release with KMP support |
| `sqlite-bundled` | 2.5.0-alpha12 | Required by Room KMP for bundled SQLite on non-Android platforms | Follows Room stable release |
| `androidx-lifecycle-alpha` | 2.10.0-alpha05 | Required for Navigation3 compatibility. The JetBrains Compose lifecycle fork needs this version for nav3 integration. | Navigation3 1.0.0 stable release |
| `androidx-nav3` | 1.0.0-alpha02 | Modern Compose navigation library replacing deprecated Navigation-Compose. Provides type-safe navigation and better KMP support. | 1.0.0 stable release |
| `screenshot` | 0.0.1-alpha12 | Official Android Compose screenshot testing plugin. No stable version exists yet. | 1.0.0 stable release |
| `composeHotReload` | 1.0.0-alpha11 | Development-time hot reload for Compose. Not used in production builds. | 1.0.0 stable release (low priority - dev-only) |

### Risk Assessment

**Low Risk:**
- `composeHotReload` - Development only, doesn't affect production
- `screenshot` - Test infrastructure only, doesn't affect production

**Medium Risk:**
- `androidx-lifecycle-alpha` - Well-tested in alpha, minimal API surface used
- `androidx-nav3` - New but from Google/JetBrains, active development

**Higher Risk (mitigated):**
- `androidx-room` / `sqlite-bundled` - Core data persistence. Mitigated by:
  - Extensive Room testing in alpha versions
  - Our database schema is simple
  - Migration tests exist (`MigrationTest.kt`)
  - Can pin version if issues arise

## Monitoring Strategy

1. **Dependabot** will create PRs when new versions are available
2. **Release notes review** before updating alpha versions
3. **CI validation** catches breaking changes automatically

## Consequences

### Positive
- Access to modern KMP-first libraries
- Type-safe navigation
- Official screenshot testing
- Faster development iteration with hot reload

### Negative
- Potential breaking changes between alpha versions
- Less community support/documentation
- May need to adjust code when stable releases differ

### Mitigation
- Pin specific versions (don't use `+` or ranges)
- Test thoroughly before updating
- Document workarounds for any alpha-specific issues
- This ADR tracks all alpha dependencies for visibility

## Version History

| Date | Change |
|------|--------|
| 2026-02-02 | Initial documentation of alpha dependencies |
