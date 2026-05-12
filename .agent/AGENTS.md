# AI Agent Contribution Guidelines

This document outlines the shared principles and workflow for all AI agents contributing to this repository.

## Guiding Principles

1.  **Single Source of Truth**: This directory, `.agent/`, is the single source of truth for all AI agent instructions.
    *   **`AGENTS.md` (This file):** The high-level charter and core workflow.
    *   **`project.md`:** Project-specific technical knowledge (architecture, builds, deployment, testing, task management).
    *   **`workflows/`:** Step-by-step playbooks for common tasks, serving as both detailed instructions for agents and user-triggerable commands (e.g., slash commands).
2.  **Consistency**: All agents must follow the workflows defined here to ensure predictable and consistent contributions.
3.  **Spotless is Mandatory**: All changes must be formatted by running `./scripts/spotless-apply.sh` before being committed. Full validation happens on PRs.

## Agent Role

By default, the agent operates as a **diligent junior software engineer**, meticulously following instructions, adhering to project conventions, and focusing on thorough implementation and testing.

When explicitly requested to act as a **senior engineer**, the agent will adopt a more proactive approach, including:
*   Proposing detailed plans for complex tasks.
*   Analyzing broader architectural context and potential impacts of changes.
*   Suggesting strategic improvements or refactoring opportunities.
*   Providing clear justifications and trade-offs for proposed solutions.

Regardless of the role, the agent remains a tool, and the user retains ultimate control and decision-making authority.

## 🚨 Critical Rules

1.  **NEVER Push Directly to `main`**:
    *   **Always** create a feature branch (`agent/your-branch-name`).
    *   **Always** open a Pull Request for changes.
    *   **No exceptions.** Even maintainer-requested changes (reverts, docs, beads updates) must go through a PR.

2.  **NEVER Create Tags or Deploy Without Explicit Permission**:
    *   **NEVER** create git tags (e.g., `android/N`, release tags) without explicit user approval.
    *   **NEVER** push tags manually - always use release scripts (e.g., `./scripts/release-android.sh`).
    *   **NEVER** trigger deployment workflows without explicit user approval.
    *   When the user says "deploy" or "release", **ASK** which target and confirm, then use the appropriate release script.
    *   Tags trigger production releases and cannot be easily undone.
    *   Release scripts provide safety checks, version validation, and confirmation prompts.

3.  **NEVER Deploy Directly to Production**:
    *   **All server deploys go to dev first.** Use `/deploy-server` or `./scripts/release-server.sh`.
    *   **Prod is always a promotion from dev.** Use `/promote-server` or `./scripts/promote-server.sh`.
    *   **NEVER** run `gh workflow run server-deploy-prod.yml` directly — always use the promote script.
    *   Validate on dev (E2E tests, manual checks) before promoting to prod.

    > **HIBERNATED (Feb 2026):** Server deploy workflows are disabled. The server
    > runs locally only. Skip `./scripts/deploy-status.sh` at session start.

4.  **ALWAYS Ask Before Destructive or Irreversible Actions**:
    *   Creating tags, deploying, force-pushing, deleting branches on remote, or any action that affects production requires explicit confirmation.
    *   When uncertain about scope, ask clarifying questions before proceeding.

5.  **ALWAYS Clean Up Branches After PR Merge**:
    *   Delete the local branch and verify the remote branch is deleted immediately after every PR merge.
    *   Use `gh pr merge --squash --delete-branch` to auto-delete remote branches.
    *   Run `git fetch --prune origin` to clean up stale remote refs.
    *   See **After Your PR is Merged** section for the full checklist.

## Build & Test Health

Keeping the build and tests healthy is a top priority. When you identify or fix build/test issues:

1. **Always Create PRs for Fixes**: Never leave build or test fixes uncommitted. Create a PR promptly so fixes are tracked and reviewed.

2. **Verify Before Closing Tasks**: Before marking a build/test verification task as complete:
   - Confirm the build/test actually runs successfully
   - If you made fixes, commit them and create a PR
   - Document any known issues or failures in the PR description

