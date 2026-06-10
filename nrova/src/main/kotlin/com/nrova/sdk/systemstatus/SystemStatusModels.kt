package com.nrova.sdk.systemstatus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Rollup status returned by the system status API. */
@Serializable
public enum class SystemStatusLevel(public val wire: String) {
    @SerialName("operational") OPERATIONAL("operational"),
    @SerialName("degraded") DEGRADED("degraded"),
    @SerialName("partial_outage") PARTIAL_OUTAGE("partial_outage"),
    @SerialName("major_outage") MAJOR_OUTAGE("major_outage"),
    @SerialName("maintenance") MAINTENANCE("maintenance"),
    @SerialName("unknown") UNKNOWN("unknown");
}

@Serializable
public data class SystemStatusComponent(
    public val id: String,
    public val key: String? = null,
    public val name: String? = null,
    public val description: String? = null,
    public val status: SystemStatusLevel? = null,
    public val sortOrder: Int? = null,
    public val updatedAt: String? = null,
)

@Serializable
public data class SystemStatusRollup(
    public val status: SystemStatusLevel? = null,
    public val updatedAt: String? = null,
    public val components: List<SystemStatusComponent>,
)

@Serializable
public data class SystemStatusComponentResponse(public val component: SystemStatusComponent)

@Serializable
public data class CreateStatusComponentRequest(
    public val key: String,
    public val name: String,
    public val description: String? = null,
    public val status: SystemStatusLevel? = null,
    public val sortOrder: Int? = null,
)

@Serializable
public data class UpdateStatusComponentRequest(
    public val name: String? = null,
    public val description: String? = null,
    public val status: SystemStatusLevel? = null,
    public val sortOrder: Int? = null,
)
