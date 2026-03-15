# Server Deployment & Configuration

Deployment pipeline, URL management, secrets, and AWS infrastructure.

> **Parent doc:** See `project.md` for architecture and `AGENTS.md` for workflow rules.

## Server Deployment

> **HIBERNATED (Feb 2026):** AWS infrastructure is not running. Server workflows
> are disabled via GitHub (`gh workflow disable`). Server runs locally only. See server/README.md.

Multi-environment deployment pipeline: dev -> staging -> prod. Same Docker image SHA promoted through environments.

**Deployment rules:**
- **All deploys go to dev first.** Never deploy directly to prod.
- **Prod is always a promotion from dev.** Use `./scripts/promote-server.sh` (or `/promote-server`) to promote the dev image to prod. This ensures prod only runs images that have been validated on dev.

**Workflows:**
- `server-build.yml` -- Auto-deploys to dev on push to main (server changes), syncs `DEV_SERVER_URL` secret
- `server-deploy-staging.yml` -- Manual trigger with `image_tag` input
- `server-deploy-prod.yml` -- Manual trigger with approval gate, syncs `PRODUCTION_SERVER_URL` secret
- `server-destroy.yml` -- Tear down staging/dev infrastructure
- `server-rollback.yml` -- Emergency rollback

**Deploy commands:**
```bash
# Check what's deployed right now
./scripts/deploy-status.sh

# Promote to staging
gh workflow run server-deploy-staging.yml -f image_tag=<sha>

# Promote to prod (requires approval)
gh workflow run server-deploy-prod.yml -f image_tag=<sha>

# Test endpoints
grpcurl -plaintext -proto protos/com/chriscartland/batterybutler/protos/battery_service.proto \
  <nlb-dns>:80 com.chriscartland.batterybutler.proto.BatteryService/GetServerStatus

# Run E2E tests against a live environment
E2E_SERVER_URL=http://<nlb-dns>:80 ./scripts/e2e-tests.sh --remote
```

**Deployment observability:**
- `./scripts/deploy-status.sh` -- Shows image tag, status, commit, and drift warnings for each environment
- GitHub commit statuses (`deploy/dev`, `deploy/staging`, `deploy/prod`) -- Annotated on each commit after deploy, visible on GitHub commit pages
- Deploy workflows always check prod vs dev drift (run `deploy-status.sh` at session start)

**Key architecture decisions:**
- ECR is managed outside terraform (data source, not resource) to avoid state lock issues
- Each environment has separate terraform state (`server/{env}/terraform.tfstate`)
- Concurrency groups prevent parallel deploys to same environment
- IAM permissions documented in `server/iam_policy.json` -- update AWS Console manually when changed
- Deploy workflows auto-sync NLB hostname to GitHub secrets (`PRODUCTION_SERVER_URL`, `DEV_SERVER_URL`)
- `release-android.yml` validates server connectivity before Play Store upload

**AWS free-tier limitations:**
- Only `db.t3.micro` RDS instances allowed
- Max 2 RDS instances -- can't run dev + staging + prod simultaneously (staging destroyed to stay within limit)
- Use `server-destroy.yml` to tear down unused environments

## Server URL Management

> **Note:** AWS is hibernated. The URLs in gradle.properties and GitHub secrets
> point to decommissioned endpoints. Only GrpcLocal mode works.

Server URLs (prod and dev) flow through the system as follows:

**Source of truth:** GitHub secrets `PRODUCTION_SERVER_URL` and `DEV_SERVER_URL`, auto-synced from terraform output after each deploy.

**How it propagates:**
1. Terraform creates NLB → deploy workflows capture `nlb_dns_name` → `gh secret set PRODUCTION_SERVER_URL` / `DEV_SERVER_URL`
2. CI workflows set `ORG_GRADLE_PROJECT_PRODUCTION_SERVER_URL` and `ORG_GRADLE_PROJECT_DEV_SERVER_URL` env vars from the secrets
3. Gradle reads them as project properties → `data-network/build.gradle.kts` generates `BuildConfig.kt` with both constants
4. Code accesses via `BuildConfig.PRODUCTION_SERVER_URL` and `BuildConfig.DEV_SERVER_URL`

**DI pattern for modules without data-network dependency:**
- `ProductionServerUrl` and `DevServerUrl` data classes (in `domain/model/`) wrap the URLs for type-safe injection
- `AppComponent` (Android/Desktop) and `NativeComponent` (iOS) provide both from BuildConfig
- ViewModels and other components receive them via constructor injection

**NetworkMode variants:**
- `NetworkMode.GrpcAws(url)` — Prod server
- `NetworkMode.GrpcDev(url)` — Dev server
- `NetworkMode.GrpcLocal(url)` — Local development server
- `NetworkMode.Mock` — Offline mock data
- `NetworkMode.None` — Network disabled (default)

Settings UI displays them in this order: Prod Server / Dev Server / gRPC Local / Mock / None (Offline).

**Key rules:**
- **NEVER hardcode NLB hostnames** in Kotlin source — use `BuildConfig.PRODUCTION_SERVER_URL` / `BuildConfig.DEV_SERVER_URL` or `ProductionServerUrl` / `DevServerUrl`
- `gradle.properties` has fallback values for local dev only; CI always overrides from secrets
- `release-android.yml` validates server connectivity before uploading to Play Store
- When adding a new NetworkMode variant, update all `when` branches (check: DelegatingGrpcClient, DelegatingRemoteDataSource, DynamicDatabaseProvider, DataStoreNetworkModeRepository, SettingsContent, DebugNetworkReceiver, NetworkModeTest)

## Secrets Management

**GitHub Secrets** (write-only — values can't be read back):
- `GEMINI_API_KEY` — Gemini AI API key, written to `local.properties` during Android release builds
- `E2E_TEST_TOKEN` — Pre-seeds synthetic auth session on dev server
- `PRODUCTION_SERVER_URL` — Auto-synced from terraform after each deploy
- `DEV_SERVER_URL` — Auto-synced from terraform after each dev deploy
- `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` — Android signing
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` — Play Store upload

## Terraform Operations

- **Always** use `-var-file` when running terraform commands — omitting it causes terraform to hang waiting for input, holding the state lock until timeout.
- DynamoDB lock key format: `{bucket-name}/{state-key}` (full S3 path). Used for manual lock removal.
- `terraform state show` acquires locks and may not release cleanly on failure.
- Each environment has separate terraform state: `server/{env}/terraform.tfstate`.
- Environment configs: `server/terraform/environments/{dev,staging,prod}.tfvars` — all use `db.t3.micro` for free-tier.

**Useful terraform commands:**
```bash
# Emergency unlock — find lock key format: {bucket}/{state-key}
aws dynamodb delete-item --table-name <lock-table> \
  --key '{"LockID":{"S":"<bucket>/<state-key>"}}'

# Get dev NLB URL
aws elbv2 describe-load-balancers --region us-west-1 \
  --query 'LoadBalancers[?contains(LoadBalancerName, `dev`)].DNSName' --output text
```

**Local secrets** in `local.properties` (gitignored) — can get overwritten by IDE/Gradle:
- Back up important keys to macOS Keychain:
  ```bash
  # Save
  security add-generic-password -a "KEY_NAME" -s "battery-butler" -w "value" -U
  # Retrieve
  security find-generic-password -a "KEY_NAME" -s "battery-butler" -w
  ```
- Currently stored in Keychain: `GEMINI_API_KEY`
