package com.nodeda.sdk.core

import kotlinx.serialization.Serializable

/**
 * Errors surfaced by the NodeDa SDK. Kotlin analog of Swift's `NodeDaError` enum
 * — implemented as a sealed exception hierarchy so callers can `try { } catch
 * (e: NodeDaError.Api) { … }` directly.
 */
public sealed class NodeDaError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** Request URL could not be constructed (programmer error / bad input). */
    public class InvalidUrl(public val raw: String) :
        NodeDaError("Could not build a valid URL from $raw.")

    /** The underlying OkHttp/network transport threw. */
    public class Transport(cause: Throwable) :
        NodeDaError("Network error: ${cause.message}", cause)

    /** The server returned a non-success status with a structured payload. */
    public class Api(public val error: ApiError) :
        NodeDaError(
            buildString {
                append("[${error.status} ${error.code}]")
                if (!error.message.isNullOrEmpty()) append(" ${error.message}")
            }
        )

    /** JSON decoding of a successful response failed. */
    public class Decoding(cause: Throwable, public val data: ByteArray?) :
        NodeDaError("Failed to decode NodeDa response: ${cause.message}", cause)

    /** The server returned an HTTP status code we don't know how to interpret. */
    public class UnexpectedStatus(public val status: Int, public val data: ByteArray?) :
        NodeDaError("Unexpected HTTP status $status.")

    /**
     * Structured payload returned by every NodeDa Cloud Function on failure.
     * Maps 1:1 to Swift's `NodeDaError.APIError`.
     */
    @Serializable
    public data class ApiError(
        public val status: Int,
        public val code: String,
        public val message: String? = null,
        public val details: Map<String, JsonValue>? = null,
    )
}
