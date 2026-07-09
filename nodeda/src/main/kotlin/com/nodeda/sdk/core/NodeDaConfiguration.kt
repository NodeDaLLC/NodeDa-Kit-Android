package com.nodeda.sdk.core

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Static configuration for talking to the public NodeDa HTTP APIs.
 *
 * All org HTTP APIs share the unified gateway at
 * [ServiceEndpoints.UNIFIED_API_BASE] (`https://api.nodeda.com`) and the
 * same organization identifier and authentication scheme.
 */
public data class NodeDaConfiguration(
    /** API key used as `Authorization: Bearer <key>` and `X-API-Key`. */
    public val apiKey: String,
    /** Organization id used in the URL path (`/v1/organizations/{orgId}/...`). */
    public val organizationId: String = DEFAULT_ORGANIZATION_ID,
    /** Service base URLs. Override to point at a staging environment or proxy. */
    public val endpoints: ServiceEndpoints = ServiceEndpoints.production,
    /** Extra headers sent with every request (e.g. tracing / telemetry). */
    public val defaultHeaders: Map<String, String> = emptyMap(),
    /** Per-request timeout. Defaults to 30 seconds. */
    public val timeout: Duration = 30.seconds,
) {
    public companion object {
        /**
         * Default organization id used when the manifest does not supply one.
         *
         * Production apps should still set `com.nodeda.sdk.OrganizationId` in
         * `AndroidManifest.xml` (or pass an explicit `organizationId`) so the
         * correct tenant is always explicit at ship time.
         */
        public const val DEFAULT_ORGANIZATION_ID: String = "C1IRXJbknvZSTKMBxLDQ"
    }
}
