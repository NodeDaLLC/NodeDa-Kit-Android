package com.nodeda.sdk

import android.content.Context
import com.nodeda.sdk.careers.CareersService
import com.nodeda.sdk.core.HealthResponse
import com.nodeda.sdk.core.HttpClient
import com.nodeda.sdk.core.ManifestConfiguration
import com.nodeda.sdk.core.NodeDaConfiguration
import com.nodeda.sdk.core.NodeDaTransport
import com.nodeda.sdk.core.OkHttpTransport
import com.nodeda.sdk.core.ServiceEndpoints
import com.nodeda.sdk.distribution.DistributionService
import com.nodeda.sdk.featureflags.FeatureFlagsService
import com.nodeda.sdk.legal.LegalService
import com.nodeda.sdk.newsroom.NewsroomService
import com.nodeda.sdk.sales.SalesService
import com.nodeda.sdk.support.SupportService
import com.nodeda.sdk.systemstatus.SystemStatusService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Top-level entry point to all NodeDa HTTP APIs.
 *
 * `NodeDaClient` is intentionally lightweight: each public property
 * ([distribution], [support], …) is a typed service class backed by a shared
 * transport and the configured [NodeDaConfiguration].
 *
 * ```kotlin
 * val client = NodeDaClient(apiKey = "sk_live_…")
 * val latest = client.distribution.latest(
 *     appId = "acme-notes",
 *     platform = DistributionPlatform.MACOS,
 *     channel = DistributionChannel.STABLE,
 * )
 * Log.d("NodeDa", latest.artifact.downloadUrl)
 * ```
 */
public class NodeDaClient private constructor(
    public val configuration: NodeDaConfiguration,
    public val transport: NodeDaTransport,
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
     * Issues `GET /health` against the unified API base for every service
     * client in parallel and returns a map keyed by service name. Direct
     * analog of Swift's `withThrowingTaskGroup`-based `healthAll()`.
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

        /** Builds a fully wired client from an explicit [NodeDaConfiguration]. */
        @JvmStatic
        @JvmOverloads
        public fun fromConfiguration(
            configuration: NodeDaConfiguration,
            transport: NodeDaTransport = OkHttpTransport(),
        ): NodeDaClient = NodeDaClient(configuration, transport)

        /** Convenience: build a client with sane defaults from an API key. */
        @JvmStatic
        @JvmOverloads
        public operator fun invoke(
            apiKey: String,
            organizationId: String = NodeDaConfiguration.DEFAULT_ORGANIZATION_ID,
            endpoints: ServiceEndpoints = ServiceEndpoints.production,
            defaultHeaders: Map<String, String> = emptyMap(),
            timeout: Duration = 30.seconds,
            transport: NodeDaTransport = OkHttpTransport(),
        ): NodeDaClient = NodeDaClient(
            configuration = NodeDaConfiguration(
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
         *     lateinit var nodeda: NodeDaClient
         *     override fun onCreate() {
         *         super.onCreate()
         *         nodeda = NodeDaClient.fromManifest(this)
         *     }
         * }
         * ```
         *
         * Add the keys to your `AndroidManifest.xml`:
         *
         * ```xml
         * <application …>
         *     <meta-data android:name="com.nodeda.sdk.ApiKey"
         *                android:value="@string/nodeda_api_key" />
         *     <meta-data android:name="com.nodeda.sdk.OrganizationId"
         *                android:value="@string/nodeda_org_id" />
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
            transport: NodeDaTransport = OkHttpTransport(),
        ): NodeDaClient = fromConfiguration(
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
            transport: NodeDaTransport = OkHttpTransport(),
        ): NodeDaClient = fromConfiguration(
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
