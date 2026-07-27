# Contributing to Battery Butler

## Getting Started
1. Clone the repository.
2. Run `./gradlew build` to verify environment.

## Code Style
- We use Spotless for formatting. Run `./gradlew spotlessApply` before committing.
- We use Detekt for linting. Run `./gradlew detekt` to check for issues.

## Testing
- Run `./gradlew check` to run all unit tests.
- See `docs/TESTING.md` for detailed testing strategy.

## Architecture
- Follow Clean Architecture principles (Domain, Data, Presentation).
- See `docs/diagrams/kotlin_module_structure.mmd` for module graph.

## Pull Requests
- Create a new branch for your feature or fix.
- Ensure all checks pass (`./scripts/validate.sh`).
- Provide a clear description of changes.

## Shared Client/Server API

The client is open source (this repo); the production server is closed source
and lives in a separate private repository. We design the API they share **in
the open, as documents**, under [`docs/api-proposals/`](docs/api-proposals/README.md).
To propose a new shared API, copy the template to a numbered proposal and open a
PR. See [ADR-005](docs/architecture/adr-005-public-api-coordination.md) for the
rationale.

### Contribution boundary

To keep the server's secrets safe while collaborating on the public API, who may
edit what depends on one question: **does your session have access to the private
server source code?**

- **With** private-server access → edit **documentation only** (proposals and
  decisions), never code in this repo. This keeps the leak-audit surface small
  and text-only.
- **Without** private-server access → edit **code** (and docs) freely; a session
  that never saw the server's secrets can't leak them.

The test is access, not job title. Server-side reviewers run a **secret-safety
review** before any proposal merges — see the
[`docs/api-proposals/` README](docs/api-proposals/README.md).
