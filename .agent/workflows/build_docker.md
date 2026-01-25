---
description: Build the Docker container locally using Jib
---

// turbo-all

1. Build to local Docker daemon
   `./gradlew :server:app:jibDockerBuild`

2. Verify the image was created
   `docker images | grep battery-butler-server`

3. Run the container locally
   `docker run -p 50051:50051 battery-butler-server:latest`

> [!TIP]
> **Alternative Build Options:**
> - **Tarball:** `./gradlew :server:app:jibBuildTar` (at `server/app/build/jib-image.tar`)
> - **Push to ECR:** `./gradlew :server:app:jib`
