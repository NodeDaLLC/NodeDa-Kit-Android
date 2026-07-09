package com.nodeda.sdk.core

import android.content.Context
import android.content.pm.PackageManager
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Android equivalent of Swift's `Info.plist` loader.
 *
 * Reads credentials from `<meta-data>` entries in your app's
 * `AndroidManifest.xml` so that secrets and tenant ids are never compiled
 * into source.
 *
 * Required:
 * - `com.nodeda.sdk.ApiKey` (string) — the API key issued from the NodeDa
 *   dashboard. Recommended to load from `local.properties` /
 *   `BuildConfig` and inject via `manifestPlaceholders` rather than
 *   committing literally.
 *
 * Optional:
 * - `com.nodeda.sdk.OrganizationId` (string) — overrides the
 *   [NodeDaConfiguration.DEFAULT_ORGANIZATION_ID]. Production apps should set
 *   this entry to their real organization id so the tenant is explicit.
 *
 * Both keys can be renamed by passing a custom [ManifestKeys].
 *
 * ```kotlin
 * // Application.onCreate
 * val client = NodeDaClient.fromManifest(this)
 * ```
 *
 * Add the keys to your app's `AndroidManifest.xml`:
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
public object ManifestConfiguration {

    public data class ManifestKeys(
        public val apiKey: String = DEFAULT_API_KEY,
        public val organizationId: String = DEFAULT_ORGANIZATION_ID,
    ) {
        public companion object {
            public const val DEFAULT_API_KEY: String = "com.nodeda.sdk.ApiKey"
            public const val DEFAULT_ORGANIZATION_ID: String = "com.nodeda.sdk.OrganizationId"
            public val Default: ManifestKeys = ManifestKeys()
        }
    }

    /** Errors raised when the manifest is missing required NodeDa configuration. */
    public sealed class ManifestError(message: String) : Exception(message) {
        /** The configured API-key meta-data entry is missing or empty. */
        public class MissingApiKey(public val manifestKey: String, public val pkg: String?) :
            ManifestError(
                "NodeDa: missing API key. Add a <meta-data android:name=\"$manifestKey\" " +
                    "android:value=\"…\"/> entry to your app's AndroidManifest.xml" +
                    (pkg?.let { " in package $it" } ?: "") + "."
            )

        /** The organization id entry is present but empty. */
        public class EmptyOrganizationId(public val manifestKey: String, public val pkg: String?) :
            ManifestError(
                "NodeDa: <meta-data $manifestKey> entry" +
                    (pkg?.let { " in package $it" } ?: "") +
                    " is empty. Remove it to fall back to the default organization, " +
                    "or set a non-empty string."
            )
    }

    /**
     * Build a configuration by reading meta-data from the supplied [context]'s
     * application manifest. Throws [ManifestError] when required entries are
     * missing.
     */
    @JvmStatic
    @JvmOverloads
    public fun fromManifest(
        context: Context,
        keys: ManifestKeys = ManifestKeys.Default,
        endpoints: ServiceEndpoints = ServiceEndpoints.production,
        defaultHeaders: Map<String, String> = emptyMap(),
        timeout: Duration = 30.seconds,
    ): NodeDaConfiguration {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val pkg = appContext.packageName

        val bundle = try {
            @Suppress("DEPRECATION") // GET_META_DATA flag works on every supported Android version.
            val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(
                    pkg,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
                )
            } else {
                pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
            }
            info.metaData
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        val raw = bundle?.get(keys.apiKey)?.toString()?.trim()
        if (raw.isNullOrEmpty()) {
            throw ManifestError.MissingApiKey(manifestKey = keys.apiKey, pkg = pkg)
        }

        val rawOrg = bundle.get(keys.organizationId)?.toString()
        val orgId = if (rawOrg != null) {
            val trimmed = rawOrg.trim()
            if (trimmed.isEmpty()) {
                throw ManifestError.EmptyOrganizationId(manifestKey = keys.organizationId, pkg = pkg)
            }
            trimmed
        } else {
            NodeDaConfiguration.DEFAULT_ORGANIZATION_ID
        }

        return NodeDaConfiguration(
            apiKey = raw,
            organizationId = orgId,
            endpoints = endpoints,
            defaultHeaders = defaultHeaders,
            timeout = timeout,
        )
    }

    /**
     * In-memory equivalent of [fromManifest]. Useful for unit tests or when
     * you'd rather load credentials from a custom source than the manifest.
     */
    @JvmStatic
    @JvmOverloads
    public fun fromMap(
        metadata: Map<String, Any?>,
        keys: ManifestKeys = ManifestKeys.Default,
        packageName: String? = null,
        endpoints: ServiceEndpoints = ServiceEndpoints.production,
        defaultHeaders: Map<String, String> = emptyMap(),
        timeout: Duration = 30.seconds,
    ): NodeDaConfiguration {
        val raw = metadata[keys.apiKey]?.toString()?.trim()
        if (raw.isNullOrEmpty()) {
            throw ManifestError.MissingApiKey(manifestKey = keys.apiKey, pkg = packageName)
        }

        val rawOrg = metadata[keys.organizationId]?.toString()
        val orgId = if (rawOrg != null) {
            val trimmed = rawOrg.trim()
            if (trimmed.isEmpty()) {
                throw ManifestError.EmptyOrganizationId(manifestKey = keys.organizationId, pkg = packageName)
            }
            trimmed
        } else {
            NodeDaConfiguration.DEFAULT_ORGANIZATION_ID
        }

        return NodeDaConfiguration(
            apiKey = raw,
            organizationId = orgId,
            endpoints = endpoints,
            defaultHeaders = defaultHeaders,
            timeout = timeout,
        )
    }
}