3. **Fix Forward**: When you encounter a broken build or test:
   - Investigate the root cause
   - Create a fix on a feature branch
   - Open a PR with clear description of the problem and solution
   - Don't just work around issues locally

4. **Test Categories**:
   - **Unit tests**: Must pass (`./gradlew test`)
   - **Instrumented tests**: Must run (all tests are offline-capable, no server needed)
   - **Screenshot tests**: Must run (baseline mismatches indicate UI changes, not broken infrastructure). Use the `/update-android-screenshots` and `/update-ios-screenshots` workflows to regenerate missing or changed baselines locally before pushing.
   - **Always regenerate and commit reference images** when adding or changing screenshot tests. Run `./scripts/generate-android-screenshots.sh` to generate PNGs, then commit the new/updated images in `android-screenshot-tests/src/screenshotTestDebug/reference/` alongside the test code. PRs that add screenshot tests without reference images are incomplete.

## Project Technical Rules

- **Configuration**:
  - **Always** check `local.properties` for sensitive or environment-specific configuration (e.g., API Keys, Server URLs).
  - Use `AppConfig` or `BuildConfig` to access these values in code, do NOT hardcode them.
  - **NEVER hardcode NLB hostnames or server URLs** in Kotlin source files. Use `BuildConfig.PRODUCTION_SERVER_URL` from data-network, or `ProductionServerUrl` (domain model) for modules without data-network dependency.
  - Server URL source of truth is the GitHub secret `PRODUCTION_SERVER_URL`, auto-synced from terraform output after deploys. See `.agent/server.md` → "Server URL Management" for the full flow.
  - **Compose stability config** (`compose_compiler_config.conf` at the repo root): plain pattern lines only — the parser used by the Kotlin Compose Compiler plugin does NOT accept `#` comments. A comment line causes `Error parsing stability configuration file on line 0`. Wire new modules into the config via `composeCompiler { stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_compiler_config.conf")) }` in the module's `build.gradle.kts`. See `.agent/project.md` → "Compose Stability Configuration" for the rationale.
  - **Kotlin data-class field removal breaks Swift K/N interop**: removing a field from a Kotlin `data class` consumed via K/N interop (e.g. `HomeScreenState` from `ios-app-swift-ui/iosAppSwiftUITests/*.swift`) compiles cleanly on commonMain but breaks Swift call sites that pass the field by name in a full-param init. Dev-mode CI skips `validation_ios_ui`, so the break only surfaces on push-to-main (or release-mode PR). Before removing a public field from a model consumed by iOS, grep Swift files for `<ModelName>(`/`field:` usage and update those sites in the same PR. Example: PR #1122 removed `HomeScreenState.groups` and `.devices`; the Swift fix landed in PR #1137 after the post-merge safety net caught the break.
  - **Kotlin data-class field _type changes_ break Swift K/N interop the same way**: changing the type of a public field on a state class consumed by Swift (e.g. `List<Device>` → `ImmutableList<Device>`) changes the Swift-side type signature even though Kotlin call sites still type-check. The same dev-mode CI blind spot applies (`validation_ios_ui` skipped on PRs). Same mitigation: grep Swift files for the model name and either update Swift inits in the same PR or, for a sweep that touches many state classes (see bb-bpbw), flip CI to release mode on the PR (`echo "release" > .github/ci-mode.txt`) so `validation_ios_ui` is required. Don't quietly do this on an unattended /loop.
  - **Migrating a KMP module that produces Compose Resources from `com.android.library` (legacy) to `com.android.kotlin.multiplatform.library` is _not_ a plug-and-play swap**: the new plugin generates `.cvr` files into the module's own intermediates but does NOT publish them as a consumable Android assets artifact for downstream consumers. The legacy plugin under KMP did expose them. Symptom: `assemble`, `lint`, `detekt`, unit tests all pass; at runtime `:compose-app:pixel5api34DebugAndroidTest` throws `org.jetbrains.compose.resources.MissingResourceException` with the path `composeResources/<package>.generated.resources/values/strings.commonMain.cvr`. Before pushing this kind of migration, **run `:compose-app:pixel5api34DebugAndroidTest` locally** — dev-mode CI skips `validation_instrumented` on PRs so the break only surfaces post-merge. Example: PR #1171 → #1172 revert on 2026-05-12. Follow-up bead: bb-7i84.
  - **Pinned dependencies need dependabot ignore**: if `build.gradle.kts` or `libs.versions.toml` has a `// Pinned to X.Y.Z` or `// Do not bump past X` comment, also add that dep to `.github/dependabot.yml` → `ignore:`. Otherwise dependabot will propose the bump weekly, and in dev-mode CI (where slow jobs are skipped on PRs) the broken bump can slip past PR review. Example: PR #1133 bumped `jib-gradle-plugin` past its documented 3.4.1 pin, broke `build_server` on main; reverted + added to ignore in PR #1146.

