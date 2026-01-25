# Build Docker

Build the Docker container locally using Jib.

## Steps

1. Build to local Docker daemon:
   ```bash
   ./gradlew :server:app:jibDockerBuild
   ```

2. Verify the image was created:
   ```bash
   docker images | grep battery-butler-server
   ```

3. Run the container locally:
   ```bash
   docker run -p 50051:50051 battery-butler-server:latest
   ```

## Alternative Build Options

**Build to tarball (no Docker daemon required):**
```bash
./gradlew :server:app:jibBuildTar
```
The tarball will be at: `server/app/build/jib-image.tar`

**Build and push to ECR:**
```bash
./gradlew :server:app:jib
```

## Notes

- Jib builds optimized containers without requiring a Docker daemon
- The image is configured in `server/app/build.gradle.kts`
- Base image: Eclipse Temurin JRE 17
- Exposed port: 50051 (gRPC)
