package com.nodeda.sdk.core

import kotlinx.serialization.Serializable

/** Response returned by every `GET /health` endpoint across NodeDa services. */
@Serializable
public data class HealthResponse(
    public val ok: Boolean,
    public val service: String? = null,
)
