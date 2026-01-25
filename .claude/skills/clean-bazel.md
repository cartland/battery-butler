# Clean Bazel

Clean up Bazel generated artifacts and symlinks.

## Steps

1. List any tracked bazel files to verify if they were accidentally committed:
   ```bash
   git ls-files "bazel-*"
   ```

2. Remove them from git tracking (but keep on disk):
   ```bash
   git rm --cached bazel-*
   ```

3. Verify .bazel/ is in .gitignore:
   ```bash
   grep -q "\.bazel/" .gitignore || echo ".bazel/" >> .gitignore
   ```

4. Commit the cleanup:
   ```bash
   git commit -m "Stop tracking generated bazel symlinks"
   ```
