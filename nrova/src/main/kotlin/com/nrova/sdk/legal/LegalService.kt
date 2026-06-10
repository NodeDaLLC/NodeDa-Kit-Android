package com.nrova.sdk.legal

import com.nrova.sdk.core.HealthResponse
import com.nrova.sdk.core.HttpClient

/**
 * Client for the **Nrova Legal Policies API**
 * (`https://us-central1-nrovallc.cloudfunctions.net/legalPoliciesApi`).
 */
public class LegalService internal constructor(
    private val http: HttpClient,
    private val orgId: String,
) {
    private fun base(): String = "v1/organizations/$orgId/legal-policies"

    public suspend fun health(): HealthResponse =
        http.get("health", HealthResponse.serializer(), authenticated = false)

    /** `GET …/legal-policies`. */
    public suspend fun listPolicies(): LegalPoliciesResponse =
        http.get(base(), LegalPoliciesResponse.serializer())

    /** `GET …/legal-policies/by-key/{policyKey}`. */
    public suspend fun getPolicyByKey(key: String): LegalPolicyResponse =
        http.get("${base()}/by-key/$key", LegalPolicyResponse.serializer())

    /** `GET …/legal-policies/{policyId}`. */
    public suspend fun getPolicyById(policyId: String): LegalPolicyResponse =
        http.get("${base()}/$policyId", LegalPolicyResponse.serializer())

    /** `POST …/legal-policies` — requires `legal:write`. */
    public suspend fun createPolicy(request: CreateLegalPolicyRequest): LegalPolicyResponse =
        http.post(
            path = base(),
            bodySerializer = CreateLegalPolicyRequest.serializer(),
            body = request,
            deserializer = LegalPolicyResponse.serializer(),
        )

    /** `PUT …/legal-policies/{policyId}` — requires `legal:write`. */
    public suspend fun updatePolicy(
        policyId: String,
        update: UpdateLegalPolicyRequest,
    ): LegalPolicyResponse =
        http.put(
            path = "${base()}/$policyId",
            bodySerializer = UpdateLegalPolicyRequest.serializer(),
            body = update,
            deserializer = LegalPolicyResponse.serializer(),
        )

    /** `DELETE …/legal-policies/{policyId}` — requires `legal:write`. */
    public suspend fun deletePolicy(policyId: String) {
        http.delete("${base()}/$policyId")
    }

    /** `POST …/legal-policies/{policyId}/sections` — requires `legal:write`. */
    public suspend fun createSection(
        policyId: String,
        request: CreateLegalSectionRequest,
    ): LegalPolicySection =
        http.post(
            path = "${base()}/$policyId/sections",
            bodySerializer = CreateLegalSectionRequest.serializer(),
            body = request,
            deserializer = LegalPolicySectionResponse.serializer(),
        ).section

    /** `PUT …/legal-policies/{policyId}/sections/{sectionId}` — requires `legal:write`. */
    public suspend fun updateSection(
        policyId: String,
        sectionId: String,
        update: UpdateLegalSectionRequest,
    ): LegalPolicySection =
        http.put(
            path = "${base()}/$policyId/sections/$sectionId",
            bodySerializer = UpdateLegalSectionRequest.serializer(),
            body = update,
            deserializer = LegalPolicySectionResponse.serializer(),
        ).section

    /** `DELETE …/legal-policies/{policyId}/sections/{sectionId}` — requires `legal:write`. */
    public suspend fun deleteSection(policyId: String, sectionId: String) {
        http.delete("${base()}/$policyId/sections/$sectionId")
    }
}
