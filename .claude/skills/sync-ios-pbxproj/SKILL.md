---
description: Synchronize iOS SwiftUI features with the Xcode project
---

# Sync iOS PBXPROJ
When you add, delete, or rename Swift files in the `ios-app-swift-ui/Features/` directory, the Xcode compiler will NOT recognize these changes until the `project.pbxproj` file is updated.

Instead of trying to manually hack the complex `project.pbxproj` XML/Plist format or parsing it with `sed` or `awk`, you MUST use the provided Ruby script to automatically parse the `Features` directory and inject/remove the file references cleanly.

### Pre-requisites
You must have the Ruby `xcodeproj` gem installed. It should already be available in the environment, but if it fails, run `gem install xcodeproj`.

### Execution
1. Navigate to the iOS directory:
   ```bash
   cd ios-app-swift-ui
   ```
2. Run the synchronization script:
   ```bash
   ruby sync_pbxproj.rb
   ```
3. Commit the resulting `project.pbxproj` modifications along with your Swift file changes!
