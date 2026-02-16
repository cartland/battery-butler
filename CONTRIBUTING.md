# Contributing to Battery Butler

## Getting Started
1. Clone the repository.
2. Run `./gradlew build` to verify environment.

## Code Style
- We use Spotless for formatting. Run `./gradlew spotlessApply` before committing.
- We use Detekt for linting. Run `./gradlew detekt` to check for issues.

## Testing
- Run `./gradlew check` to run all unit tests.
- See `TESTING_PLAN.md` for detailed testing strategy.

## Architecture
- Follow Clean Architecture principles (Domain, Data, Presentation).
- See `docs/diagrams/kotlin_module_structure.mmd` for module graph.

## Pull Requests
- Create a new branch for your feature or fix.
- Ensure all checks pass (`./scripts/validate.sh`).
- Provide a clear description of changes.
