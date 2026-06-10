package com.nrova.sdk.featureflags

import com.nrova.sdk.core.HealthResponse
import com.nrova.sdk.core.HttpClient

/**
 * Client for the **Nrova Feature Flags / Developer API**
 * (`https://us-central1-nrovallc.cloudfunctions.net/developerApi`).
 */
public class FeatureFlagsService internal constructor(
    private val http: HttpClient,
    private val orgId: String,
) {
    private fun base(): String = "v1/organizations/$orgId"

    public suspend fun health(): HealthResponse =
        http.get("health", HealthResponse.serializer(), authenticated = false)

    /** `GET …/flags` — requires the `flags:read` scope. */
    public suspend fun listFlags(): FeatureFlagsResponse =
        http.get("${base()}/flags", FeatureFlagsResponse.serializer())

    /**
     * `POST …/evaluate` — requires the `evaluate` scope. Returns a
     * `Map<flagKey, Boolean>` keyed by the flag key.
     */
    public suspend fun evaluate(request: EvaluateFlagsRequest): EvaluateFlagsResponse =
        http.post(
            path = "${base()}/evaluate",
            bodySerializer = EvaluateFlagsRequest.serializer(),
            body = request,
            deserializer = EvaluateFlagsResponse.serializer(),
        )

    /**
     * Convenience that evaluates a single subject and returns just the
     * boolean result for the requested flag key.
     */
    public suspend fun isEnabled(
        flagKey: String,
        subjectId: String,
        countryCode: String? = null,
    ): Boolean = evaluate(
        EvaluateFlagsRequest(
            subjectId = subjectId,
            countryCode = countryCode,
            flagKeys = listOf(flagKey),
        )
    ).results[flagKey] ?: false
}
