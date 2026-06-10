package com.nrova.sdk

import android.content.Context
import com.nrova.sdk.careers.CareersService
import com.nrova.sdk.core.HealthResponse
import com.nrova.sdk.core.HttpClient
import com.nrova.sdk.core.ManifestConfiguration
import com.nrova.sdk.core.NrovaConfiguration
import com.nrova.sdk.core.NrovaTransport
import com.nrova.sdk.core.OkHttpTransport
import com.nrova.sdk.core.ServiceEndpoints
import com.nrova.sdk.distribution.DistributionService
import com.nrova.sdk.featureflags.FeatureFlagsService
import com.nrova.sdk.legal.LegalService
import com.nrova.sdk.newsroom.NewsroomService
import com.nrova.sdk.sales.SalesService
import com.nrova.sdk.support.SupportService
import com.nrova.sdk.systemstatus.SystemStatusService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Top-level entry point to all Nrova HTTP APIs.
 *
 * `NrovaClient` is intentionally lightweight: each public property
 * ([distribution], [support], …) is a typed service class backed by a shared
 * transport and the configured [NrovaConfiguration].
 *
 * ```kotlin
 * val client = NrovaClient(apiKey = "sk_live_…")
 * val latest = client.distribution.latest(
 *     appId = "acme-notes",
 *     platform = DistributionPlatform.MACOS,
 *     channel = DistributionChannel.STABLE,
 * )
 * Log.d("Nrova", latest.artifact.downloadUrl)
 * ```
 */
public class NrovaClient private constructor(
    public val configuration: NrovaConfiguration,
    public val transport: NrovaTransport,
) {
    public val distribution: DistributionService
    public val support: SupportService
    public val sales: SalesService
    public val careers: CareersService
    public val newsroom: NewsroomService
    public val featureFlags: FeatureFlagsService
    public val systemStatus: SystemStatusService
    public val legal: LegalService

    init {
        val orgId = configuration.organizationId
        distribution = DistributionService(
            http = HttpClient(configuration.endpoints.distribution, configuration, transport),
            orgId = orgId,
        )
        support = SupportService(
            http = HttpClient(configuration.endpoints.support, configuration, transport),
            orgId = orgId,
        )
        sales = SalesService(
            http = HttpClient(configuration.endpoints.sales, configuration, transport),
            orgId = orgId,
        )
        careers = CareersService(
            http = HttpClient(configuration.endpoints.careers, configuration, transport),
            orgId = orgId,
        )
        newsroom = NewsroomService(
            http = HttpClient(configuration.endpoints.newsroom, configuration, transport),
            orgId = orgId,
        )
        featureFlags = FeatureFlagsService(
            http = HttpClient(configuration.endpoints.developer, configuration, transport),
            orgId = orgId,
        )
        systemStatus = SystemStatusService(
            http = HttpClient(configuration.endpoints.systemStatus, configuration, transport),
            orgId = orgId,
        )
        legal = LegalService(
            http = HttpClient(configuration.endpoints.legalPolicies, configuration, transport),
            orgId = orgId,
        )
    }

    /**
     * Issues `GET /health` against every service base URL in parallel and
     * returns a map keyed by service name. Direct analog of Swift's
     * `withThrowingTaskGroup`-based `healthAll()`.
     */
    public suspend fun healthAll(): Map<String, HealthResponse> = coroutineScope {
        val tasks = listOf(
            "distribution" to async { distribution.health() },
            "support" to async { support.health() },
            "sales" to async { sales.health() },
            "careers" to async { careers.health() },
            "newsroom" to async { newsroom.health() },
            "featureFlags" to async { featureFlags.health() },
            "systemStatus" to async { systemStatus.health() },
            "legal" to async { legal.health() },
        )
        tasks.associate { (name, deferred) -> name to deferred.await() }
    }

    public companion object {
        // -------------------------------------------------------------------
        // Builders
        // -------------------------------------------------------------------

        /** Builds a fully wired client from an explicit [NrovaConfiguration]. */
        @JvmStatic
        @JvmOverloads
        public fun fromConfiguration(
            configuration: NrovaConfiguration,
            transport: NrovaTransport = OkHttpTransport(),
        ): NrovaClient = NrovaClient(configuration, transport)

        /** Convenience: build a client with sane defaults from an API key. */
        @JvmStatic
        @JvmOverloads
        public operator fun invoke(
            apiKey: String,
            organizationId: String = NrovaConfiguration.DEFAULT_ORGANIZATION_ID,
            endpoints: ServiceEndpoints = ServiceEndpoints.production,
            defaultHeaders: Map<String, String> = emptyMap(),
            timeout: Duration = 30.seconds,
            transport: NrovaTransport = OkHttpTransport(),
        ): NrovaClient = NrovaClient(
            configuration = NrovaConfiguration(
                apiKey = apiKey,
                organizationId = organizationId,
                endpoints = endpoints,
                defaultHeaders = defaultHeaders,
                timeout = timeout,
            ),
            transport = transport,
        )

        /**
         * Convenience initializer that pulls the API key and organization id
         * from the supplied context's `AndroidManifest.xml` — the recommended
         * way to wire the client up inside an Android app.
         *
         * ```kotlin
         * class MyApp : Application() {
         *     lateinit var nrova: NrovaClient
         *     override fun onCreate() {
         *         super.onCreate()
         *         nrova = NrovaClient.fromManifest(this)
         *     }
         * }
         * ```
         *
         * Add the keys to your `AndroidManifest.xml`:
         *
         * ```xml
         * <application …>
         *     <meta-data android:name="com.nrova.sdk.ApiKey"
         *                android:value="@string/nrova_api_key" />
         *     <meta-data android:name="com.nrova.sdk.OrganizationId"
         *                android:value="@string/nrova_org_id" />
         * </application>
         * ```
         */
        @JvmStatic
        @JvmOverloads
        public fun fromManifest(
            context: Context,
            keys: ManifestConfiguration.ManifestKeys = ManifestConfiguration.ManifestKeys.Default,
            endpoints: ServiceEndpoints = ServiceEndpoints.production,
            defaultHeaders: Map<String, String> = emptyMap(),
            timeout: Duration = 30.seconds,
            transport: NrovaTransport = OkHttpTransport(),
        ): NrovaClient = fromConfiguration(
            configuration = ManifestConfiguration.fromManifest(
                context = context,
                keys = keys,
                endpoints = endpoints,
                defaultHeaders = defaultHeaders,
                timeout = timeout,
            ),
            transport = transport,
        )

        /**
         * In-memory equivalent of [fromManifest]. Useful for unit tests or
         * when you'd rather load credentials from a custom source than the
         * manifest. Mirrors Swift's `fromInfoDictionary` helper.
         */
        @JvmStatic
        @JvmOverloads
        public fun fromMap(
            metadata: Map<String, Any?>,
            keys: ManifestConfiguration.ManifestKeys = ManifestConfiguration.ManifestKeys.Default,
            packageName: String? = null,
            endpoints: ServiceEndpoints = ServiceEndpoints.production,
            defaultHeaders: Map<String, String> = emptyMap(),
            timeout: Duration = 30.seconds,
            transport: NrovaTransport = OkHttpTransport(),
        ): NrovaClient = fromConfiguration(
            configuration = ManifestConfiguration.fromMap(
                metadata = metadata,
                keys = keys,
                packageName = packageName,
                endpoints = endpoints,
                defaultHeaders = defaultHeaders,
                timeout = timeout,
            ),
            transport = transport,
        )
    }
}
