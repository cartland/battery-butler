# Release Android

Release the Android app to Play Store internal testing by creating a release tag.

> [!CAUTION]
> **Agent Rule:** NEVER create tags or push them without explicit user permission.
> Tags trigger production releases and cannot be easily undone.
> Always confirm the release details and get user approval before proceeding.

## Prerequisites

- On `main` branch with a clean working tree
- All CI checks passing on the commit being released
- Keystore and Play Store credentials configured in GitHub secrets

## Steps

1. Check current state:
   ```bash
   git fetch origin --tags
   git tag -l 'android/*' --format='%(refname:short) %(objectname:short) %(creatordate:short)' | sort -t/ -k2 -n | tail -5
   ```

2. Run the release script with non-interactive flags:
   ```bash
   ./scripts/release-android.sh --allow-duplicate-tag --confirm-release
   ```

   The script will:
   - Find the highest existing `android/N` tag
   - Create `android/N+1` on the current commit
   - Push the tag to trigger the `release-android.yml` workflow

3. Monitor the release workflow:
   ```bash
   gh run list --workflow=release-android.yml --limit 1
   ```

## What the Workflow Does

The `release-android.yml` workflow (triggered by `android/*` tags):
1. Builds the release APK/AAB with signing
2. Validates server connectivity (`PRODUCTION_SERVER_URL`)
3. Uploads to Play Store internal testing track
4. Creates a GitHub Release with the APK attached

## Notes

- Tags are sequential integers: `android/1`, `android/2`, `android/3`, etc.
- The script refuses to run with uncommitted changes
- If the commit already has an `android/*` tag, the script warns before creating a duplicate
- Release notes for Play Store are generated separately via `/generate-mobile-release-notes`
