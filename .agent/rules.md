# Project Rules and Best Practices

## Self Improvements
- **Always** update this file when adding new rules or best practices for the project.

## iOS Builds
- **Always** use `-derivedDataPath build` when running `xcodebuild` in scripts or CI to ensure artifacts are output to a predictable location (`./build`) instead of the default system DerivedData path.
- **Always** use `-target` instead of `-scheme` if the scheme file is not shared (checked into git).
- **Always** disable code signing for local simulator builds or CI builds without certificates using `CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO`.

## Bazel
- **Bazel Outputs:** All Bazel outputs are consolidated in `.bazel/` (e.g. `.bazel/bin`) via `.bazelrc`. This directory is gitignored and excluded from Spotless.
- **Spotless vs Bazel:** Spotless exclusions are configured to ignore `.bazel/`. Running `spotlessApply` works safely even with Bazel symlinks present.

## Validation
- **Always** run `./scripts/validate.sh` before pushing to main. This script is maintained to match `ci.yml` strictly.
- **Avoid** `clean` steps in scripts and CI if possible, relying on Gradle's incremental build and caching for speed.
