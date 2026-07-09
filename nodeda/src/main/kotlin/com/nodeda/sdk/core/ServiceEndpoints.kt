package com.nodeda.sdk.core

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Base URLs for each NodeDa service client.
 *
 * Production traffic goes through the unified API gateway
 * (`https://api.nodeda.com`). The gateway routes by path prefix
 * (`/v1/organizations/{orgId}/support/`, `/sales/`, …), so every
 * service shares the same host. Per-service fields remain so callers
 * can override individual bases (e.g. staging proxies) without
 * changing the rest of the SDK.
 */
public data class ServiceEndpoints(
    public val distribution: HttpUrl,
    public val support: HttpUrl,
    public val sales: HttpUrl,
    public val careers: HttpUrl,
    public val newsroom: HttpUrl,
    public val developer: HttpUrl,
    public val systemStatus: HttpUrl,
    public val legalPolicies: HttpUrl,
) {
    public companion object {
        /** Unified production API gateway (no trailing slash). */
        public const val UNIFIED_API_BASE: String = "https://api.nodeda.com"

        /** All services pointed at [UNIFIED_API_BASE]. */
        public val production: ServiceEndpoints = of(UNIFIED_API_BASE.toHttpUrl())

        /** Builds endpoints where every service shares [base]. */
        public fun of(base: HttpUrl): ServiceEndpoints = ServiceEndpoints(
            distribution = base,
            support = base,
            sales = base,
            careers = base,
            newsroom = base,
            developer = base,
            systemStatus = base,
            legalPolicies = base,
        )
    }
}
