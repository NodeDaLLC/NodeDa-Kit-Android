package com.nodeda.sdk.distribution

import com.nodeda.sdk.core.HealthResponse
import com.nodeda.sdk.core.HttpClient
import com.nodeda.sdk.core.NodeDaError

/**
 * Client for the **NodeDa Distribution API**
 * (`https://api.nodeda.com` — path prefix `/v1/organizations/{orgId}/applications/`).
 *
 * Endpoints covered:
 * * `GET /health`
 * * `GET /v1/organizations/{orgId}/applications/public` (no auth)
 * * `GET /v1/organizations/{orgId}/applications`
 * * `GET /v1/organizations/{orgId}/applications/{appId}`
 * * `GET /v1/organizations/{orgId}/applications/{appId}/releases`
 * * `GET /v1/organizations/{orgId}/applications/{appId}/releases/{releaseId}`
 * * `GET /v1/organizations/{orgId}/applications/{appId}/latest`
 * * `GET /v1/organizations/{orgId}/applications/{appId}/download` (302)
 * * `GET /v1/organizations/{orgId}/applications/{appId}/icon` (302 or JSON)
 * * `POST /v1/organizations/{orgId}/applications/{appId}/releases`
 * * `PATCH /v1/organizations/{orgId}/applications/{appId}/releases/{releaseId}`
 */
public class DistributionService internal constructor(
    private val http: HttpClient,
    private val orgId: String,
) {
    private fun base(): String = "v1/organizations/$orgId/applications"

    // -------------------------------------------------------------------
    // Health
    // -------------------------------------------------------------------

    /** `GET /health` — does not require an API key. */
    public suspend fun health(): HealthResponse =
        http.get("health", HealthResponse.serializer(), authenticated = false)

    // -------------------------------------------------------------------
    // Listing
    // -------------------------------------------------------------------

    /** Unauthenticated public app feed (`GET …/applications/public`). */
    public suspend fun listPublicApplications(): DistributionApplicationsResponse =
        http.get(
            path = "${base()}/public",
            deserializer = DistributionApplicationsResponse.serializer(),
            authenticated = false,
        )

    /** `GET …/applications` — requires `distribution:read`. */
    public suspend fun listApplications(): DistributionApplicationsResponse =
        http.get(base(), DistributionApplicationsResponse.serializer())

    /** `GET …/applications/{appId}` — requires `distribution:read`. */
    public suspend fun getApplication(appId: String): DistributionApplication =
        http.get("${base()}/$appId", DistributionApplicationResponse.serializer()).application

    // -------------------------------------------------------------------
    // Releases
    // -------------------------------------------------------------------

    /** `GET …/applications/{appId}/releases` — optional channel/platform/limit filters. */
    public suspend fun listReleases(
        appId: String,
        channel: DistributionChannel? = null,
        platform: DistributionPlatform? = null,
        limit: Int? = null,
    ): List<DistributionRelease> {
        val envelope = http.get(
            path = "${base()}/$appId/releases",
            deserializer = DistributionReleasesResponse.serializer(),
            query = mapOf(
                "channel" to channel?.wire,
                "platform" to platform?.wire,
                "limit" to limit?.toString(),
            ),
        )
        return envelope.releases
    }

    /** `GET …/applications/{appId}/releases/{releaseId}`. */
    public suspend fun getRelease(appId: String, releaseId: String): DistributionRelease =
        http.get(
            path = "${base()}/$appId/releases/$releaseId",
            deserializer = DistributionReleaseResponse.serializer(),
        ).release

    /**
     * `GET …/applications/{appId}/latest` — resolves the latest release for
     * the requested `platform` + `channel`, optionally narrowing to `install`
     * or `update` artifacts.
     */
    public suspend fun latest(
        appId: String,
        platform: DistributionPlatform,
        channel: DistributionChannel = DistributionChannel.STABLE,
        purpose: DistributionArtifactPurpose? = null,
    ): DistributionLatestResponse =
        http.get(
            path = "${base()}/$appId/latest",
            deserializer = DistributionLatestResponse.serializer(),
            query = mapOf(
                "platform" to platform.wire,
                "channel" to channel.wire,
                "purpose" to purpose?.wire,
            ),
        )

    // -------------------------------------------------------------------
    // Convenience 302 helpers
    // -------------------------------------------------------------------

    /**
     * Resolves the final download URL for the `GET …/download` endpoint by
     * reading the `Location` header from its 302 response. Does **not**
     * actually fetch the binary bytes — that's left to the caller.
     */
    public suspend fun resolveDownloadUrl(
        appId: String,
        platform: DistributionPlatform,
        channel: DistributionChannel = DistributionChannel.STABLE,
        purpose: DistributionArtifactPurpose = DistributionArtifactPurpose.INSTALL,
    ): String {
        http.head(
            path = "${base()}/$appId/download",
            query = mapOf(
                "platform" to platform.wire,
                "channel" to channel.wire,
                "purpose" to purpose.wire,
            ),
        ).use { resp ->
            val location = resp.header("Location")
            if (location.isNullOrEmpty()) {
                throw NodeDaError.UnexpectedStatus(resp.code, null)
            }
            return location
        }
    }

    public enum class IconFormat { JSON, REDIRECT }

    /**
     * Returns the public icon URL for an app.
     *
     * - [IconFormat.JSON] (default) returns the typed JSON payload.
     * - [IconFormat.REDIRECT] follows the underlying 302 and returns the
     *   resolved URL.
     */
    public suspend fun icon(
        appId: String,
        format: IconFormat = IconFormat.JSON,
    ): DistributionIconResponse = when (format) {
        IconFormat.JSON -> http.get(
            path = "${base()}/$appId/icon",
            deserializer = DistributionIconResponse.serializer(),
            query = mapOf("format" to "json"),
        )
        IconFormat.REDIRECT -> http.head("${base()}/$appId/icon").use { resp ->
            val location = resp.header("Location")
                ?: throw NodeDaError.UnexpectedStatus(resp.code, null)
            DistributionIconResponse(
                schema = null,
                appId = appId,
                iconUrl = location,
                iconStoragePath = null,
            )
        }
    }

    // -------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------

    /** `POST …/applications/{appId}/releases` — requires `distribution:write`. */
    public suspend fun publishRelease(
        appId: String,
        request: PublishReleaseRequest,
    ): DistributionRelease =
        http.post(
            path = "${base()}/$appId/releases",
            bodySerializer = PublishReleaseRequest.serializer(),
            body = request,
            deserializer = DistributionReleaseResponse.serializer(),
        ).release

    /**
     * `PATCH …/applications/{appId}/releases/{releaseId}` — update notes or
     * yank the release. Requires `distribution:write`.
     */
    public suspend fun updateRelease(
        appId: String,
        releaseId: String,
        update: UpdateReleaseRequest,
    ): DistributionRelease =
        http.patch(
            path = "${base()}/$appId/releases/$releaseId",
            bodySerializer = UpdateReleaseRequest.serializer(),
            body = update,
            deserializer = DistributionReleaseResponse.serializer(),
        ).release
}
