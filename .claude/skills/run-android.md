# Run Android

Build and run the Compose Multiplatform Android app.

## Steps

1. Open the project in Android Studio (if not already open):
   ```bash
   open -a "Android Studio" .
   ```

2. Build and run the Android app:
   ```bash
   ./gradlew :compose-app:installDebug
   ```

3. Or use Android Studio:
   - Select the `composeApp` configuration
   - Click Run (or press Ctrl+R / Cmd+R)
   - Choose an emulator or connected device

## Troubleshooting

- If build fails, try cleaning: `./gradlew clean`
- Ensure you have an Android emulator running or device connected: `adb devices`
