package com.nrova.sdk.careers

import com.nrova.sdk.core.HealthResponse
import com.nrova.sdk.core.HttpClient

/**
 * Client for the **Nrova Careers API**
 * (`https://us-central1-nrovallc.cloudfunctions.net/careersApi`).
 */
public class CareersService internal constructor(
    private val http: HttpClient,
    private val orgId: String,
) {
    private fun base(): String = "v1/organizations/$orgId/careers"

    public suspend fun health(): HealthResponse =
        http.get("health", HealthResponse.serializer(), authenticated = false)

    /** `GET …/careers/postings`. */
    public suspend fun listPostings(): List<CareerPosting> =
        http.get(
            path = "${base()}/postings",
            deserializer = CareerPostingsResponse.serializer(),
        ).postings

    /** `GET …/careers/postings/{requisitionNodeId}`. */
    public suspend fun getPosting(requisitionNodeId: String): CareerPosting =
        http.get(
            path = "${base()}/postings/$requisitionNodeId",
            deserializer = CareerPostingResponse.serializer(),
        ).posting

    /** `GET …/careers/application-template`. */
    public suspend fun applicationTemplate(): CareerApplicationTemplate =
        http.get(
            path = "${base()}/application-template",
            deserializer = CareerApplicationTemplateResponse.serializer(),
        ).template

    /** `GET …/careers/applications`. */
    public suspend fun listApplications(
        applicantEmail: String? = null,
        contactEmail: String? = null,
        status: String? = null,
        limit: Int? = null,
    ): List<CareerApplication> =
        http.get(
            path = "${base()}/applications",
            deserializer = CareerApplicationsResponse.serializer(),
            query = mapOf(
                "applicantEmail" to applicantEmail,
                "contactEmail" to contactEmail,
                "status" to status,
                "limit" to limit?.toString(),
            ),
        ).applications

    /** `GET …/careers/applications/{applicationId}`. */
    public suspend fun getApplication(applicationId: String): CareerApplication =
        http.get(
            path = "${base()}/applications/$applicationId",
            deserializer = CareerApplicationResponse.serializer(),
        ).application

    /** `POST …/careers/applications` — requires `careers:apply`. */
    public suspend fun submitApplication(
        request: SubmitCareerApplicationRequest,
    ): CareerApplication =
        http.post(
            path = "${base()}/applications",
            bodySerializer = SubmitCareerApplicationRequest.serializer(),
            body = request,
            deserializer = CareerApplicationResponse.serializer(),
        ).application
}
