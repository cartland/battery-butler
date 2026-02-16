# Closed PR Analysis

## Overview

**Total closed PRs: 484** — 314 merged, 170 closed without merge.

## Closed-Without-Merge Categories (170 PRs)

### 1. Superseded Auto-Generated PRs (141 PRs — 83%)

All from `auto-generate.yml` workflow (`app/github-actions` author). These create PRs for diagram regeneration or screenshot baseline updates on shared branches (`auto/update-generated-content`, `auto/update-screenshots`). Each new code merge triggers a fresh PR that force-replaces the previous one, causing the old PR to be closed.

This is **by design** — not a problem to fix.

### 2. Dependabot: Breaking Changes / CI Failures (4 PRs)

| PR | Title | Reason |
|----|-------|--------|
| #480 | Bump kotlin group 2.3.0→2.3.10 | All CI jobs fail — Kotlin 2.3.10 incompatible with Compose |
| #481 | Bump grpc group (9 updates) | 11 CI failures across all targets — breaking API changes |
| #265 | Bump grpc group 1.63.0→1.78.0 | Earlier version of same breaking grpc bump |
| #433 | Bump kotlin group 2.3.0→2.3.10 | Earlier version of same kotlin incompatibility |

### 3. Dependabot: Superseded by Newer Version (1 PR)

| PR | Title | Reason |
|----|-------|--------|
| #435 | Bump grpc group 1.63.0→1.79.0 | Replaced by #481 with same versions |

### 4. Dependabot: User Ignored Release (2 PRs)

| PR | Title | Reason |
|----|-------|--------|
| #267 | Bump h2 2.2.224→2.4.240 | User told dependabot to ignore |
| #436 | Bump lifecycle-alpha alpha05→alpha08 | User told dependabot to ignore |

### 5. Superseded by Better Implementation (8 PRs)

| PR | Title | Superseded by |
|----|-------|---------------|
| #16 | Fix: Add syncStatus to test fakes | #17 (includes fix + agent rules) |
| #68 | Testing: Add NetworkMode tests | #79 (comprehensive 12+ test cases) |
| #121 | Style: Fix spotless formatting | #128 (spotless + missing param fix) |
| #154 | CI: Revert to direct push | #163 (improved error handling) |
| #297 | Beads: Close bb-g8p | #300 (included in iOS build caching PR) |
| #406 | Fix: terraform state lock | #407 (removes ECR from terraform entirely) |
| #462 | Auto-update screenshot baselines | Newer auto-generated PR |
| #486 | Regenerate architecture diagrams | Closed — stale after #473 |

### 6. Consolidated / Reimplemented from Scratch (3 PRs)

| PR | Title | Reason |
|----|-------|--------|
| #450 | Gradle KMP Migration & Resource Fix | Consolidated with #452/#468 into fresh branches |
| #452 | Refactor: inject DispatcherProvider | Consolidated — reimplemented as #476 |
| #468 | Refactor: Move AI to Domain | Consolidated — reimplemented as #470 |

These were closed because multiple related PRs accumulated conflicts and it was cleaner to start fresh.

### 7. CI Failures / Incompatible with Main (4 PRs)

| PR | Title | Reason |
|----|-------|--------|
| #115 | Build: Improve Gradle performance settings | File system watching incompatible with CI; KMP hierarchy affected source sets |
| #147 | Ops: Configure Crashlytics and Kermit | Multiple CI failures, needed rework |
| #148 | UI: Migrate hardcoded strings to resources | Spotless failures, large migration needs careful review |
| #149 | Common: Introduce DispatcherProvider | Compilation failures, needed significant rework |

### 8. Intentional / Special Purpose (4 PRs)

| PR | Title | Reason |
|----|-------|--------|
| #19 | Docs: Architecture reconciliation plan | Plan document — closed in favor of direct implementation |
| #155 | Worker Swarm Plan Record | Intentionally not merged — historical record only |
| #247 | Test: Verify docs-only CI | Test PR — closed after confirming CI works correctly |
| #67 | Build: Upgrade appcompat | Wrong direction — was actually a downgrade |

### 9. Silently Closed / Obsoleted (3 PRs)

| PR | Title | Likely reason |
|----|-------|---------------|
| #199 | Docs: Add beads commit strategy | Obsoleted by evolving CLAUDE.md |
| #210 | Chore: Update beads tracking | Included in subsequent PR |
| #245 | Chore: Close bb-9h3 | Folded into another PR |

### 10. External Bot (1 PR)

| PR | Title | Reason |
|----|-------|--------|
| #1 | Fix GitHub Actions: Use setup-java and Java 21 | Google Jules bot — unsolicited, never merged |

## Summary Table

| Category | Count | % of closed-without-merge |
|----------|-------|--------------------------|
| Auto-generated (superseded by design) | 141 | 83% |
| Dependabot: breaking/CI failures | 4 | 2.4% |
| Dependabot: superseded by newer | 1 | 0.6% |
| Dependabot: user ignored | 2 | 1.2% |
| Superseded by better implementation | 8 | 4.7% |
| Consolidated / reimplemented | 3 | 1.8% |
| CI failures / incompatible | 4 | 2.4% |
| Intentional / special purpose | 4 | 2.4% |
| Silently closed / obsoleted | 3 | 1.8% |
| External bot | 1 | 0.6% |
