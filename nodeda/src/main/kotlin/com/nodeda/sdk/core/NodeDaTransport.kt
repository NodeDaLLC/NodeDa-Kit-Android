package com.nodeda.sdk.core

import okhttp3.Request
import okhttp3.Response

/**
 * Minimal interface the SDK uses to perform HTTP requests. Conforming types
 * can be substituted in tests to replay canned responses.
 *
 * Direct analog of Swift's `NodeDaTransport` protocol — uses `suspend` instead
 * of `async throws` and OkHttp's `Request` / `Response` types instead of
 * `URLRequest` / `URLResponse`.
 */
public interface NodeDaTransport {
    public suspend fun send(request: Request): Response
}
