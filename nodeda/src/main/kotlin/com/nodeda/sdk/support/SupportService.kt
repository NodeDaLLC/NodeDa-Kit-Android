package com.nodeda.sdk.support

import com.nodeda.sdk.core.HealthResponse
import com.nodeda.sdk.core.HttpClient

/**
 * Client for the **NodeDa Support API**
 * (`https://api.nodeda.com` — path prefix `/v1/organizations/{orgId}/support/`).
 */
public class SupportService internal constructor(
    private val http: HttpClient,
    private val orgId: String,
) {
    private fun base(): String = "v1/organizations/$orgId/support"

    /** `GET /health`. */
    public suspend fun health(): HealthResponse =
        http.get("health", HealthResponse.serializer(), authenticated = false)

    /** `POST …/support/tickets`. */
    public suspend fun createTicket(request: CreateSupportTicketRequest): SupportTicket =
        http.post(
            path = "${base()}/tickets",
            bodySerializer = CreateSupportTicketRequest.serializer(),
            body = request,
            deserializer = SupportTicketResponse.serializer(),
        ).ticket

    /** `GET …/support/tickets?contactEmail=…`. */
    public suspend fun listTickets(contactEmail: String): List<SupportTicket> =
        http.get(
            path = "${base()}/tickets",
            deserializer = SupportTicketsResponse.serializer(),
            query = mapOf("contactEmail" to contactEmail),
        ).tickets

    /** `GET …/support/tickets/{ticketId}`. */
    public suspend fun getTicket(ticketId: String): SupportTicket =
        http.get(
            path = "${base()}/tickets/$ticketId",
            deserializer = SupportTicketResponse.serializer(),
        ).ticket

    /** `GET …/support/tickets/{ticketId}/comments`. */
    public suspend fun listComments(ticketId: String): List<SupportComment> =
        http.get(
            path = "${base()}/tickets/$ticketId/comments",
            deserializer = SupportCommentsResponse.serializer(),
        ).comments

    /** `POST …/support/tickets/{ticketId}/comments`. */
    public suspend fun addComment(
        ticketId: String,
        request: CreateSupportCommentRequest,
    ): SupportComment =
        http.post(
            path = "${base()}/tickets/$ticketId/comments",
            bodySerializer = CreateSupportCommentRequest.serializer(),
            body = request,
            deserializer = SupportCommentResponse.serializer(),
        ).comment
}
