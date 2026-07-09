package com.nodeda.sdk.core

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * Internal HTTP plumbing shared by every service client. Handles base-URL
 * composition, query-string encoding, Bearer + `X-API-Key` auth headers, JSON
 * encoding/decoding, and the documented error payload shape.
 *
 * Marked `internal` because consumers should never construct this directly —
 * they go through `NodeDaClient`.
 */
internal class HttpClient(
    val baseUrl: HttpUrl,
    val configuration: NodeDaConfiguration,
    val transport: NodeDaTransport,
    val requiresAuth: Boolean = true,
) {
    private val applicationJson = "application/json".toMediaType()

    suspend fun <T> get(
        path: String,
        deserializer: DeserializationStrategy<T>,
        query: Map<String, String?> = emptyMap(),
        authenticated: Boolean = true,
    ): T {
        val request = buildRequest("GET", path, query, body = null, authenticated = authenticated)
        return perform(request, deserializer)
    }

    suspend fun <Body, Response> post(
        path: String,
        bodySerializer: SerializationStrategy<Body>,
        body: Body,
        deserializer: DeserializationStrategy<Response>,
        query: Map<String, String?> = emptyMap(),
        authenticated: Boolean = true,
    ): Response {
        val request = buildRequest(
            method = "POST",
            path = path,
            query = query,
            body = json.encodeToString(bodySerializer, body),
            authenticated = authenticated,
        )
        return perform(request, deserializer)
    }

    suspend fun <Body, Response> patch(
        path: String,
        bodySerializer: SerializationStrategy<Body>,
        body: Body,
        deserializer: DeserializationStrategy<Response>,
        query: Map<String, String?> = emptyMap(),
        authenticated: Boolean = true,
    ): Response {
        val request = buildRequest(
            method = "PATCH",
            path = path,
            query = query,
            body = json.encodeToString(bodySerializer, body),
            authenticated = authenticated,
        )
        return perform(request, deserializer)
    }

    suspend fun <Body, Response> put(
        path: String,
        bodySerializer: SerializationStrategy<Body>,
        body: Body,
        deserializer: DeserializationStrategy<Response>,
        query: Map<String, String?> = emptyMap(),
        authenticated: Boolean = true,
    ): Response {
        val request = buildRequest(
            method = "PUT",
            path = path,
            query = query,
            body = json.encodeToString(bodySerializer, body),
            authenticated = authenticated,
        )
        return perform(request, deserializer)
    }

    suspend fun delete(
        path: String,
        query: Map<String, String?> = emptyMap(),
        authenticated: Boolean = true,
    ) {
        val request = buildRequest("DELETE", path, query, body = null, authenticated = authenticated)
        val response = sendThrowing(request)
        response.use { validate(it) }
    }

    /**
     * Returns the raw response — useful for the 302 endpoints (`download`,
     * `icon`) where we need to inspect the `Location` header. Caller owns the
     * `Response.close()` call.
     */
    suspend fun head(
        path: String,
        query: Map<String, String?> = emptyMap(),
        authenticated: Boolean = true,
    ): Response {
        val request = buildRequest("GET", path, query, body = null, authenticated = authenticated)
        return sendThrowing(request)
    }

    // -----------------------------------------------------------------------
    // Request building
    // -----------------------------------------------------------------------

    private fun buildRequest(
        method: String,
        path: String,
        query: Map<String, String?>,
        body: String?,
        authenticated: Boolean,
    ): Request {
        val urlBuilder = baseUrl.newBuilder()
        path.trim('/').takeIf { it.isNotEmpty() }?.split('/')?.forEach { segment ->
            urlBuilder.addPathSegment(segment)
        }
        query.forEach { (key, value) ->
            if (value != null) urlBuilder.addQueryParameter(key, value)
        }
        val url = urlBuilder.build()

        val builder = Request.Builder().url(url).method(
            method,
            body?.toRequestBody(applicationJson),
        )

        builder.header("Accept", "application/json")
        if (body != null) builder.header("Content-Type", "application/json")

        if (authenticated && requiresAuth && configuration.apiKey.isNotEmpty()) {
            builder.header("Authorization", "Bearer ${configuration.apiKey}")
            builder.header("X-API-Key", configuration.apiKey)
        }
        configuration.defaultHeaders.forEach { (k, v) -> builder.header(k, v) }
        return builder.build()
    }

    // -----------------------------------------------------------------------
    // Response handling
    // -----------------------------------------------------------------------

    private suspend fun sendThrowing(request: Request): Response = try {
        transport.send(request)
    } catch (cancel: kotlinx.coroutines.CancellationException) {
        throw cancel
    } catch (t: Throwable) {
        throw NodeDaError.Transport(t)
    }

    private suspend fun <T> perform(request: Request, deserializer: DeserializationStrategy<T>): T {
        val response = sendThrowing(request)
        return response.use { resp ->
            val bytes = resp.body?.bytes() ?: ByteArray(0)
            validate(resp, bytes)
            decodeBody(bytes, deserializer)
        }
    }

    private fun <T> decodeBody(bytes: ByteArray, deserializer: DeserializationStrategy<T>): T {
        // Swift's `EmptyResponse` short-circuit lives in the service layer
        // (we just pass `Unit.serializer()` when there is no body to read).
        if (deserializer === Unit.serializer()) {
            @Suppress("UNCHECKED_CAST")
            return Unit as T
        }
        val text = bytes.decodeToString()
        return try {
            json.decodeFromString(deserializer, text)
        } catch (t: Throwable) {
            throw NodeDaError.Decoding(t, bytes)
        }
    }

    internal fun validate(response: Response, bytes: ByteArray? = null) {
        if (response.isSuccessful) return
        val payload = bytes ?: runCatching { response.peekBody(64 * 1024).bytes() }.getOrNull()
        val apiError = decodeApiError(response.code, payload)
        if (apiError != null) throw NodeDaError.Api(apiError)
        throw NodeDaError.UnexpectedStatus(response.code, payload)
    }

    private fun decodeApiError(status: Int, data: ByteArray?): NodeDaError.ApiError? {
        if (data == null || data.isEmpty()) return null
        return try {
            val element: JsonElement = json.parseToJsonElement(data.decodeToString())
            val obj = (element as? JsonObject)?.jsonObject ?: return null
            val code = (obj["error"] as? JsonPrimitive)?.contentOrNull
                ?: (obj["code"] as? JsonPrimitive)?.contentOrNull
                ?: "unknown_error"
            val message = (obj["message"] as? JsonPrimitive)?.contentOrNull
            val details = obj["details"]?.let { detailsEl ->
                json.decodeFromJsonElement(
                    deserializer = kotlinx.serialization.builtins.MapSerializer(
                        String.serializer(),
                        JsonValue.serializer(),
                    ),
                    element = detailsEl,
                )
            }
            NodeDaError.ApiError(status = status, code = code, message = message, details = details)
        } catch (_: Throwable) {
            null
        }
    }

    companion object {
        /**
         * Decoder/encoder used by every request. `ignoreUnknownKeys = true`
         * lets the SDK stay forward-compatible when the API adds fields, and
         * `explicitNulls = false` matches Swift's behavior of omitting nil
         * properties from outgoing JSON bodies.
         */
        val json: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = false
            encodeDefaults = false
        }
    }
}
