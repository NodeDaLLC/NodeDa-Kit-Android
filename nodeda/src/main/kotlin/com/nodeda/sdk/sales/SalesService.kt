package com.nodeda.sdk.sales

import com.nodeda.sdk.core.HealthResponse
import com.nodeda.sdk.core.HttpClient

/**
 * Client for the **NodeDa Sales API**
 * (`https://api.nodeda.com` — path prefix `/v1/organizations/{orgId}/sales/`).
 */
public class SalesService internal constructor(
    private val http: HttpClient,
    private val orgId: String,
) {
    private fun base(): String = "v1/organizations/$orgId/sales"

    public suspend fun health(): HealthResponse =
        http.get("health", HealthResponse.serializer(), authenticated = false)

    /** `POST …/sales/submissions`. */
    public suspend fun createSubmission(request: CreateSalesSubmissionRequest): SalesSubmission =
        http.post(
            path = "${base()}/submissions",
            bodySerializer = CreateSalesSubmissionRequest.serializer(),
            body = request,
            deserializer = SalesSubmissionResponse.serializer(),
        ).submission

    /** `GET …/sales/submissions?contactEmail=…&limit=…`. */
    public suspend fun listSubmissions(
        contactEmail: String,
        limit: Int? = null,
    ): List<SalesSubmission> =
        http.get(
            path = "${base()}/submissions",
            deserializer = SalesSubmissionsResponse.serializer(),
            query = mapOf(
                "contactEmail" to contactEmail,
                "limit" to limit?.toString(),
            ),
        ).submissions

    /** `GET …/sales/submissions/{submissionId}`. */
    public suspend fun getSubmission(submissionId: String): SalesSubmission =
        http.get(
            path = "${base()}/submissions/$submissionId",
            deserializer = SalesSubmissionResponse.serializer(),
        ).submission

    /** `GET …/sales/submissions/{submissionId}/comments`. */
    public suspend fun listComments(submissionId: String): List<SalesComment> =
        http.get(
            path = "${base()}/submissions/$submissionId/comments",
            deserializer = SalesCommentsResponse.serializer(),
        ).comments

    /** `POST …/sales/submissions/{submissionId}/comments`. */
    public suspend fun addComment(
        submissionId: String,
        request: CreateSalesCommentRequest,
    ): SalesComment =
        http.post(
            path = "${base()}/submissions/$submissionId/comments",
            bodySerializer = CreateSalesCommentRequest.serializer(),
            body = request,
            deserializer = SalesCommentResponse.serializer(),
        ).comment
}
