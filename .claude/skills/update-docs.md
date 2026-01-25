# Update Docs

Update all generated documentation (architecture diagrams and code share analysis).

## Steps

1. Generate architecture diagrams (Mermaid):
   ```bash
   ./gradlew generateMermaidGraph
   ```

   This updates:
   - `docs/diagrams/kotlin_module_structure.mmd`
   - `docs/diagrams/kotlin_module_structure.svg`
   - `docs/diagrams/full_system_structure.mmd`
   - `docs/diagrams/full_system_structure.svg`

2. Generate code share analysis report:
   ```bash
   ./gradlew analyzeCodeShare
   ```

   This updates:
   - `docs/Code_Share_Analysis.md`

3. Review and commit changes:
   ```bash
   git add docs/
   git commit -m "docs: Update diagrams and code analysis"
   ```

## Automation

- Architecture diagrams are automatically updated by post-commit hooks
- GitHub Actions will auto-update diagrams on main branch pushes (see `.github/workflows/update-diagrams.yml`)
- If diagrams are updated during commit, you'll be prompted to amend

## Notes

- Diagrams reflect the current module dependency structure
- Code share analysis shows breakdown of code by module and platform
- Both tasks are idempotent - safe to run multiple times
