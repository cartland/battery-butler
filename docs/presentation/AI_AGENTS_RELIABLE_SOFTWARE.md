# The Pit of Success: Making AI Agents Write Reliable Software

> Three formats of the same talk: [Lightning Talk](#lightning-talk-5-10-min) | [Full Talk](#full-talk-20-30-min) | [Written Reference](#written-reference)

Evidence drawn from **Battery Butler**: a Kotlin Multiplatform app (Android, iOS, Desktop) with a Ktor gRPC server, built primarily with AI coding agents.

---

# Lightning Talk (5-10 min)

## Slide 1 — Title

**The Pit of Success: Making AI Agents Write Reliable Software**

*How to design a codebase where AI agents — and humans — naturally fall into doing the right thing.*

## Slide 2 — The Problem

AI coding agents are powerful. They write code fast, understand context, and work tirelessly.

But they make mistakes. Just like any contributor.

The question isn't **"Can AI agents code?"** — it's **"Can your codebase catch their mistakes before they ship?"**

## Slide 3 — The Insight

Two structural goals make a codebase resilient to mistakes from *any* contributor — human or AI:

1. **Make it hard to do the wrong thing** — automated enforcement, guardrails, build failures
2. **Make it easy to see the right thing** — actionable error messages, inline signals, step-by-step playbooks

Together, these create a **wide pit of success**: a codebase where the path of least resistance leads to correct, consistent code.

## Slide 4 — Hard to Do Wrong: Architecture Enforcement

A custom Gradle task encodes the **allowed dependency graph** for every module. Violations fail the build instantly.

```
Module ':viewmodel' depends on forbidden module ':data'.
Allowed: [:usecase, :domain, :presentation-model].
```

The agent doesn't need to remember the architecture. The build tells it.

**Key insight:** Module boundaries enforced by the compiler are worth more than any number of architecture diagrams.

## Slide 5 — Hard to Do Wrong: Convention Tests

Reflection-based tests scan the codebase at build time and assert structural invariants:

- Every `UseCase` must have `operator fun invoke()` — enforced by `UseCaseConventionTest`
- Every `ViewModel` must have a corresponding test file — enforced by `ViewModelTestConventionTest`

If an agent creates a new ViewModel without a test file, the build fails and tells it exactly what to create.

## Slide 6 — Hard to Do Wrong: Git Guardrail Hooks

Eight guardrails run as pre-tool-use hooks on every git command the agent issues:

- Can't push to `main` — must use a feature branch and PR
- Can't force push — destructive operations are blocked
- Can't push without validation — must run `validate.sh` first
- Can't skip squash merge, create tags, or run destructive git commands

The agent literally cannot do these things. No willpower required.

## Slide 7 — Easy to See the Right Thing: Actionable Error Messages

Every violation message includes **what's wrong** and **how to fix it**:

```
presentation-feature/src/.../EditDeviceScreen.kt:42 [no-raw-color-literal]
  Raw Color literals must be defined in presentation-core/theme/
  Fix: Use MaterialTheme.colorScheme.* or LocalButlerColors.current instead
```

The agent reads this, understands it, and fixes it — without asking a human.

**Standard format:** `file:line [rule-id] Description\n  Fix: Concrete action`

## Slide 8 — Easy to See the Right Thing: Preview Coverage Check

A Gradle task scans all `@Preview` composables and cross-references them against screenshot test files. It reports exactly which previews are missing tests:

```
Screenshot coverage gap: 2 preview(s) missing tests:
  - AddDeviceFilledPreview (presentation-feature/src/.../AddDeviceScreen.kt)
  - AddDeviceTypeErrorPreview (presentation-feature/src/.../AddDeviceTypeScreen.kt)
```

The agent knows exactly what to do next.

## Slide 9 — Easy to See the Right Thing: Workflow Playbooks

29 step-by-step playbooks in `.agent/workflows/` cover every common operation — from running tests to deploying the server to creating PRs.

The agent reads the recipe. Follows it. Gets it right the first time.

No tribal knowledge. No "ask the senior engineer." The process is documented and executable.

## Slide 10 — The Numbers

| Metric | Value |
|--------|-------|
| Lines of code | 36,846 |
| Modules | 22 |
| Unit tests | ~200 |
| Screenshot tests | 223 (131 Android + 92 iOS) |
| Custom Gradle checks | 4 |
| Git guardrails | 8 |
| Convention tests | 2 |
| Workflow playbooks | 29 |
| CI time (dev mode) | ~5.5 min |

All of this is maintained primarily by AI agents — guided by the pit of success.

## Slide 11 — Call to Action

You don't need all of this on day one. Start with **one custom check**.

Think about the code review comment you leave most often. Now turn it into a lint rule, a convention test, or a Gradle task.

**Make the pit of success wider.** Every check you add makes your next contributor — human or AI — more productive and less error-prone.

---

# Full Talk (20-30 min)

## Part 1 — The Premise

### The New Reality

AI coding agents are becoming a regular part of software teams. They can:
- Read and understand large codebases
- Write implementations that follow patterns they see
- Run tests, read errors, and iterate

But they also:
- Forget constraints that aren't encoded in the build
- Take shortcuts when docs and enforcement disagree
- Don't have the tribal knowledge that experienced team members carry

### The Thesis: Pit of Success

The **pit of success** is a design principle from programming language design: make the easy path the correct path. In a well-designed API, you have to work hard to misuse it.

Applied to codebases: **make the default workflow produce correct, consistent code — and make deviations produce immediate, helpful feedback.**

Two structural goals:
1. **Make it hard to do the wrong thing** — the build fails, the hook blocks, the test catches it
2. **Make it easy to see the right thing** — the error message explains the fix, the playbook documents the process

Together they create **lanterns in the pit**: signals that light up when you stray, guiding you back to the correct path.

### Why This Matters for AI Agents

Humans build intuition over time. They learn from code reviews, absorb team culture, develop judgment about "how we do things here."

AI agents start fresh every session. They read docs, they follow instructions, but they don't *remember* the lesson from last week's code review.

This means the codebase itself must encode the lessons. Not as docs that might be read, but as **checks that must be passed**.

## Part 2 — Make It Hard to Do Wrong

### The Escalation Ladder

Not every rule needs the same enforcement mechanism. Choose the right tool:

| Level | Mechanism | Best For | Example |
|-------|-----------|----------|---------|
| 1 | **Detekt rule** | Kotlin code patterns | `ForbiddenMethodCall`, `FunctionNaming` |
| 2 | **Custom Gradle task** | Cross-file scanning, dependency graphs | `ArchitectureCheckTask`, `ThemeLayerCheckTask` |
| 3 | **Convention test** | Runtime reflection: "every X has a Y" | `UseCaseConventionTest` |
| 4 | **Claude Code hook** | Agent-specific workflow guardrails | `git-guardrails.sh` |

Each level is more powerful but requires more effort to maintain. Start at level 1 and escalate only when needed.

### Architecture Enforcement (Custom Gradle Task)

The `ArchitectureCheckTask` defines an allowed-dependency map for every module in the project:

```kotlin
private val allowedDependencies = mapOf(
    ":domain" to listOf(),                    // Domain depends on nothing
    ":usecase" to listOf(":domain", ":presentation-model"),
    ":viewmodel" to listOf(":usecase", ":domain", ":presentation-model"),
    ":data" to listOf(":domain", ":data-local", ":data-network"),
    ":presentation-feature" to listOf(
        ":presentation-core", ":presentation-model",
        ":compose-resources", ":viewmodel", ":domain",
    ),
    ":compose-app" to listOf("*"),            // App wires everything
    // ... 20+ modules total
)
```

At build time, it walks every subproject's dependency configurations. If a dependency isn't in the allowed list:

```
Architecture Validation Failed:
Module ':viewmodel' depends on forbidden module ':data'.
Allowed: [:usecase, :domain, :presentation-model]
```

**What this catches:** An agent adding a "quick" import from `:viewmodel` to `:data` to grab a repository directly. The architecture check stops this immediately and the error message tells the agent to move data access into a UseCase.

The task also enforces domain purity — the `:domain` module must be pure Kotlin with zero Android dependencies.

### Theme Layer Enforcement (Custom Gradle Task)

The `ThemeLayerCheckTask` prevents raw color literals and dark-theme checks from leaking into feature modules:

```kotlin
private val rules = listOf(
    Rule(
        id = "no-raw-color-literal",
        pattern = Regex("""Color\(0x"""),
        message = "Raw Color literals must be defined in presentation-core/theme/",
        fix = "Use MaterialTheme.colorScheme.* or LocalButlerColors.current instead",
    ),
    Rule(
        id = "no-dark-theme-check",
        pattern = Regex("""isSystemInDarkTheme\(\)"""),
        message = "Theme detection must happen in presentation-core",
        fix = "Use DeviceIconMapper.getResolvedIconAccent() or LocalButlerColors.current instead",
    ),
    Rule(
        id = "no-color-import",
        pattern = Regex("""import\s+androidx\.compose\.ui\.graphics\.Color"""),
        message = "Import Color only in presentation-core/theme/",
        fix = "Access colors through MaterialTheme.colorScheme.* or LocalButlerColors.current",
    ),
)
```

The violation format is standardized:

```
Theme Layer Check Failed (1 violation(s)):
  presentation-feature/src/.../DeviceDetailScreen.kt:42 [no-raw-color-literal]
    Raw Color literals must be defined in presentation-core/theme/
    Fix: Use MaterialTheme.colorScheme.* or LocalButlerColors.current instead
```

**Why this matters:** AI agents love to inline colors. `Color(0xFF537A66)` is faster than looking up the theme token. This check catches it instantly.

### Convention Tests (Reflection-Based)

Convention tests use runtime reflection to assert structural invariants:

**ViewModelTestConventionTest** — Every ViewModel class must have a corresponding test file:
```
Test coverage gap: 1 class(es) missing tests:

viewmodel/src/commonMain/.../HomeViewModel.kt:15 [test-coverage]
  Class 'HomeViewModel' has no corresponding test file.
  Fix: Create 'HomeViewModelTest.kt' in a test source set,
       or add '// @NoTestRequired: <reason>' above the class.
```

**UseCaseConventionTest** — Every UseCase must follow the `operator fun invoke()` convention, ensuring call sites read like plain functions.

Convention tests run with `./gradlew test` — no special CI configuration needed.

### Git Guardrail Hooks (Agent-Specific)

Eight guardrails run as pre-tool-use hooks on every Bash command the agent executes:

| # | Guardrail | What It Blocks |
|---|-----------|---------------|
| 1 | Push validation | Pushing without running `validate.sh` first |
| 2 | Push to main | Direct pushes to `main` or `master` |
| 3 | Force push | `--force`, `--force-with-lease` |
| 4 | Squash merge | `gh pr merge` without `--squash` |
| 5 | Tag creation | Creating tags (must use release scripts) |
| 6 | Destructive commands | `git reset --hard`, `git clean -f`, `git checkout .` |
| 7 | `git -C` | Running git from a different directory |
| 8 | Shell control flow | Warning on `for`/`while`/`if` (prefer separate tool calls) |

The hooks use pattern matching on the command string after stripping quoted strings and heredocs:

```bash
# Strip heredoc bodies first, then quoted strings
STRIPPED=$(echo "$COMMAND" | sed '/<<.*EOF/,/^EOF/d' | \
  sed -E "s/'[^']*'//g; s/\"[^\"]*\"//g")

# Block push to main
if echo "$STRIPPED" | grep -qE '\bgit\s+push\b.*\b(main|master)\b'; then
  deny "BLOCKED: Never push directly to main. Create a branch and open a PR."
fi
```

**Key design decision:** Hooks guard the agent, not the human. Human developers use the same linters and CI checks; hooks add agent-specific workflow safety.

## Part 3 — Make It Easy to See the Right Thing

### Error Message Standards

Every violation message follows a standard format:

```
file:line [rule-id] Description of violation
  Fix: Concrete action to take
```

**Bad:**
```
Architecture violation
```

**Good:**
```
Module ':viewmodel' depends on forbidden module ':data'.
Allowed: [:usecase, :domain, :presentation-model].
Move data access into a UseCase.
```

**Bad:**
```
Test missing
```

**Good:**
```
viewmodel/src/commonMain/.../FooViewModel.kt:12 [test-coverage]
  Class 'FooViewModel' has no corresponding test file.
  Fix: Create 'FooViewModelTest.kt' in a test source set,
       or add '// @NoTestRequired: <reason>' above the class.
```

This format is critical for AI agents. They parse error output, understand the fix, and apply it — *without human intervention*. The more specific your error messages, the more autonomous your agents become.

### Preview Coverage Check

The `PreviewCoverageCheckTask` connects two worlds:
1. Scans `presentation-core` and `presentation-feature` for `@Preview` composable functions
2. Scans `android-screenshot-tests` for import statements that reference those previews

If a preview exists without a screenshot test, the build tells you exactly which one:

```
Screenshot coverage gap: 2 preview(s) missing tests:
  - AddDeviceFilledPreview (presentation-feature/src/.../AddDeviceScreen.kt)
  - AddDeviceTypeErrorPreview (presentation-feature/src/.../AddDeviceTypeScreen.kt)
```

This creates a virtuous cycle: adding a `@Preview` (which is natural during development) automatically creates pressure to add its screenshot test.

### Test Coverage Enforcement

The `TestCoverageCheckTask` scans source modules for classes matching enforced patterns (UseCases, ViewModels, repositories, AI engines) and cross-references against test source sets:

```kotlin
val rules = listOf(
    ModuleRule(
        module = "usecase",
        classPatterns = listOf(Regex("""\w*UseCase""")),
        sourceSets = listOf("commonMain"),
        testSourceSets = listOf("commonTest", "jvmTest"),
    ),
    ModuleRule(
        module = "viewmodel",
        classPatterns = listOf(Regex("""\w*ViewModel""")),
        sourceSets = listOf("commonMain"),
        testSourceSets = listOf("commonTest", "desktopTest"),
    ),
    // ... data, ai modules
)
```

Escape hatches exist for special cases:
- Inline: `// @NoTestRequired: <reason>` above the class
- Central: `test-coverage-exemptions.txt` with pattern-based exemptions

Both require documenting *why* the test isn't needed — forcing a deliberate decision rather than silent omission.

### Workflow Playbooks

29 step-by-step playbooks in `.agent/workflows/` cover common operations:

| Category | Playbooks |
|----------|-----------|
| **Build & Run** | `run-android.md`, `run-desktop.md`, `run-ios-swiftui.md`, `run-server.md`, `build-docker.md`, `build-ios-framework.md` |
| **Testing** | `run-tests.md`, `update-screenshots.md`, `update-ios-screenshots.md`, `validate-changes.md` |
| **Release** | `release-android.md`, `deploy-server.md`, `promote-server.md`, `generate-mobile-release-notes.md` |
| **Code Quality** | `format-code.md`, `create-pr.md`, `merge-prs.md`, `prepare-commit-then-push.md` |
| **Maintenance** | `update-docs.md`, `update-project-docs.md`, `dump-context.md`, `sync-ios-pbxproj.md` |

Each playbook is both documentation and an executable recipe. The agent reads it and follows it step by step, without needing tribal knowledge.

### The Self-Improvement Loop

The agents are instructed to update `.agent/` documentation when they learn something new:

> "Always update `.agent/` documentation when learning a critical piece of information that will improve future agent performance."

This creates a feedback loop:
1. Agent encounters a problem
2. Agent solves it (with human guidance if needed)
3. Agent documents the solution in `.agent/` docs
4. Next agent (or same agent, next session) reads the docs and avoids the problem

Over time, the docs get better, the playbooks get more complete, and the agents get more autonomous.

## Part 4 — The Testing Pyramid

### Variety of Techniques

| Layer | What It Proves | Count |
|-------|---------------|-------|
| **Unit tests** | Business logic is correct (UseCases, ViewModels, data layer, AI layer) | ~200 |
| **Convention tests** | Structural invariants hold (every UseCase has invoke, every ViewModel has a test) | 2 |
| **Screenshot tests** | UI renders correctly across all screens and states (light + dark) | 223 |
| **Instrumented tests** | App navigates without crashing on real Android device | ~10 |
| **Architecture checks** | Module dependency rules are enforced | 4 Gradle tasks |
| **Code style** | Formatting, naming, Compose rules | Detekt + Spotless |

### Two-Mode CI System

CI runs in two modes controlled by a single file (`.github/ci-mode.txt`):

**Development mode** (~5.5 min):
- Spotless, Lint, Detekt, Unit tests, Architecture checks, Theme layer checks
- Fast feedback for iteration

**Release mode** (full suite):
- Everything in development mode plus screenshot tests, instrumented tests, iOS builds
- Complete safety net before releases

The agent gets fast feedback during development and full validation before shipping.

### Screenshot Testing Strategy

Three tiers:
- **Tier 1 (Required):** Every screen x every key state (Success, Empty, Loading, NotFound) x Light + Dark
- **Tier 2 (Recommended):** Data variations, edge cases, form states
- **Tier 3 (Optional):** Individual components, alternate form factors

Current state: **Tier 1 complete** on both platforms — 131 Android PNGs, 92 iOS PNGs.

OOM safeguard: max 10 screenshot tests per file. This was learned the hard way when a 15-test file caused CI runners to run out of memory.

## Part 5 — Putting It All Together

### What a Typical Agent Session Looks Like

1. Agent reads `.agent/AGENTS.md` and `.agent/project.md` (entry points)
2. Agent creates a feature branch from `origin/main`
3. Agent implements the change, guided by architecture checks and convention tests
4. Agent runs `./scripts/validate.sh` (CI-parity local gate)
5. If violations: reads error messages, fixes them, re-runs
6. Agent commits, pushes, creates PR using the `/create-pr` workflow
7. CI runs and verifies everything

At every step, the pit of success is guiding the agent toward correct behavior. The agent doesn't need to remember all the rules — the build remembers for it.

### The Cumulative Effect

Each individual check is simple. But together they compound:

- Architecture enforcement prevents coupling
- Convention tests prevent gaps
- Theme checks prevent inconsistency
- Git hooks prevent workflow mistakes
- Playbooks prevent process mistakes
- Actionable errors prevent confusion

The result: a codebase where AI agents contribute at high quality with minimal human oversight.

### Start Small, Build Up

You don't need all of this on day one. The escalation path:

1. **Week 1:** Turn your most common code review comment into a lint rule
2. **Week 2:** Add one convention test for a structural invariant you care about
3. **Week 3:** Write your first workflow playbook for a process the agent gets wrong
4. **Month 2:** Add a custom Gradle task for a cross-file rule
5. **Month 3:** Add git hooks for workflow safety

Each addition makes the next contribution — human or AI — a little more reliable.

---

# Written Reference

This section provides comprehensive documentation of every technique, with code examples, configuration details, and cross-references.

## Project Overview

**Battery Butler** is a Kotlin Multiplatform application for tracking household battery replacements. It runs on Android, iOS, and Desktop, with a Ktor gRPC server deployed on AWS ECS Fargate.

| Metric | Value | Source |
|--------|-------|--------|
| Total lines of code | 36,846 | `docs/CODE_ANALYSIS.md` |
| Language split | 89.3% Kotlin, 10.7% Swift | `docs/CODE_ANALYSIS.md` |
| Modules | 22 | `settings.gradle.kts` |
| Shared code | 67.8% (24,972 lines) | `docs/CODE_ANALYSIS.md` |
| Unit tests | ~200 | `*/src/commonTest/`, `*/src/jvmTest/`, `*/src/desktopTest/` |
| Android screenshot tests | 131 PNGs, 74 test functions, 14 files | `android-screenshot-tests/` |
| iOS snapshot tests | 92 PNGs, 46 test functions, 19 files | `ios-app-swift-ui/iosAppSwiftUITests/` |
| Custom Gradle checks | 4 | `buildSrc/src/main/kotlin/` |
| Convention tests | 2 | `usecase/src/jvmTest/`, `viewmodel/src/desktopTest/` |
| Git guardrails | 8 | `.claude/hooks/git-guardrails.sh` |
| Workflow playbooks | 29 | `.agent/workflows/` |
| ADRs | 4 | `docs/architecture/adr-*.md` |
| CI time (dev mode) | ~5.5 min | `.github/workflows/ci.yml` |

## Complete Techniques Inventory

Every engineering technique in use, organized by category:

### API Design

| Technique | Implementation | Location |
|-----------|---------------|----------|
| `Result<D, E>` sealed type | `Success<D>` / `Error<E>` with `map`, `flatMap`, `getOrNull`, `onSuccess`, `onError` | `domain/src/commonMain/.../model/Result.kt` |
| Sealed error hierarchies | `DataError` (Network, Database, Ai, Unknown), `AuthError` (ConfigurationNotConfigured, SignInFailed, SignOutFailed) | `domain/src/commonMain/.../model/` |
| `operator fun invoke()` | All use cases enforce callable convention via `UseCaseConventionTest` | `usecase/src/commonMain/` |
| `@Deprecated` with `ReplaceWith` | `DataResult<T>` → `Result<T, DataError>` migration guide | `domain/src/commonMain/.../model/DataResult.kt` |
| Exhaustive `when` | `ElseCaseInsteadOfExhaustiveWhen` detekt rule on sealed/enum types | `detekt.yml` |

### Architecture

| Technique | Implementation | Location |
|-----------|---------------|----------|
| Module dependency enforcement | `ArchitectureCheckTask` — allowed-dependency map | `buildSrc/src/main/kotlin/architecture/ArchitectureCheckTask.kt` |
| Theme layer boundary | `ThemeLayerCheckTask` — blocks raw `Color()`, `isSystemInDarkTheme()` | `buildSrc/src/main/kotlin/themelayer/ThemeLayerCheckTask.kt` |
| Domain purity | `:domain` depends on nothing — pure interfaces and models | `domain/` module |
| Clean architecture layers | domain → usecase → viewmodel → presentation | Enforced by `ArchitectureCheckTask` |
| kotlin-inject DI | Constructor injection with `@Inject` / `@Component` | `compose-app/.../di/AppComponent.kt` |
| `DispatcherProvider` abstraction | Interface in domain, `DefaultDispatcherProvider` in data | `domain/.../model/DispatcherProvider.kt` |
| ADR documentation | 4 architectural decision records | `docs/architecture/adr-*.md` |

### Testing

| Technique | Implementation | Location |
|-----------|---------------|----------|
| Unit tests (~200) | UseCase, ViewModel, data layer, AI layer, Gradle task tests | `*/src/commonTest/`, `*/src/jvmTest/`, `*/src/desktopTest/`, `buildSrc/src/test/` |
| Convention tests (2) | `UseCaseConventionTest`, `ViewModelTestConventionTest` | `usecase/src/jvmTest/`, `viewmodel/src/desktopTest/` |
| Android screenshot tests | 131 PNGs — Tier 1 complete (all screens x key states x light/dark) | `android-screenshot-tests/src/screenshotTestDebug/` |
| iOS snapshot tests | 92 PNGs — Tier 1 complete with dark mode | `ios-app-swift-ui/iosAppSwiftUITests/` |
| Fakes over mocks | `FakeDeviceRepository`, `FakeAuthRepository`, `FakeAiEngine`, etc. (7 fakes) | `test-common/src/commonMain/.../testcommon/` |
| Test data builders | `TestDevices.createDevice()`, `createDeviceType()`, `createBatteryEvent()` | `test-common/.../TestDevices.kt` |
| CrashProof pattern | Tests that ViewModel errors surface as `Result.Error`, not uncaught exceptions | `viewmodel/src/commonTest/.../CrashProof*Test.kt` |
| Tiered screenshot strategy | Tier 1 (required), Tier 2 (recommended), Tier 3 (optional) | `docs/SCREENSHOT_STRATEGY.md` |
| Two-mode CI | Development (fast) vs release (full) via `.github/ci-mode.txt` | `.github/workflows/ci.yml` |
| Navigate-all-screens smoke test | Instrumented test visiting 12 of 13 screens | `compose-app/src/androidTest/` |

### Custom Enforcement

| Technique | Implementation | Location |
|-----------|---------------|----------|
| Custom Gradle tasks (4) | `checkArchitecture`, `checkThemeLayer`, `checkTestCoverage`, `checkPreviewCoverage` | `buildSrc/src/main/kotlin/` |
| detekt + compose plugin | Code patterns, naming, complexity, modifier naming/ordering | `detekt.yml` |
| Spotless + ktlint | Formatting, import ordering, whitespace | `build.gradle.kts` |
| Git guardrail hooks (2 scripts) | 8 guardrails + mode-aware admin bypass | `.claude/hooks/` |
| CI-parity gate | `validate.sh` writes HEAD hash; hook checks before push | `scripts/validate.sh` |
| Post-merge auto-generation | Screenshots, diagrams, analysis auto-generated on `main` | `.github/workflows/auto-generate.yml` |

## The Four Custom Gradle Tasks — In Detail

### 1. `checkArchitecture` — Module Dependency Enforcement

**Purpose:** Prevents modules from depending on modules they shouldn't. Enforces clean architecture layers (domain → usecase → viewmodel → presentation).

**Mechanism:** Defines an allowed-dependency map. At build time, walks every subproject's dependency configurations and checks each `ProjectDependency` against the allowed list. Also verifies `:domain` is pure Kotlin (no Android plugins or dependencies).

**Complete allowed-dependency map:**

```kotlin
private val allowedDependencies = mapOf(
    ":domain" to listOf(),                          // Pure — depends on nothing
    ":ai" to listOf(":domain", ":presentation-model"),
    ":data-local" to listOf(":domain"),
    ":data-network" to listOf(":domain", ":fixtures"),
    ":data" to listOf(":domain", ":data-local", ":data-network"),
    ":usecase" to listOf(":domain", ":presentation-model"),
    ":presentation-model" to listOf(":domain"),
    ":viewmodel" to listOf(":usecase", ":domain", ":presentation-model"),
    ":presentation-core" to listOf(
        ":domain", ":presentation-model", ":compose-resources"
    ),
    ":presentation-feature" to listOf(
        ":presentation-core", ":presentation-model",
        ":compose-resources", ":viewmodel", ":domain",
    ),
    ":compose-resources" to listOf(),
    ":fixtures" to listOf(":domain"),
    ":compose-app" to listOf("*"),                  // App wires everything
    ":ios-swift-di" to listOf("*"),
    ":server:domain" to listOf(":domain"),
    ":server:data" to listOf(":server:domain", ":domain", ":fixtures"),
    ":server:app" to listOf(":server:domain", ":server:data", ":domain"),
    ":git" to listOf(),
    ":scripts" to listOf(),
)
```

**Violation message format:**

```
Architecture Validation Failed:
Module ':viewmodel' depends on forbidden module ':data'. Allowed: [:usecase, :domain, :presentation-model]
```

**Domain purity check:**

```
Module ':domain' must be Pure Kotlin but has Android plugins applied.
Module ':domain' depends on Android library 'androidx.room:room-runtime'. Domain must be pure.
```

**Source:** `buildSrc/src/main/kotlin/architecture/ArchitectureCheckTask.kt`

### 2. `checkThemeLayer` — Theme Boundary Enforcement

**Purpose:** Prevents raw color literals, dark-theme detection, and Color imports from leaking into `presentation-feature`. All color definitions must live in `presentation-core/theme/`.

**Mechanism:** Three regex-based rules scan all `.kt` files in `presentation-feature/src` (excluding test directories):

| Rule ID | Pattern | What It Catches |
|---------|---------|----------------|
| `no-raw-color-literal` | `Color(0x` | Inline hex color values |
| `no-dark-theme-check` | `isSystemInDarkTheme()` | Direct theme mode detection |
| `no-color-import` | `import androidx.compose.ui.graphics.Color` | Color class imports |

**Violation message format:**

```
Theme Layer Check Failed (1 violation(s)):
  presentation-feature/src/.../EditDeviceScreen.kt:42 [no-raw-color-literal]
    Raw Color literals must be defined in presentation-core/theme/
    Fix: Use MaterialTheme.colorScheme.* or LocalButlerColors.current instead
```

**Source:** `buildSrc/src/main/kotlin/themelayer/ThemeLayerCheckTask.kt`

### 3. `checkTestCoverage` — Test File Existence Enforcement

**Purpose:** Ensures enforced class patterns (UseCases, ViewModels, repository implementations, AI engines) have corresponding test files.

**Mechanism:** Scans source modules for classes matching patterns, cross-references against test source sets. Supports two escape hatches:

1. **Inline suppression:** `// @NoTestRequired: <reason>` above the class declaration
2. **Central exemptions file:** `test-coverage-exemptions.txt` with `ClassName | reason` entries

**Enforced module rules:**

| Module | Class Patterns | Test Source Sets |
|--------|---------------|-----------------|
| `usecase` | `*UseCase` | `commonTest`, `jvmTest` |
| `viewmodel` | `*ViewModel` | `commonTest`, `desktopTest` |
| `data` | `Default*` | `commonTest` |
| `ai` | `*AiEngine`, `*AiConfig` | `commonTest` |

**Excluded:** DI/provider directories, Factory/Component suffixes, `KmpViewModelStore`.

**Violation message format:**

```
Test coverage gap: 1 class(es) missing tests:

viewmodel/src/commonMain/.../HomeViewModel.kt:15 [test-coverage]
  Class 'HomeViewModel' has no corresponding test file.
  Fix: Create 'HomeViewModelTest.kt' in a test source set,
       or add '// @NoTestRequired: <reason>' above the class.
```

**Source:** `buildSrc/src/main/kotlin/testcoverage/TestCoverageCheckTask.kt`

### 4. `checkPreviewCoverage` — Preview-to-Screenshot Mapping

**Purpose:** Ensures every `@Preview` composable in presentation modules has a corresponding screenshot test.

**Mechanism:**
1. Scans `presentation-core` and `presentation-feature` for functions matching `@Preview...@Composable fun *Preview(`
2. Scans `android-screenshot-tests` for import statements referencing those preview function names
3. Reports any previews without screenshot test imports

**Violation message format:**

```
Screenshot coverage gap: 2 preview(s) missing tests:
  - AddDeviceFilledPreview (presentation-feature/src/.../AddDeviceScreen.kt)
  - AddDeviceTypeErrorPreview (presentation-feature/src/.../AddDeviceTypeScreen.kt)
```

**Source:** `buildSrc/src/main/kotlin/screenshotcoverage/PreviewCoverageCheckTask.kt`

## Convention Test Patterns

### UseCaseConventionTest

Scans all UseCase classes and asserts they have `operator fun invoke()`:

- **Location:** `usecase/src/jvmTest/`
- **What it enforces:** All use cases follow the callable convention, so call sites read like plain functions: `val result = myUseCase(params)` instead of `myUseCase.execute(params)`
- **Runs with:** `./gradlew test`

### ViewModelTestConventionTest

Scans all ViewModel classes and asserts a corresponding `*Test.kt` file exists:

- **Location:** `viewmodel/src/desktopTest/`
- **Why `desktopTest`:** The viewmodel module uses `jvm("desktop")` target, so `desktopTest` is the JVM test source set
- **What it enforces:** Every ViewModel has at least a test file — the test itself may be minimal, but the file must exist
- **Runs with:** `./gradlew test`

## Git Hook System Architecture

### Overview

Two hook scripts in `.claude/hooks/`:

1. **`git-guardrails.sh`** — 8 guardrails for git workflow safety
2. **`block-admin-bypass.sh`** — Mode-aware (warns in development, blocks in release)

### How Hooks Work

Hooks run as **pre-tool-use** handlers. When the agent invokes a Bash command, the hook receives the command string via stdin as JSON. The hook can:

- **Allow** (exit 0, no output) — command runs normally
- **Deny** (output JSON with `permissionDecision: "deny"`) — command is blocked
- **Warn** (print to stderr) — command runs but agent sees a warning

### Processing Pipeline

```bash
# 1. Read the command from JSON input
INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# 2. Strip heredoc bodies first (before quote stripping removes EOF markers)
STRIPPED=$(echo "$COMMAND" | sed '/<<.*EOF/,/^EOF/d')

# 3. Strip quoted strings to avoid false positives on prose content
STRIPPED=$(echo "$STRIPPED" | sed -E "s/'[^']*'//g; s/\"[^\"]*\"//g")

# 4. Run each guardrail check on the stripped command
```

### The Eight Guardrails

| # | Guardrail | Check | Action |
|---|-----------|-------|--------|
| 1 | **Push validation** | `validate.sh` marker matches current HEAD | Warn if stale |
| 2 | **Push to main** | `git push` targeting `main`/`master`, or current branch is main | Block |
| 3 | **Force push** | `--force` or `--force-with-lease` flag | Block |
| 4 | **Squash merge** | `gh pr merge` without `--squash` | Block |
| 5 | **Tag creation** | `git tag` without `-l`/`--list` | Block |
| 6 | **Destructive commands** | `git reset --hard`, `git clean -f`, `git checkout .` | Block |
| 7 | **`git -C`** | Running git from a different directory | Block |
| 8 | **Shell control flow** | `for`/`while`/`if` keywords | Warn only |

### Validation Marker System

The `validate.sh` script writes the current HEAD hash to `.claude/.validation-passed` on success. The push guardrail checks this marker:

```bash
MARKER="$REPO_ROOT/.claude/.validation-passed"
VALIDATED_HASH=$(cat "$MARKER" 2>/dev/null)
CURRENT_HEAD=$(git rev-parse HEAD 2>/dev/null)
if [ "$VALIDATED_HASH" != "$CURRENT_HEAD" ]; then
    # Check if changes are docs-only (exempt from validation)
    # Otherwise warn
fi
```

This ensures validation is tied to a specific commit — amending or adding commits after validation invalidates the marker.

## Screenshot Testing Strategy

### Tier System

| Tier | Requirement | Coverage |
|------|------------|----------|
| **Tier 1** | Required — every screen x every key state (Success, Empty, Loading, NotFound) x Light + Dark | Complete on both platforms |
| **Tier 2** | Recommended — data variations, edge cases, form states | Partial |
| **Tier 3** | Optional — individual components, alternate form factors, marketing | Android-heavy |

### Platform Parity

| Platform | PNGs | Test Functions | Files |
|----------|------|---------------|-------|
| Android | 131 | 74 | 14 |
| iOS | 92 | 46 | 19 |

Both platforms cover every screen in light and dark mode. Key structural differences:
- Android has Play Store marketing screenshots and a component gallery
- iOS has more form-state variants (filled forms, error states)
- AI overlay is Android-only (not yet built in SwiftUI)

### OOM Safeguard

Max 10 screenshot tests per file. This was established after a file with ~15 tests caused OOM failures on CI runners. The solution was splitting `ScreensScreenshotTest.kt` into multiple files.

## CI System

### Two-Mode Architecture

A single file, `.github/ci-mode.txt`, controls which jobs are required on PRs:

**Development mode (~5.5 min wall clock):**
- `spotless` (53s) — formatting
- `validation_lint` (5m19s) — Android Lint (bottleneck)
- `detekt` (56s) — Kotlin code patterns
- `validation_test` (3m42s) — unit tests
- `validation_architecture` — custom Gradle checks
- `theme_layer` (41s) — theme boundary enforcement

**Release mode (adds):**
- Screenshot tests
- Instrumented tests
- iOS builds
- Full test suite

### Push-to-Main Always Runs Everything

Regardless of mode, pushes to `main` trigger the full job suite. This ensures the main branch is always fully validated.

### Path Filtering

`dorny/paths-filter` skips builds for documentation-only changes. Files in `.beads/`, `.agent/`, and `*.md` don't trigger builds.

## Aspirational Goals

Techniques we want to build but haven't yet:

| Goal | Mechanism | Purpose |
|------|-----------|---------|
| `throw` checker | Gradle task scanning non-boundary modules | Enforce `Result<D, E>` pattern everywhere |
| `Dispatchers.*` checker | Gradle task scanning for direct dispatcher usage | Enforce `DispatcherProvider` abstraction |
| UiState → Preview convention test | Reflection on sealed UiState subclasses | Ensure every UI state variant has a preview |
| Property-based tests | kotest-property for pure functions | Better coverage of edge cases |
| Mutation testing | Pitest | Measure test effectiveness beyond line coverage |
| SRP heuristic | Convention test flagging large classes | Encourage single responsibility |
| Custom detekt AST rules | Graduate regex Gradle checks | Fewer false positives |

## Cross-Reference Index

| Topic | Primary Doc | ADR | Enforcement |
|-------|------------|-----|-------------|
| Module dependencies | `docs/Architecture.md` | ADR-001 | `checkArchitecture` Gradle task |
| Error handling | `.agent/project.md` § Error Handling | — | Convention (no automated check yet) |
| Test coverage | `docs/TESTING.md` | ADR-002 | `checkTestCoverage` Gradle task |
| Alpha dependencies | — | ADR-003 | Manual review |
| Single responsibility | `.agent/project.md` § SRP | ADR-004 | Convention (no automated check yet) |
| Theme layer | — | — | `checkThemeLayer` Gradle task |
| Screenshot tests | `docs/SCREENSHOT_STRATEGY.md` | — | `checkPreviewCoverage` Gradle task |
| UseCase conventions | — | — | `UseCaseConventionTest` |
| ViewModel test existence | — | — | `ViewModelTestConventionTest` |
| Code formatting | — | — | Spotless + ktlint |
| Kotlin patterns | — | — | detekt + compose plugin |
| Git workflow safety | `.agent/AGENTS.md` | — | `.claude/hooks/git-guardrails.sh` |
| CI modes | `.agent/ci.md` | — | `.github/ci-mode.txt` |
| Feature parity | `docs/FEATURE_PARITY_MAPPING.md` | — | Manual review |
| User journeys | `docs/USER_JOURNEYS.md` | — | Manual review |

## Key Takeaways

1. **Encode rules in checks, not docs.** A lint that fails the build is worth more than a paragraph in a wiki.

2. **Error messages are the UI for your checks.** Include the file, the line, what's wrong, and how to fix it. This is especially critical for AI agents, which parse error output to decide their next action.

3. **Use the escalation ladder.** Start with detekt rules (free), graduate to custom Gradle tasks (moderate effort), add convention tests for reflection-based invariants, and use hooks for agent-specific safety.

4. **Make escape hatches explicit.** `// @NoTestRequired: <reason>` forces a deliberate decision. Silent exemptions accumulate silently.

5. **Playbooks replace tribal knowledge.** When a process exists only in someone's head, it's a single point of failure. Write it down in a format that both humans and agents can follow.

6. **Start with one check.** Turn your most-violated code review comment into an automated rule. Then add another. The compound effect is remarkable.

---

*This presentation is based on Battery Butler, a KMP app with 36,846 lines of code across 22 modules, built primarily with AI coding agents. All code snippets and statistics are from the actual codebase.*
