/*
 * Illustrative usage of the NodeDa Android SDK. This file is NOT compiled as
 * part of the library — it's a reference snippet you can paste into your own
 * Android app. The full project lives at:
 *
 *     com.nodeda:nodeda-android:1.3
 */
package com.example.nodedasample

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nodeda.sdk.NodeDa
import com.nodeda.sdk.NodeDaClient
import com.nodeda.sdk.core.NodeDaError
import com.nodeda.sdk.distribution.DistributionChannel
import com.nodeda.sdk.distribution.DistributionPlatform
import com.nodeda.sdk.featureflags.EvaluateFlagsRequest
import com.nodeda.sdk.llmhub.ChatMessage
import com.nodeda.sdk.llmhub.ChatMessageRole
import com.nodeda.sdk.support.CreateSupportTicketRequest
import com.nodeda.sdk.support.SupportCategory
import com.nodeda.sdk.support.SupportPriority
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// 1. Set up the client once at app startup.
// ---------------------------------------------------------------------------
class NodeDaApp : Application() {
    lateinit var nodeda: NodeDaClient
        private set

    override fun onCreate() {
        super.onCreate()

        // Reads `com.nodeda.sdk.ApiKey` and (optional) `com.nodeda.sdk.OrganizationId`
        // from your AndroidManifest.xml <meta-data> tags.
        nodeda = NodeDaClient.fromManifest(this)

        Log.i("NodeDaApp", "NodeDa SDK ${NodeDa.VERSION} booted")
    }
}

// ---------------------------------------------------------------------------
// 2. Drive it from a ViewModel.
// ---------------------------------------------------------------------------
class ReleasesViewModel(private val client: NodeDaClient) : ViewModel() {

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
            } catch (e: NodeDaError.Api) {
                Log.w("Releases", "API error ${e.error.status}: ${e.error.code}")
            } catch (e: NodeDaError.Transport) {
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

    fun askLlmHub() {
        viewModelScope.launch {
            // Prefer omitting model — Hub / BYO defaults apply on the server.
            val completion = client.llmHub.chat(
                messages = listOf(
                    ChatMessage(role = ChatMessageRole.SYSTEM, content = "You are a helpful assistant."),
                    ChatMessage(role = ChatMessageRole.USER, content = "Summarize our release notes."),
                ),
                temperature = 0.2,
                maxTokens = 512,
            )
            Log.i("LLMHub", completion.firstContent ?: "(empty)")
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
        // Runs all 9 /health endpoints concurrently and returns once they
        // all respond — handy at boot for diagnostics dashboards.
        client.healthAll().forEach { (name, health) ->
            Log.i("Health", "$name -> ${health.ok}")
        }
    }
}