- **Self Improvements**:
  - **Always** update `.agent/` documentation when learning a critical piece of information that will improve future agent performance. Workflow rules go in `AGENTS.md`; project knowledge goes in `project.md`.
  - **Proactively suggest and implement meta-improvements**: Whenever you notice a repetitive task, recurring CI failures from the same root cause, or manual procedures that should be automated, you MUST suggest creating a new workflow (`.agent/workflows/` and `.claude/skills/`), an automation script (`scripts/`), Github Actions, or Git Hooks (e.g. `pre-commit` / `pre-push`). Actively engineer the project to require less manual agentic intervention in the future.
  - **Always** run `/dump-context` before ending a session where significant work was done. This captures tasks, decisions, and operational knowledge into beads and docs.

- **Session Start**:
  - Run `bd ready` to see current tasks. Run `bd list` for all open issues.
  - Run `/check-ci-issues` (or `gh issue list --label ci-failure --state open`) — if any `ci-failure` + `blocking` issue is open, **fixing main is priority zero** and PR auto-merges are paused until it closes. See `.agent/ci.md` → "Post-Merge Auto-Issue Safety Net".
  - **"0 open ci-failure issues" is necessary but not sufficient** for "main is healthy." The auto-issue workflow only fires after a full push-to-main CI completes (~22 min for `validation_ios_ui` alone). If the latest push-to-main run is still in progress, the safety net hasn't had a chance to file. Verify with `gh run list --branch main --workflow "Battery Butler CI" --event push --limit 1 --json conclusion,status` — only trust `conclusion: success` AND `status: completed` for the actual HEAD commit. Observed this session: reported "Issues: 0 open" in `/repo-check`, then 7 min later the workflow filed #1144 + #1145 for an in-progress failure I missed.
  - Run `./scripts/deploy-status.sh` to check server deployment state and drift.
  - For team sessions, use Claude's TaskCreate/TaskList for coordination — not `bd`.

- **Bash Commands**:
  - **Avoid** shell control flow keywords (`for`, `while`, `until`, `if`/`then`/`else`/`fi`, `do`/`done`) in a single bash command. Prefer separate Bash tool calls instead.
  - `&&`, `||`, `;`, and `|` (pipes) are allowed for simple chaining.
  - **Example**: Instead of `for f in *.kt; do echo $f; done`, make separate bash tool calls.
  - The `.claude/hooks/git-guardrails.sh` hook warns (but does not block) on shell control flow.
  - `grep -qE '--flag'` treats `--flag` as a grep option — use `grep -qF -- '--flag'` instead.
  - `jq` with `!=` can be unreliable — use `select(.conclusion == "")` instead.

- **`gh pr merge` is guard-railed**: `.claude/hooks/git-guardrails.sh` blocks `gh pr merge` invocations that don't pass `--squash --delete-branch`. It also blocks `gh pr merge --disable-auto`. To disable auto-merge on a PR, use the GraphQL mutation instead:
  ```bash
  gh api graphql -f query='mutation($id: ID!) { disablePullRequestAutoMerge(input: {pullRequestId: $id}) { pullRequest { number } } }' -F id="$(gh pr view <N> --json id --jq .id)"
  ```

