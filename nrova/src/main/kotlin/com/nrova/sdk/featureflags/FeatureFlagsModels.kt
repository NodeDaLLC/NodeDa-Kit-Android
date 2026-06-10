package com.nrova.sdk.featureflags

import kotlinx.serialization.Serializable

@Serializable
public data class FeatureFlag(
    public val id: String,
    public val key: String,
    public val name: String? = null,
    public val description: String? = null,
    public val enabled: Boolean,
    public val rolloutPercent: Double? = null,
    public val countryMode: String? = null,
    public val countryCodes: List<String>? = null,
    public val status: String? = null,
    public val startsAt: String? = null,
    public val endsAt: String? = null,
)

@Serializable
public data class FeatureFlagsResponse(
    public val orgId: String,
    public val generatedAt: String? = null,
    public val flags: List<FeatureFlag>,
)

@Serializable
public data class EvaluateFlagsRequest(
    public val subjectId: String,
    public val countryCode: String? = null,
    public val flagKeys: List<String>? = null,
)

@Serializable
public data class EvaluateFlagsResponse(
    public val orgId: String,
    public val subjectId: String,
    public val countryCode: String? = null,
    public val evaluatedAt: String? = null,
    public val results: Map<String, Boolean>,
)
