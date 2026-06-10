package com.nrova.sdk.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Default [NrovaTransport] backed by OkHttp. Mirrors Swift's
 * `extension URLSession: NrovaTransport` — the SDK's batteries-included path
 * that consumers never need to touch.
 *
 * Construct your own `OkHttpClient` and wrap it with [OkHttpTransport] when
 * you need an interceptor, a custom DNS, a proxy, or pinned certificates.
 */
public class OkHttpTransport(
    private val client: OkHttpClient = defaultClient,
) : NrovaTransport {

    override suspend fun send(request: Request): Response = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { runCatching { call.cancel() } }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }
            })
        }
    }

    public companion object {
        public val defaultClient: OkHttpClient by lazy { OkHttpClient() }
    }
}
