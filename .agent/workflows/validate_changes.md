---
description: Validate changes locally with CI parity
---

1. Run the validation script defined in the scripts directory
   `bazel run //scripts:validate`

2. If any step fails, check the specific output section (e.g. "iOS Checks").

3. To debug iOS specific issues, try running the xcodebuild command manually with verbose filtering if needed, and ensure `CODE_SIGNING_ALLOWED=NO` is used for simulator builds.