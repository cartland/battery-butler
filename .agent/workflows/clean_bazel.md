---
description: Clean up Bazel generated artifacts and symlinks
---

// turbo-all

1. List any tracked bazel files to verify if they were accidentally committed
   `git ls-files "bazel-*"`

2. Remove them from git tracking (but keep on disk)
   `git rm --cached bazel-*`

3. Verify .bazel/ is in .gitignore
   `grep -q "\.bazel/" .gitignore || echo ".bazel/" >> .gitignore`

4. Commit the cleanup
   `git commit -m "Stop tracking generated bazel symlinks"`
