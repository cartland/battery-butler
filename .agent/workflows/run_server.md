---
description: Run the gRPC server locally
---

// turbo-all

1. Start the server
   `./gradlew :server:app:run`

   > [!NOTE]
   > The server will start listening on port 50051.

2. Test the server is running (optional)
   `grpcurl -plaintext localhost:50051 list`
