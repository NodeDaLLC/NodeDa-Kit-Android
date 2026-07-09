package com.nodeda.sdk.systemstatus

import com.nodeda.sdk.core.HealthResponse
import com.nodeda.sdk.core.HttpClient

/**
 * Client for the **NodeDa System Status API**
 * (`https://api.nodeda.com` — path prefix `/v1/organizations/{orgId}/status/`).
 */
public class SystemStatusService internal constructor(
    private val http: HttpClient,
    private val orgId: String,
) {
    private fun base(): String = "v1/organizations/$orgId/status"

    public suspend fun health(): HealthResponse =
        http.get("health", HealthResponse.serializer(), authenticated = false)

    /** `GET …/status` — rollup + components, sorted by sortOrder then name. */
    public suspend fun rollup(): SystemStatusRollup =
        http.get(base(), SystemStatusRollup.serializer())

    /** `GET …/status/components/{componentId}`. */
    public suspend fun getComponent(componentId: String): SystemStatusComponent =
        http.get(
            path = "${base()}/components/$componentId",
            deserializer = SystemStatusComponentResponse.serializer(),
        ).component

    /** `POST …/status/components` — requires `status:write`. */
    public suspend fun createComponent(
        request: CreateStatusComponentRequest,
    ): SystemStatusComponent =
        http.post(
            path = "${base()}/components",
            bodySerializer = CreateStatusComponentRequest.serializer(),
            body = request,
            deserializer = SystemStatusComponentResponse.serializer(),
        ).component

    /** `PUT …/status/components/{componentId}` — requires `status:write`. */
    public suspend fun updateComponent(
        componentId: String,
        update: UpdateStatusComponentRequest,
    ): SystemStatusComponent =
        http.put(
            path = "${base()}/components/$componentId",
            bodySerializer = UpdateStatusComponentRequest.serializer(),
            body = update,
            deserializer = SystemStatusComponentResponse.serializer(),
        ).component

    /** `DELETE …/status/components/{componentId}` — requires `status:write`. */
    public suspend fun deleteComponent(componentId: String) {
        http.delete("${base()}/components/$componentId")
    }
}
