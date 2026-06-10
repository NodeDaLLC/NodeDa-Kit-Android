package com.nrova.sdk.core

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Base URLs for the individual Cloud Functions that back each service. */
public data class ServiceEndpoints(
    public val distribution: HttpUrl,
    public val support: HttpUrl,
    public val sales: HttpUrl,
    public val careers: HttpUrl,
    public val newsroom: HttpUrl,
    public val developer: HttpUrl,
    public val systemStatus: HttpUrl,
    public val legalPolicies: HttpUrl,
) {
    public companion object {
        /** The default production hosts on `us-central1-nrovallc.cloudfunctions.net`. */
        public val production: ServiceEndpoints = ServiceEndpoints(
            distribution = "https://us-central1-nrovallc.cloudfunctions.net/distributionApi".toHttpUrl(),
            support      = "https://us-central1-nrovallc.cloudfunctions.net/crmSupportApi".toHttpUrl(),
            sales        = "https://us-central1-nrovallc.cloudfunctions.net/crmSalesApi".toHttpUrl(),
            careers      = "https://us-central1-nrovallc.cloudfunctions.net/careersApi".toHttpUrl(),
            newsroom     = "https://us-central1-nrovallc.cloudfunctions.net/newsroomApi".toHttpUrl(),
            developer    = "https://us-central1-nrovallc.cloudfunctions.net/developerApi".toHttpUrl(),
            systemStatus = "https://us-central1-nrovallc.cloudfunctions.net/systemStatusApi".toHttpUrl(),
            legalPolicies = "https://us-central1-nrovallc.cloudfunctions.net/legalPoliciesApi".toHttpUrl(),
        )
    }
}
