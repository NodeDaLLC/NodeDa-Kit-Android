/*
 * Illustrative usage of the Nrova Android SDK. This file is NOT compiled as
 * part of the library — it's a reference snippet you can paste into your own
 * Android app. The full project lives at:
 *
 *     com.nrova:nrova-android:1.0
 */
package com.example.nrovasample

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nrova.sdk.Nrova
import com.nrova.sdk.NrovaClient
import com.nrova.sdk.core.NrovaError
import com.nrova.sdk.distribution.DistributionChannel
import com.nrova.sdk.distribution.DistributionPlatform
import com.nrova.sdk.featureflags.EvaluateFlagsRequest
import com.nrova.sdk.support.CreateSupportTicketRequest
import com.nrova.sdk.support.SupportCategory
import com.nrova.sdk.support.SupportPriority
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// 1. Set up the client once at app startup.
// ---------------------------------------------------------------------------
class NrovaApp : Application() {
    lateinit var nrova: NrovaClient
        private set

    override fun onCreate() {
        super.onCreate()

        // Reads `com.nrova.sdk.ApiKey` and (optional) `com.nrova.sdk.OrganizationId`
        // from your AndroidManifest.xml <meta-data> tags.
        nrova = NrovaClient.fromManifest(this)

        Log.i("NrovaApp", "Nrova SDK ${Nrova.VERSION} booted")
    }
}

// ---------------------------------------------------------------------------
// 2. Drive it from a ViewModel.
// ---------------------------------------------------------------------------
class ReleasesViewModel(private val client: NrovaClient) : ViewModel() {

    fun fetchLatestRelease() {
        viewModelScope.launch {
            try {
                val latest = client.distribution.latest(
                    appId = "acme-notes",
                    platform = DistributionPlatform.MACOS,
                    channel = DistributionChannel.STABLE,
                )
                val version = latest.artifact.version ?: latest.release.version
                Log.i("Releases", "Latest version: $version")
                Log.i("Releases", "Download: ${latest.artifact.downloadUrl}")
            } catch (e: NrovaError.Api) {
                Log.w("Releases", "API error ${e.error.status}: ${e.error.code}")
            } catch (e: NrovaError.Transport) {
                Log.w("Releases", "Network problem: ${e.message}")
            }
        }
    }

    fun fileSupportTicket(userEmail: String) {
        viewModelScope.launch {
            client.support.createTicket(
                CreateSupportTicketRequest(
                    contactEmail = userEmail,
                    applicationName = "Acme Notes Android",
                    subject = "Crash on launch",
                    body = "App crashes immediately after splash screen.",
                    priority = SupportPriority.HIGH,
                    category = SupportCategory.TECHNICAL,
                    environment = "production",
                )
            )
        }
    }

    fun checkFeatureFlag(userId: String) {
        viewModelScope.launch {
            val response = client.featureFlags.evaluate(
                EvaluateFlagsRequest(
                    subjectId = userId,
                    countryCode = "US",
                    flagKeys = listOf("dark_mode", "new_onboarding"),
                )
            )
            Log.i("Flags", "dark_mode = ${response.results["dark_mode"]}")
        }
    }

    suspend fun pingEverything() {
        // Runs all 8 /health endpoints concurrently and returns once they
        // all respond — handy at boot for diagnostics dashboards.
        client.healthAll().forEach { (name, health) ->
            Log.i("Health", "$name -> ${health.ok}")
        }
    }
}