- **Git**:
  - **Always** use non-interactive flags for commands that might open an editor (e.g., `git cherry-pick --continue --no-edit`). This prevents the shell from getting stuck waiting for user input.
  - **Always** escape special characters in command arguments (e.g., `$` and `` ` ``) to prevent unintended shell expansion. Use single quotes or backslashes (`\`) for escaping.
  - **NEVER** use heredoc subshells, multi-line quoted strings, or `&&`-chained write-then-use commands in `gh` or `git commit` commands. These cause Claude Code to require manual approval even when the command prefix is in the allow list, because permission patterns match only the **first command** in a chain.
  - Instead: **use the Write tool** to create files in `.tmp/` (gitignored local temp directory), then issue a **standalone** Bash command referencing the file:
    - Commit: `Write` to `.tmp/commit-msg.txt`, then `Bash(git commit -F .tmp/commit-msg.txt)`
    - PR: `Write` to `.tmp/pr-body.txt`, then `Bash(gh pr create --title "..." --body-file .tmp/pr-body.txt)`
  - This ensures each Bash call starts with an allowed prefix (`git commit`, `gh pr create`) with no chaining.
  - **`.tmp/` files persist across sessions** — always `Read` the file first (even if it might not exist) before `Write`, otherwise Write will fail with "File has not been read yet".
  - After PR merge deletes a remote branch, don't force-push — it re-creates the branch as an orphan. Check PR state first.
  - After a sibling PR merges, check remaining open PRs for merge conflicts (`gh pr view <N> --json mergeable`) and rebase if needed.
  - Worktree agents can fail silently (no push) — always verify remote branches exist after agent completion.
  - `gh pr create` has no `--auto` flag — create the PR first, then run `gh pr merge <N> --auto --squash`.
  - Heredoc stripping must run BEFORE quote stripping in hooks, otherwise `<<'EOF'` markers get consumed.

- **Pull Requests**:
  - **Always** ensure the Pull Request title and description accurately reflect the final changes. If the scope of a branch evolves, update the PR description before merging.
  - **Always verify PR state before reporting it to the user.** Never assume a PR's merge status, auto-merge status, or check status from memory — query it with `gh pr view <N> --json state,mergedAt,autoMergeRequest,mergeable`. Auto-merge can be silently cleared when the base branch updates or checks restart.
  - See [merge-strategy.md](merge-strategy.md) for merge sequencing and broken build handling.

- **iOS Builds**:
  - **Always** run `xcodebuild` from the repo root using `-project ios-app-swift-ui/iosAppSwiftUI.xcodeproj`. Never `cd` into subdirectories — it changes the working directory for subsequent commands and causes confusion.
  - **Always** use `-derivedDataPath ios-app-swift-ui/build/<target_name>` (e.g., `ios-app-swift-ui/build/ios-build`) when running `xcodebuild`. This ensures **artifact isolation** between steps, mimicking CI parity, and prevents accidental cross-linking of frameworks.
  - **Always** use `-target` instead of `-scheme` if the scheme file is not shared (checked into git).
  - **Always** disable code signing for local simulator builds or CI builds without certificates using `CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO`.
  - **Always** specify the OS version for simulator destinations (e.g., `name=iPhone 16,OS=18.5`). Omitting `OS` causes "device not found" when multiple OS versions are installed.

- **Bazel**:
  - **Bazel Outputs:** All Bazel outputs are consolidated in `.bazel/` (e.g. `.bazel/bin`) via `.bazelrc`. This directory is gitignored and excluded from Spotless.
  - **Spotless vs Bazel:** Spotless exclusions are configured to ignore `.bazel/`. Running `spotlessApply` works safely even with Bazel symlinks present.

- **Validation**:
  - **Always** run `./scripts/validate.sh` before pushing to main. This script is maintained to match `ci.yml` strictly.
  - **Always** run `./scripts/spotless-apply.sh` and fix errors before pushing to main.
  - **Every plan must include `./scripts/validate.sh` as a verification step.** If a plan lists abbreviated checks (e.g. `compileDebugSources + spotless + test`), replace or append `./scripts/validate.sh` — it covers all of those and more (detekt, lint, architecture). Never let a plan leave out the full validation step.
  - **Avoid** `clean` steps in scripts and CI if possible, relying on Gradle's incremental build and caching for speed.
  - Stale Gradle daemons cause Kotlin version mismatch errors in lint — run `./gradlew --stop` then re-validate.

- **Linter & Architecture Enforcement**:
  - **Prefer automated enforcement over prose rules.** When you discover a rule that should always hold (naming convention, dependency direction, forbidden API usage), first check if an existing linter can enforce it. If not, propose adding a new check. A lint that fails the build is worth more than a paragraph in docs.
  - **Existing enforcement mechanisms** (choose the right one for each rule):

    | Mechanism | Location | Best For | Error Reporting |
    |-----------|----------|----------|-----------------|
    | **Detekt rules** (`detekt.yml`) | Config-only | Kotlin code patterns, naming, complexity, forbidden methods | Built-in detekt format |
    | **Detekt custom rules** (`detekt-rules/`) | `detekt-rules/src/main/kotlin/` | AST-level Compose checks: hardcoded strings in `Text()`, `contentDescription` | Built-in detekt format |
    | **Detekt Compose plugin** | `detekt.yml` Compose section | Compose-specific: modifier naming, parameter order, CompositionLocal allowlist | Built-in detekt format |
    | **Custom Gradle tasks** (`buildSrc/`) | `buildSrc/src/main/kotlin/` | Cross-file rules, dependency graphs, theme layer boundaries, coverage gaps | Custom `GradleException` |
    | **Convention tests** (test source sets) | `*Test.kt` in `jvmTest`/`desktopTest` | Runtime reflection checks: "every X has a Y", API shape enforcement | JUnit assertion messages |
    | **Shell-based analysis** (`scripts/`) | `scripts/*.sh` | Multi-tool pipelines, graph analysis, report generation | Script exit codes + stdout |
    | **Spotless / ktlint** | `build.gradle.kts` | Formatting, import ordering, whitespace | Built-in ktlint format |
    | **Android Lint** | `lint.xml` / Gradle lint config | Android-specific: resource issues, API level compat, accessibility | Built-in Android Lint format |
    | **Claude Code hooks** (`.claude/hooks/`) | `.claude/hooks/*.sh` | Agent-specific guardrails: git safety, workflow enforcement | Hook stdout (warn or block) |

  - **Decision guide — which mechanism to use:**
    1. Can an existing detekt rule cover it? → Enable/configure it in `detekt.yml`
    2. Is it a file-scanning pattern match (regex on source)? → Custom Gradle task (follow `ThemeLayerCheckTask` pattern)
    3. Is it a structural rule about module dependencies? → Extend `ArchitectureCheckTask`
    4. Does it require runtime reflection (class/method existence)? → Convention test (follow `UseCaseConventionTest` pattern)
    5. Is it agent-specific (git safety, workflow)? → Claude Code hook in `.claude/hooks/`

  - **Error message standards** (critical for AI tooling):
    - Every violation message **must** include: file path, line number (when applicable), rule ID, what's wrong, and how to fix it.
    - Format: `file:line [rule-id] Description of violation\n  Fix: Concrete action to take`
    - Bad: `"Architecture violation"` — Good: `"Module ':viewmodel' depends on forbidden module ':data'. Allowed: [:usecase, :domain, :presentation-model]. Move data access into a UseCase."`
    - Bad: `"Test missing"` — Good: `"ViewModel classes missing a corresponding '*ViewModelTest' class:\n  - com.example.FooViewModel\n\nCreate a test file for each ViewModel listed above."`

  - **Adding a new Gradle check** (template):
    1. Create `buildSrc/src/main/kotlin/<package>/<CheckName>CheckTask.kt` extending `DefaultTask`
    2. Create `buildSrc/src/main/kotlin/<package>/<CheckName>Plugin.kt` registering the task in group `"verification"`
    3. Register the plugin ID in `buildSrc/build.gradle.kts` under `gradlePlugin { plugins { ... } }`
    4. Apply the plugin in root `build.gradle.kts`: `id("<plugin-id>")`
    5. Add the task to `scripts/validate.sh` and the appropriate CI job in `.github/workflows/ci.yml`
    6. Follow the `ThemeLayerCheckTask` pattern: define rules as data, scan files, collect violations, throw `GradleException` with actionable messages

  - **Adding a new convention test** (template):
    1. Create `<module>/src/<testSourceSet>/kotlin/.../ConventionTestName.kt`
    2. Use classpath scanning to discover classes matching a pattern (see `UseCaseConventionTest`)
    3. Assert the convention with a clear failure message listing every violator
    4. The test runs automatically with `./gradlew test` — no extra CI config needed

  - **When to propose a new check** (agents should proactively suggest these):
    - A code review catches the same mistake twice → it should be a lint
    - A rule exists only in docs/comments → it should be enforced automatically
    - A PR introduces a new convention (e.g., "all screens must have X") → add a convention test
    - An agent makes a mistake that a linter could have caught → propose adding that linter rule

- **Compose-Specific Detekt Rules** (enforced by `detekt-compose` plugin, checked in `validate.sh`):
  - Modifier parameters in `@Composable` functions **must** be named `modifier` (not `contentModifier`, `shellModifier`, etc.).
  - Parameter order in `@Composable` functions: **non-default params first**, then `modifier: Modifier = Modifier`, then other params with defaults, then trailing lambdas.
  - When extracting or creating new composables that accept a `Modifier`, verify both rules before committing.

- **Exhaustive `when` on Sealed/Enum Types** (`ElseCaseInsteadOfExhaustiveWhen` rule in `detekt.yml`):
  - **Prefer listing all cases explicitly** over `else ->` when the `when` subject is a sealed class, enum, or boolean. This ensures the compiler catches unhandled variants when new subtypes are added.
  - When `else ->` is intentionally used as a catch-all default (e.g., many Screen subtypes mapping to a single default), add `@Suppress("ElseCaseInsteadOfExhaustiveWhen")` with a comment explaining why.
  - **Note**: This rule requires type resolution. It is enforced in CI via the `detektAndroidMain` task (see below) and in IDEs with the detekt plugin.

- **Detekt Type-Resolution Analysis** (`detektAndroidMain` task):
  - CI runs **both** `detekt` (syntax-only) and `detektAndroidMain` (with Kotlin compiler type information). Type resolution enables rules like `NullableToStringCall`, `UnreachableCode`, and `ElseCaseInsteadOfExhaustiveWhen`.
  - Generated code (`build/` directories from Room KSP, Compose resources) is excluded via a file-path filter in `build.gradle.kts`.
  - `NonBooleanPropertyPrefixedWithIs` is disabled in `detekt.yml` — it produces false positives on `Flow<Boolean>` properties like `isAvailable`.
  - Performance overhead is ~1 second at current project scale.

- **CI Workflow Synchronization**:
  - **When changing JDK version**: Update ALL workflow files in `.github/workflows/` that use `setup-java`.
  - **When changing Gradle version**: Verify all workflows use compatible settings.
  - **When changing tooling requirements**: Check `auto-generate.yml` in addition to main CI.
  - **Checklist for tooling changes**:
    1. `ci.yml` - Main CI workflow (includes `check_generated_content` drift check)
    2. `auto-generate.yml` - Post-merge generation of diagrams, analysis, screenshots
    3. Any other workflows in `.github/workflows/`
  - **Generated content architecture**: Workflows NEVER push commits to PR branches. Generated content (diagrams, analysis, screenshots) is updated post-merge on `main` via follow-up PRs created by `auto-generate.yml`.

## Branch Management

**Default: Every task should result in a PR.** When the user asks you to do any work, the default assumption is that you will create a branch and open a PR for it. Each PR should typically be based on `origin/main` (independent PRs). Stacked PRs (one branch based on another) are rarely needed and should only be used when changes genuinely depend on each other.

**Before starting new work, decide if you need a new branch:**
- Check if there's an existing PR for the current branch.
- If new work is unrelated to the current branch/PR, create a new branch from `origin/main`.
- When uncertain, ask the user or default to creating a new branch.

## Core Git Workflow

1.  **Sync with `main`**: Before starting new work, fetch the latest `main` branch.
    ```bash
    git fetch origin main
    ```

2.  **Create a Branch**: Create a new branch from `origin/main`. The branch MUST be prefixed with `agent/`.
    ```bash
    git checkout -b agent/your-branch-name origin/main
    ```

3.  **Implement Changes**: Make all code modifications according to the project's established conventions, as detailed in `project.md`.

4.  **Format Code**: Run Spotless to ensure code style compliance.
    ```bash
    ./scripts/spotless-apply.sh
    ```

5.  **Commit and Push**: Once validation passes, commit the changes with a clear message and push the branch.
    ```bash
    git add .
    git commit -m "feat: Describe the feature or fix"
    git push origin agent/your-branch-name
    ```

    > [!IMPORTANT]
    > **Spotless Verification**: After running `git commit`, check the output. If the post-commit hook reports `spotlessCheck FAILED`, you MUST run `./gradlew spotlessApply`, amend your commit (or create a fix commit), and verify again. Do not ignore this failure.

6.  **Create a Pull Request**: Open a pull request against the `main` branch. Direct pushes to `main` are prohibited. Use the `/create-pr` workflow for standardized title and body formatting.

## After Your PR is Merged

**MANDATORY: Always clean up branches immediately after a PR is merged.** Stale branches accumulate quickly and clutter the repository.

1.  **Switch to the `main` Branch**:
    ```bash
    git checkout main
    ```

2.  **Pull the Latest Changes**: Ensure your local `main` branch is up-to-date.
    ```bash
    git pull origin main
    ```

3.  **Delete the Local Branch**:
    ```bash
    git branch -d agent/your-branch-name
    ```

4.  **Delete the Remote Branch** (if `--delete-branch` was not used during merge):
    ```bash
    git push origin --delete agent/your-branch-name
    ```

5.  **Prune stale remote refs**:
    ```bash
    git fetch --prune origin
    ```

> **Tip:** Use `gh pr merge --squash --delete-branch` to auto-delete the remote branch on merge. You still must delete the local branch manually.

**Example for `AGENT_NAME.md`:**
```markdown
# AGENT_NAME Instructions

This file provides the initial instructions for AGENT_NAME.

**First Action:** Immediately read the main contribution guidelines located at `.agent/AGENTS.md`. This file contains the required workflow and rules for all AI agents in this project. Do not proceed with any other actions until you have read and understood it.
```

## File Index

| File | Contains | Read when |
|------|----------|-----------|
| `AGENTS.md` (this file) | Safety rules, git workflow, core processes | Always (entry point) |
| `project.md` | Architecture, modules, DI, error handling, UI patterns | Always (entry point) |
| `merge-strategy.md` | PR merge workflow, batch merging, integration branches | Merging PRs |
| `testing.md` | Test types, screenshot tests, convention tests, E2E | Writing/running tests |
| `ci.md` | CI modes, path filtering, auto-generate, concurrency | Modifying CI or debugging failures |
| `ios.md` | SwiftUI architecture, design system, xcodebuild, snapshots | iOS work |
| `server.md` | Deployment, URLs, secrets, Terraform, E2E auth | Server work |
| `workflows/` | Step-by-step playbooks (29 files) | Specific operations |
