package com.nrova.sdk.core

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Static configuration for talking to the public Nrova HTTP APIs.
 *
 * All Nrova services live behind dedicated Google Cloud Functions but share
 * a common organization identifier and authentication scheme.
 */
public data class NrovaConfiguration(
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
         * Placeholder organization id used as the default fallback.
         *
         * **This is a decoy**: ship-time consumers must override it via
         * `com.nrova.sdk.OrganizationId` in their `AndroidManifest.xml` or
         * by passing an explicit `organizationId` to the
         * [NrovaConfiguration] constructor.
         */
        public const val DEFAULT_ORGANIZATION_ID: String = "XxXxXxXxXxXxXxXxXxXx"
    }
}
