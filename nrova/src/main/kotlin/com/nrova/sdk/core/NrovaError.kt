package com.nrova.sdk.core

import kotlinx.serialization.Serializable

/**
 * Errors surfaced by the Nrova SDK. Kotlin analog of Swift's `NrovaError` enum
 * — implemented as a sealed exception hierarchy so callers can `try { } catch
 * (e: NrovaError.Api) { … }` directly.
 */
public sealed class NrovaError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** Request URL could not be constructed (programmer error / bad input). */
    public class InvalidUrl(public val raw: String) :
        NrovaError("Could not build a valid URL from $raw.")

    /** The underlying OkHttp/network transport threw. */
    public class Transport(cause: Throwable) :
        NrovaError("Network error: ${cause.message}", cause)

    /** The server returned a non-success status with a structured payload. */
    public class Api(public val error: ApiError) :
        NrovaError(
            buildString {
                append("[${error.status} ${error.code}]")
                if (!error.message.isNullOrEmpty()) append(" ${error.message}")
            }
        )

    /** JSON decoding of a successful response failed. */
    public class Decoding(cause: Throwable, public val data: ByteArray?) :
        NrovaError("Failed to decode Nrova response: ${cause.message}", cause)

    /** The server returned an HTTP status code we don't know how to interpret. */
    public class UnexpectedStatus(public val status: Int, public val data: ByteArray?) :
        NrovaError("Unexpected HTTP status $status.")

    /**
     * Structured payload returned by every Nrova Cloud Function on failure.
     * Maps 1:1 to Swift's `NrovaError.APIError`.
     */
    @Serializable
    public data class ApiError(
        public val status: Int,
        public val code: String,
        public val message: String? = null,
        public val details: Map<String, JsonValue>? = null,
    )
}
