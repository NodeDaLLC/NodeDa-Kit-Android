package com.nrova.sdk.sales

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class SalesLeadStatus(public val wire: String) {
    @SerialName("new") NEW("new"),
    @SerialName("working") WORKING("working"),
    @SerialName("qualified") QUALIFIED("qualified"),
    @SerialName("unqualified") UNQUALIFIED("unqualified"),
    @SerialName("converted") CONVERTED("converted");
}

@Serializable
public data class SalesSubmission(
    public val id: String,
    public val contactEmail: String,
    public val formName: String? = null,
    public val firstName: String? = null,
    public val lastName: String? = null,
    public val message: String? = null,
    public val details: String? = null,
    public val leadStatus: String? = null,
    public val leadSource: String? = null,
    public val company: String? = null,
    public val companySize: String? = null,
    public val companyWebsite: String? = null,
    public val address: String? = null,
    public val phone: String? = null,
    public val createdAt: String? = null,
    public val updatedAt: String? = null,
)

@Serializable
public data class SalesComment(
    public val id: String,
    public val body: String,
    public val authorDisplayName: String? = null,
    public val authorEmail: String? = null,
    public val createdAt: String? = null,
)

// Request bodies

@Serializable
public data class CreateSalesSubmissionRequest(
    public val contactEmail: String,
    public val formName: String,
    public val firstName: String,
    public val lastName: String,
    public val message: String? = null,
    public val details: String? = null,
    public val leadStatus: SalesLeadStatus? = null,
    public val leadSource: String? = null,
    public val company: String? = null,
    public val companySize: String? = null,
    public val companyWebsite: String? = null,
    public val address: String? = null,
    public val phone: String? = null,
)

@Serializable
public data class CreateSalesCommentRequest(
    public val body: String,
    public val authorDisplayName: String? = null,
    public val authorEmail: String? = null,
)

// Response envelopes

@Serializable
public data class SalesSubmissionsResponse(public val submissions: List<SalesSubmission>)

@Serializable
public data class SalesSubmissionResponse(public val submission: SalesSubmission)

@Serializable
public data class SalesCommentsResponse(public val comments: List<SalesComment>)

@Serializable
public data class SalesCommentResponse(public val comment: SalesComment)
