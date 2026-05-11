---
description: Check Android screenshot reference images for blank renders and other health issues.
allowed-tools: Bash(*), Read, Glob, Grep
user-invocable: true
---

# Check Screenshot Health

Detect broken or blank screenshot reference images. This is a warning tool — it never blocks work. Broken screenshots may be app errors, preview errors, or test infrastructure issues.

## When to Use

- After generating screenshots (`/update-android-screenshots`)
- After changing preview composables or theme colors
- When reviewing a PR that modifies UI components
- Proactively during `/repo-check` to surface issues early

## Steps

### 1. Run the health check script

```bash
./scripts/check-screenshot-health.sh
```

This checks for:
- **Blank/tiny PNGs** (< 1KB) — the preview rendered empty, usually because it depends on runtime state unavailable in screenshot tests
- **Total vs healthy count** — how many screenshots are working

### 2. Report findings

If blank screenshots are found:
- List each one with its file path
- Explain the likely cause: the preview depends on a `ViewModel` collected via `collectAsStateWithLifecycle()`, on `LocalFileSaver`/`LocalFileLoader`, on `appComponent`, or on similar runtime state
- The fix pattern: create a stateless preview overload that accepts demo data as parameters and call **that** from the screenshot test
- **Do not block any work** — this is a high-priority note, not a gate

### 3. Suggested follow-ups

- If the same file owns the broken `@Preview`, propose the smallest possible split (a `XContent` overload that takes the data the wrapper would have collected).
- If the breakage is widespread (e.g. a new component was just added without a stateless preview overload), call it out as a pattern to fix at the source.

## Output Format

```
Screenshots:   N total, N healthy, N likely blank
Blank files:   (list each with path)
Action:        Create stateless preview overloads for blank screenshots
```

## Tips

- Blank screenshots are usually 69–200 bytes (just PNG header, no content)
- A preview that resolves runtime state via `LocalX.current` or `viewModel.something.collectAsState()` will be blank in screenshot tests
- The fix pattern: `@Composable fun FooContentPreview() { BatteryButlerTheme { FooContent(state = TestData.fooSuccess(), ...) } }` — pass data directly, don't reach into runtime
- Working examples in this repo: `HomeScreenEmptyPreview`, `HistoryListContentEmptyPreview`, `DeviceTypeListContentEmptyPreview` — all accept the state as a static value
