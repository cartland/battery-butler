---
description: Run validation scripts, commit changes, and push to remote
---

1. Prepare for commit
   // turbo
   `./scripts/prepare-for-commit.sh`

2. Resolve any issues
   If the script fails (e.g. lint errors, broken tests), fix them and re-run step 1.

3. Commit changes
   `git add .`
   `git commit -m "Your commit message"`

4. Resolve commit warnings
   If git hooks or other checks warn / fail during commit, address them.

5. Push to remote
   `git push`
