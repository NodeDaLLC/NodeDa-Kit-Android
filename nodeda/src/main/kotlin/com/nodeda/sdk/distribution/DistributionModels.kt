package com.nodeda.sdk.distribution

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

/** Platforms supported by the Distribution API. */
@Serializable
public enum class DistributionPlatform(public val wire: String) {
    @SerialName("macos") MACOS("macos"),
    @SerialName("windows") WINDOWS("windows");

    public companion object {
        public fun fromWire(value: String?): DistributionPlatform? =
            entries.firstOrNull { it.wire == value }
    }
}

/** Release channels. */
@Serializable
public enum class DistributionChannel(public val wire: String) {
    @SerialName("stable") STABLE("stable"),
    @SerialName("beta") BETA("beta"),
    @SerialName("dev") DEV("dev");

    public companion object {
        public fun fromWire(value: String?): DistributionChannel? =
            entries.firstOrNull { it.wire == value }
    }
}

/**
 * Distinguishes auto-update payloads (typically `.zip`) from user-facing
 * installers (typically `.dmg`). The API treats missing as `update`.
 */
@Serializable
public enum class DistributionArtifactPurpose(public val wire: String) {
    @SerialName("install") INSTALL("install"),
    @SerialName("update") UPDATE("update");

    public companion object {
        public fun fromWire(value: String?): DistributionArtifactPurpose? =
            entries.firstOrNull { it.wire == value }
    }
}

/** Architectures the Distribution API reports for an artifact. */
@Serializable
public enum class DistributionArchitecture(public val wire: String) {
    @SerialName("x64") X64("x64"),
    @SerialName("arm64") ARM64("arm64"),
    @SerialName("universal") UNIVERSAL("universal"),
    @SerialName("x86") X86("x86");

    public companion object {
        public fun fromWire(value: String?): DistributionArchitecture? =
            entries.firstOrNull { it.wire == value }
    }
}

// ---------------------------------------------------------------------------
// Application
// ---------------------------------------------------------------------------

/**
 * A distribution application — typically maps to one product line shipping
 * both a macOS and Windows binary.
 */
@Serializable
public data class DistributionApplication(
    public val id: String,
    public val slug: String,
    public val name: String,
    public val platforms: List<DistributionPlatform>,
    public val bundleId: String? = null,
    public val description: String? = null,
    public val homepageUrl: String? = null,
    public val iconUrl: String? = null,
    public val iconStoragePath: String? = null,
    public val isPublic: Boolean? = null,
    public val latest: Map<String, Map<String, DistributionLatestPointer>>? = null,
    public val createdAt: String? = null,
    public val updatedAt: String? = null,
)

/** Pointer to the most recent release for a platform/channel pair. */
@Serializable
public data class DistributionLatestPointer(
    public val releaseId: String,
    public val version: String,
    public val updatedAt: String? = null,
)

// ---------------------------------------------------------------------------
// Releases & artifacts
// ---------------------------------------------------------------------------

@Serializable
public data class DistributionRelease(
    public val id: String,
    public val version: String,
    public val channel: DistributionChannel,
    public val buildNumber: String? = null,
    public val notes: String? = null,
    public val isYanked: Boolean,
    public val releasedAt: String? = null,
    public val updatedAt: String? = null,
    public val artifacts: List<DistributionArtifact>,
)

@Serializable
public data class DistributionArtifact(
    public val platform: DistributionPlatform,
    public val fileName: String,
    public val downloadUrl: String,
    public val sizeBytes: Long,
    public val contentType: String,
    public val sha256: String? = null,
    public val version: String? = null,
    public val buildNumber: String? = null,
    public val minOsVersion: String? = null,
    public val architecture: DistributionArchitecture? = null,
    public val installPurpose: DistributionArtifactPurpose? = null,
    public val metadataAutoDetected: Boolean? = null,
)

// ---------------------------------------------------------------------------
// Response envelopes
// ---------------------------------------------------------------------------

@Serializable
public data class DistributionApplicationsResponse(
    public val schema: String? = null,
    public val orgId: String? = null,
    public val applications: List<DistributionApplication>,
)

@Serializable
public data class DistributionApplicationResponse(
    public val schema: String? = null,
    public val application: DistributionApplication,
)

@Serializable
public data class DistributionReleasesResponse(
    public val schema: String? = null,
    public val appId: String? = null,
    public val releases: List<DistributionRelease>,
)

@Serializable
public data class DistributionReleaseResponse(
    public val schema: String? = null,
    public val release: DistributionRelease,
)

@Serializable
public data class DistributionLatestResponse(
    public val schema: String? = null,
    public val appId: String,
    public val channel: DistributionChannel,
    public val platform: DistributionPlatform,
    public val release: DistributionRelease,
    public val artifact: DistributionArtifact,
)

@Serializable
public data class DistributionIconResponse(
    public val schema: String? = null,
    public val appId: String? = null,
    public val iconUrl: String,
    public val iconStoragePath: String? = null,
)

// ---------------------------------------------------------------------------
// Request bodies
// ---------------------------------------------------------------------------

@Serializable
public data class PublishReleaseRequest(
    public val version: String,
    public val channel: DistributionChannel,
    public val buildNumber: String? = null,
    public val notes: String? = null,
    public val artifacts: List<DistributionArtifact>,
)

@Serializable
public data class UpdateReleaseRequest(
    public val notes: String? = null,
    public val isYanked: Boolean? = null,
)
