package com.nodeda.sdk.support

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class SupportPriority(public val wire: String) {
    @SerialName("low") LOW("low"),
    @SerialName("medium") MEDIUM("medium"),
    @SerialName("high") HIGH("high"),
    @SerialName("urgent") URGENT("urgent");
}

@Serializable
public enum class SupportCategory(public val wire: String) {
    @SerialName("billing") BILLING("billing"),
    @SerialName("technical") TECHNICAL("technical"),
    @SerialName("account") ACCOUNT("account"),
    @SerialName("feature_request") FEATURE_REQUEST("feature_request"),
    @SerialName("general") GENERAL("general"),
    @SerialName("other") OTHER("other");
}

@Serializable
public data class SupportTicket(
    public val id: String,
    public val contactEmail: String,
    public val applicationName: String? = null,
    public val subject: String,
    public val body: String? = null,
    public val priority: String? = null,
    public val category: String? = null,
    public val status: String? = null,
    public val channel: String? = null,
    public val environment: String? = null,
    public val deviceInfo: String? = null,
    public val relatedUrl: String? = null,
    public val requesterName: String? = null,
    public val createdAt: String? = null,
    public val updatedAt: String? = null,
)

@Serializable
public data class SupportComment(
    public val id: String,
    public val ticketId: String? = null,
    public val body: String,
    public val authorDisplayName: String? = null,
    public val authorEmail: String? = null,
    public val isInternal: Boolean? = null,
    public val createdAt: String? = null,
)

// Request bodies

@Serializable
public data class CreateSupportTicketRequest(
    public val contactEmail: String,
    public val applicationName: String,
    public val subject: String,
    public val body: String,
    public val priority: SupportPriority? = null,
    public val category: SupportCategory? = null,
    public val channel: String? = null,
    public val environment: String? = null,
    public val deviceInfo: String? = null,
    public val relatedUrl: String? = null,
    public val requesterName: String? = null,
)

@Serializable
public data class CreateSupportCommentRequest(
    public val body: String,
    public val authorDisplayName: String? = null,
    public val authorEmail: String? = null,
    public val isInternal: Boolean? = null,
)

// Response envelopes

@Serializable
public data class SupportTicketsResponse(public val tickets: List<SupportTicket>)

@Serializable
public data class SupportTicketResponse(public val ticket: SupportTicket)

@Serializable
public data class SupportCommentsResponse(public val comments: List<SupportComment>)

@Serializable
public data class SupportCommentResponse(public val comment: SupportComment)
