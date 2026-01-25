# Google Play Publishing Setup

This document explains how to configure GitHub Actions to automatically publish
the Android app to Google Play's internal test track when a tag matching
`android/N` is pushed.

## How It Works

When you push a tag like `android/123`:
1. GitHub Actions validates the tag format
2. Builds a signed release bundle (AAB)
3. Uploads to Google Play internal test track

The version code is extracted from the tag (e.g., `android/123` → versionCode = 123).

## Prerequisites

1. A Google Play Developer account
2. The app must be manually uploaded to Google Play at least once
3. A signing keystore for your app

## Required GitHub Secrets

Configure these secrets in your repository settings
(Settings → Secrets and variables → Actions):

### 1. `KEYSTORE_BASE64`

Your Android signing keystore encoded as Base64.

**To create and encode a keystore:**

```bash
# Create a new keystore (if you don't have one)
keytool -genkey -v -keystore release.keystore -alias battery-butler \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass YOUR_STORE_PASSWORD -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Battery Butler, O=Chris Cartland"

# Encode the keystore to Base64
base64 -i release.keystore | tr -d '\n'
```

Copy the Base64 output and save it as the `KEYSTORE_BASE64` secret.

**Important:** Store your original keystore file securely. If you lose it, you
cannot update your app on Google Play.

### 2. `KEYSTORE_PASSWORD`

The password used when creating the keystore (`-storepass` value).

### 3. `KEY_ALIAS`

The alias of the signing key (`-alias` value). Example: `battery-butler`

### 4. `KEY_PASSWORD`

The password for the key (`-keypass` value). Often the same as `KEYSTORE_PASSWORD`.

### 5. `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`

A Google Play service account JSON key for API access.

**To create the service account:**

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the **Google Play Android Developer API**:
   - Go to APIs & Services → Library
   - Search for "Google Play Android Developer API"
   - Click Enable

4. Create a service account:
   - Go to APIs & Services → Credentials
   - Click "Create Credentials" → "Service Account"
   - Name: `play-store-publisher`
   - Click "Create and Continue"
   - Skip role assignment (we'll grant permissions in Play Console)
   - Click "Done"

5. Create a JSON key:
   - Click on the service account you just created
   - Go to "Keys" tab
   - Click "Add Key" → "Create new key"
   - Select "JSON"
   - Click "Create"
   - Save the downloaded JSON file

6. Grant access in Google Play Console:
   - Go to [Google Play Console](https://play.google.com/console/)
   - Click "Users and permissions" in the left menu
   - Click "Invite new users"
   - Enter the service account email (from the JSON file: `client_email`)
   - Under "App permissions", select your app
   - Grant these permissions:
     - **Release to production, exclude devices, and use Play App Signing**
     - **Release apps to testing tracks**
     - **Manage testing track configuration**
   - Click "Invite user"

7. Copy the entire JSON file contents and save as the
   `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret.

**Example JSON structure (DO NOT share your actual file):**
```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "abc123...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "play-store-publisher@your-project.iam.gserviceaccount.com",
  "client_id": "123456789",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/..."
}
```

## Publishing a Release

Once secrets are configured, publish using one of these methods:

### Option 1: Push a tag (recommended)

```bash
# Check current version codes in Play Console first!
# Version code must be higher than any previously uploaded version

git tag android/123
git push origin android/123
```

### Option 2: Manual retry (for failed runs)

If a tag-triggered run fails transiently, retry it:

1. Go to Actions → "Publish Android to Play Store"
2. Click "Run workflow"
3. In the branch/tag dropdown, select the existing `android/N` tag
4. Click "Run workflow"

Note: You must select an existing `android/N` tag. Running on a branch will fail.

The action will:
1. Build the app with the specified versionCode (versionName is defined in `compose-app/build.gradle.kts`)
2. Sign with your release keystore
3. Upload to internal test track with release notes from `distribution/whatsnew/`

## Release Notes

Edit the release notes before publishing:

```
distribution/whatsnew/en-US
```

The file contains plain text that appears in Google Play. You can add additional
language files (e.g., `es-ES`, `fr-FR`) for localized release notes.

## Troubleshooting

### "Package not found" error
- Ensure you've manually uploaded the app to Google Play at least once
- Verify the `packageName` in the workflow matches your `applicationId`

### "Invalid grant" or authentication errors
- Verify the service account email has been added to Play Console
- Ensure the Google Play Android Developer API is enabled
- Check that permissions include release access

### "Version code already used" error
- Each versionCode must be unique and higher than previous versions
- Check Play Console for the latest uploaded version code
- Use a higher number in your tag

### Signing errors
- Verify KEYSTORE_BASE64 was encoded correctly (no newlines)
- Ensure passwords don't contain special characters that need escaping
- Double-check the KEY_ALIAS matches what's in your keystore

### Build failures
- Check that the release build works locally first:
  ```bash
  ./gradlew :compose-app:bundleRelease
  ```

## Security Notes

- Never commit keystores or service account JSON files to the repository
- Rotate credentials if they're ever exposed
- Use GitHub's secret scanning to detect accidental credential commits
- Consider using Google Play App Signing for additional security

## Version Code Strategy

Google Play requires each upload to have a unique, incrementing version code.
The tag format `android/N` maps directly to versionCode, making it easy to track:

- `android/1` → versionCode 1 (first upload)
- `android/2` → versionCode 2 (second upload)
- etc.

### Safety Limit

The workflow enforces a safety limit of **1,000,000** by default to prevent
accidentally exhausting version codes. If you exceed this limit, the workflow
will fail with an error explaining how to increase it.

**To change the safety limit:**

Edit `VERSION_CODE_SAFETY_LIMIT` at the top of `.github/workflows/publish-android.yml`:

```yaml
env:
  VERSION_CODE_SAFETY_LIMIT: 1000000  # Change this value
```

**Warning:** Google Play has a hard limit of 2,100,000,000. Once you reach it,
you cannot upload new versions of your app. The safety limit exists to catch
mistakes (like accidentally tagging `android/1000000` instead of `android/10`).

### Version Name

The version name (e.g., "1.0.0") is defined in source code at
`compose-app/build.gradle.kts` and is independent of the version code. Update
it there when you want to change the user-visible version string.
