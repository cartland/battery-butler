# Run Server

Run the gRPC server locally.

## Steps

1. Start the server:
   ```bash
   ./gradlew :server:app:run
   ```

2. The server will start listening on port 50051 (default gRPC port).

3. Test the server is running:
   ```bash
   grpcurl -plaintext localhost:50051 list
   ```

## Notes

- The server uses PostgreSQL for persistence (configure connection in server/app/src/main/resources/application.conf)
- For production deployment to AWS, use `/deploy-server` instead
- The server implements the gRPC service definitions from the Wire/proto files
