# Android Smoke-Test Queue

Each Play Store internal-track release should be smoke-tested on a real device before promotion to production. This file is the durable queue: each release gets a `## android/N` section with a checklist. The release is "smoke-clear" once every item is checked or explicitly marked `n/a` with a reason.

**Why this exists:** android/30 shipped [bb-lg42](../TODO.md) (post-restore Loading bug) to internal track because no one walked the restore flow before promotion. android/31 was needed two days later to fix it. A 5-minute manual smoke test would have caught the regression. This queue makes the test durable and discoverable — future releases can't accidentally skip it without leaving a record.

## How to use

1. After `./scripts/release-android.sh` pushes the tag and Play Store internal upload completes, install the new build on a test device.
2. Add a new `## android/N` section below using the **template** at the bottom.
3. Walk each item in the checklist. Mark `[x]` for pass, `[F]` for fail with a one-line note, or `[~]` for "skipped — reason".
4. Any `[F]` blocks promotion to production. Add a bug task to `TODO.md` for the failure.
5. Commit the updated queue alongside (or before) promoting internal → production.

This is **advisory**, not a gate. The maintainer can promote without smoke-testing — but the queue file makes the gap visible.

## Critical paths (default checklist)

These are the user-facing flows that have broken historically and should be checked on every release. Add/remove items as the app grows.

- **App launch** — cold start succeeds, splash → home tab loads within ~2s
- **Sign-in / account state** — signed-in state persists across cold launch
- **Devices tab** — lists all devices for the signed-in user, items are tappable, each item navigates to detail and loads
- **Device Types tab** — loads list, items are tappable, navigation works
- **History tab** — loads list, scrolling works, items are tappable
- **Settings → Import/Export round-trip** — export current data, import back, verify lists are unchanged
- **Settings → Restore Previous Data** — restore from legacy DB, then **verify all three tabs (Devices / Device Types / History) load without an app restart** (this is the exact bb-lg42 regression)

## Queue

<!--
  Add the newest release at the top. Each section is one release.
  The release is "smoke-clear" only when every item has `[x]`, `[F]`, or `[~]`.
  Promotion to production should record the smoke-clear status here.
-->

## No release has been smoke-tested

**This queue has never been used.** It was created after android/30 shipped the `bb-lg42`
post-restore Loading bug to internal track and needed android/31 two days later — yet the
file below has only ever contained its template. Every release since has gone to the Play
Store internal track with no device pass.

That statement is deliberately phrased as an invariant rather than a list of release
numbers: it stays true (and stays accurate) until someone actually adds a `## android/N`
section, instead of going stale the moment the next release ships. As a snapshot, the
newest release at the time of writing was **android/67 (2026-07-31)**, and the run of
untested releases goes back at least to android/62 (2026-07-29).

Nothing here is filled in with checkmarks. Nobody walked these builds on a device, and
recording otherwise would be worse than recording nothing — the whole point of the file is
that *"the queue makes the gap visible"*, and an empty file made untested releases look
identical to tested ones.

**Decision needed from the maintainer.** Either:

- start the process from the next release and treat the backlog as accepted risk, or
- back-fill by walking the checklist against the current internal build (one pass covers
  the backlog, since the newest build is what's installed), or
- delete this file — a process doc that every release ignores is worse than no doc.

An agent cannot close this: it needs a real device.

### Template

```markdown
## android/N — YYYY-MM-DD

Tester: <name/role>
Device(s): <e.g., Pixel 8 / Android 14>
Internal track upload completed: <YYYY-MM-DD HH:MM>

- [ ] App launch
- [ ] Sign-in / account state
- [ ] Devices tab
- [ ] Device Types tab
- [ ] History tab
- [ ] Settings → Import/Export round-trip
- [ ] Settings → Restore Previous Data → all tabs load without restart

Notes:
- <any failures, surprises, or "checked X-specific path because release notes called it out">

Promoted to production: <YYYY-MM-DD or "not yet">
```

## See also

- `.claude/skills/release-android/SKILL.md` — release procedure (the script and gate that get a tag onto internal track)
- `bb-lg42` in `../TODO.md` — the post-restore Loading bug that motivated this queue
