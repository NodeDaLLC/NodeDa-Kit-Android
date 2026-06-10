package com.nrova.sdk

import com.nrova.sdk.core.NrovaTransport
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Test transport that returns canned responses. Each invocation passes the
 * outgoing `Request` to the supplied `responder` lambda so tests can assert
 * on URL composition, headers, and request bodies.
 *
 * Direct analog of Swift's `MockTransport`.
 */
internal class MockTransport(
    private val responder: (Request) -> Pair<ByteArray, Int> =
        { req -> "{}".toByteArray() to 200 },
) : NrovaTransport {

    override suspend fun send(request: Request): Response {
        val (bytes, status) = responder(request)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message(if (status in 200..299) "OK" else "Error")
            .body(bytes.toResponseBody("application/json".toMediaType()))
            .build()
    }

    companion object {
        fun json(body: String, status: Int = 200): (Request) -> Pair<ByteArray, Int> =
            { _ -> body.toByteArray() to status }
    }
}
