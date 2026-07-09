package com.nodeda.sdk.legal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class LegalPolicyStatus(public val wire: String) {
    @SerialName("draft") DRAFT("draft"),
    @SerialName("published") PUBLISHED("published"),
    @SerialName("archived") ARCHIVED("archived");
}

@Serializable
public data class LegalPolicy(
    public val id: String,
    public val key: String,
    public val title: String,
    public val description: String? = null,
    public val status: LegalPolicyStatus? = null,
    public val sectionCount: Int? = null,
    public val createdAt: String? = null,
    public val updatedAt: String? = null,
)

@Serializable
public data class LegalPolicySection(
    public val id: String,
    public val title: String? = null,
    public val body: String,
    public val sortOrder: Int? = null,
    public val createdAt: String? = null,
    public val updatedAt: String? = null,
)

@Serializable
public data class LegalPoliciesResponse(
    public val orgId: String? = null,
    public val policies: List<LegalPolicy>,
    public val generatedAt: String? = null,
)

@Serializable
public data class LegalPolicyResponse(
    public val orgId: String? = null,
    public val policy: LegalPolicy,
    public val sections: List<LegalPolicySection>,
    public val generatedAt: String? = null,
)

@Serializable
public data class LegalPolicySectionResponse(public val section: LegalPolicySection)

// Request bodies

@Serializable
public data class CreateLegalPolicyRequest(
    public val key: String,
    public val title: String,
    public val description: String? = null,
    public val status: LegalPolicyStatus? = null,
)

@Serializable
public data class UpdateLegalPolicyRequest(
    public val title: String? = null,
    public val description: String? = null,
    public val status: LegalPolicyStatus? = null,
)

@Serializable
public data class CreateLegalSectionRequest(
    public val title: String? = null,
    public val body: String,
    public val sortOrder: Int? = null,
)

@Serializable
public data class UpdateLegalSectionRequest(
    public val title: String? = null,
    public val body: String? = null,
    public val sortOrder: Int? = null,
)

/** Stable policy keys present in the live NodeDa organization. */
public object LegalPolicyKey {
    public const val PRIVACY: String = "privacy"
    public const val PRIVACY_CHOICES: String = "privacy_choices"
    public const val TERMS: String = "terms"
}
