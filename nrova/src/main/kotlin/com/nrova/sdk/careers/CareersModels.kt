package com.nrova.sdk.careers

import com.nrova.sdk.core.JsonValue
import kotlinx.serialization.Serializable

/** A job posting returned from `GET …/careers/postings`. */
@Serializable
public data class CareerPosting(
    public val requisitionNodeId: String,
    public val title: String? = null,
    public val location: String? = null,
    public val department: String? = null,
    public val employmentType: String? = null,
    public val description: String? = null,
    public val publishedAt: String? = null,
    public val updatedAt: String? = null,
    public val isOpen: Boolean? = null,
)

@Serializable
public data class CareerPostingsResponse(public val postings: List<CareerPosting>)

@Serializable
public data class CareerPostingResponse(public val posting: CareerPosting)

/**
 * Application form definition. Sections + fields are returned verbatim so the
 * client can render dynamic forms.
 */
@Serializable
public data class CareerApplicationTemplate(
    public val templateVersion: String,
    public val sections: List<CareerTemplateSection>,
)

@Serializable
public data class CareerTemplateSection(
    public val id: String,
    public val title: String? = null,
    public val description: String? = null,
    public val fields: List<CareerTemplateField>,
)

@Serializable
public data class CareerTemplateField(
    public val id: String,
    public val type: String,
    public val label: String? = null,
    public val required: Boolean? = null,
    public val options: List<String>? = null,
    public val helpText: String? = null,
)

@Serializable
public data class CareerApplicationTemplateResponse(public val template: CareerApplicationTemplate)

/** One submitted application. */
@Serializable
public data class CareerApplication(
    public val id: String,
    public val requisitionNodeId: String? = null,
    public val applicantEmail: String? = null,
    public val contactEmail: String? = null,
    public val templateVersion: String? = null,
    public val status: String? = null,
    public val answers: Map<String, JsonValue>? = null,
    public val createdAt: String? = null,
    public val updatedAt: String? = null,
)

@Serializable
public data class CareerApplicationsResponse(public val applications: List<CareerApplication>)

@Serializable
public data class CareerApplicationResponse(public val application: CareerApplication)

// Submission body

@Serializable
public data class SubmitCareerApplicationRequest(
    public val requisitionNodeId: String,
    public val templateVersion: String,
    public val applicantEmail: String,
    public val answers: Map<String, JsonValue>,
)
