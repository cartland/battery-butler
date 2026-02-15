package com.chriscartland.batterybutler.server.app

import co.touchlab.kermit.Logger
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status

/**
 * gRPC server interceptor that validates session tokens on incoming requests.
 *
 * Exempts unauthenticated RPCs: GetServerStatus and VerifyToken.
 * For authenticated RPCs, reads the `authorization` metadata header,
 * validates the session via [AuthService], and puts user info into
 * the gRPC [Context].
 */
class AuthInterceptor(
    private val authService: AuthService,
) : ServerInterceptor {
    private val log = Logger.withTag("AuthInterceptor")

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val methodName = call.methodDescriptor.bareMethodName ?: ""

        // Exempt public RPCs from auth
        if (methodName in PUBLIC_METHODS) {
            return next.startCall(call, headers)
        }

        val token = headers.get(AUTHORIZATION_KEY)
        if (token.isNullOrBlank()) {
            log.w { "Missing authorization header for $methodName" }
            call.close(Status.UNAUTHENTICATED.withDescription("Missing authorization header"), Metadata())
            return NoOpListener()
        }

        // Strip "Bearer " prefix if present
        val sessionToken = if (token.startsWith("Bearer ", ignoreCase = true)) {
            token.substring(7)
        } else {
            token
        }

        val session = authService.validateSession(sessionToken)
        if (session == null) {
            log.w { "Invalid or expired session token for $methodName" }
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid or expired session token"), Metadata())
            return NoOpListener()
        }

        // Put user info into gRPC Context so services can access it
        val ctx = Context
            .current()
            .withValue(USER_ID_KEY, session.userId)
            .withValue(USER_EMAIL_KEY, session.email)

        return Contexts.interceptCall(ctx, call, headers, next)
    }

    companion object {
        val AUTHORIZATION_KEY: Metadata.Key<String> =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

        val USER_ID_KEY: Context.Key<String> = Context.key("user-id")
        val USER_EMAIL_KEY: Context.Key<String> = Context.key("user-email")

        private val PUBLIC_METHODS = setOf(
            "GetServerStatus",
            "VerifyToken",
        )
    }
}

/** No-op listener returned when the call is rejected before reaching the handler. */
private class NoOpListener<T> : ServerCall.Listener<T>()
