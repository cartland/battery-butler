---
description: How to add, edit, and use string resources in the Compose Multiplatform project and screenshot tests
---

# String Resources Workflow

This project uses a dedicated `:compose-resources` module to manage string resources for Kotlin Multiplatform. This ensures a clean dependency graph where any module (Features, Core, App, Tests) can access strings without circular dependencies.

## 1. Adding or Editing Strings

All string resources are located in the `:compose-resources` module.

**File Path:**
`compose-resources/src/commonMain/composeResources/values/strings.xml`

**Steps:**
1.  Open `strings.xml`.
2.  Add your string: `<string name="my_new_string">Hello World</string>`.
3.  Build the project (or run a Gradle sync) to generate the `Res` class accessor.

## 2. Using Strings in Compose

We use a generated public `Res` class to access strings typesafely.

**Package:** `com.chriscartland.batterybutler.composeresources.generated.resources`

**Usage:**

```kotlin
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.my_new_string
import org.jetbrains.compose.resources.stringResource

@Composable
fun MyScreen() {
    Text(text = stringResource(Res.string.my_new_string))
}
```

## 3. The `AppStrings` Abstraction

To support different environments (Production vs. Screenshot Tests vs. Previews), we use an abstraction layer located in `:compose-resources`.

*   **Interface:** `AppStrings` (defines how strings are resolved).
*   **CompositionLocal:** `LocalAppStrings` (provides the current implementation).
*   **Helper:** `composeStringResource()` (wraps `LocalAppStrings` lookup).

**Preferred Usage in UI Components:**
Instead of directly calling `stringResource(...)`, you should often use the abstraction if you want the component to be easily testable with mock strings in previews.

```kotlin
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.my_new_string

@Composable
fun MyComponent() {
    // This allows swapping string resolution strategies
    val text = composeStringResource(Res.string.my_new_string)
    Text(text)
}
```

## 4. Screenshot Tests & Previews

The screenshot tests (in `:android-screenshot-tests`) and Previews often run in an environment where standard resource loading might be slow or flaky (e.g. LayoutLib).

*   **Production**: Uses `ComposeAppStrings` -> delegates to real `stringResource(Res.string...)`.
*   **Previews/Tests**: Can use `PlaceholderAppStrings` -> returns "Mock: my_string_key".
*   **Screenshot Tests**: Uses `ScreenshotAppStrings`.
    *   This special implementation parses the **real XML file** from the file system (`../compose-resources/.../strings.xml`) to provide actual string values during screenshot generation without needing a full Android Context or Compose Resource runtime.

**If you add a string and it shows as "Mock: ..." in screenshots:**
Ensure your test is using `ScreenshotTestTheme`, which sets up the `ScreenshotAppStrings` provider.
