package com.nodeda.sdk

import com.google.common.truth.Truth.assertThat
import com.nodeda.sdk.core.ManifestConfiguration
import com.nodeda.sdk.core.NodeDaConfiguration
import com.nodeda.sdk.core.NodeDaError
import com.nodeda.sdk.core.ServiceEndpoints
import com.nodeda.sdk.distribution.DistributionArtifact
import com.nodeda.sdk.distribution.DistributionArtifactPurpose
import com.nodeda.sdk.distribution.DistributionChannel
import com.nodeda.sdk.distribution.DistributionPlatform
import com.nodeda.sdk.distribution.PublishReleaseRequest
import com.nodeda.sdk.llmhub.ChatCompletionRequest
import com.nodeda.sdk.llmhub.ChatMessage
import com.nodeda.sdk.llmhub.ChatMessageRole
import com.nodeda.sdk.llmhub.LLMHubModelID
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.Buffer
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Direct port of `NodeDaClientTests.swift`. Where the Swift suite reaches for
 * `XCTUnwrap` / `XCTAssertEqual`, the Kotlin port uses Google Truth so
 * failure messages stay readable.
 */
class NodeDaClientTest {

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------

    @Test
    fun `default configuration uses production endpoints`() {
        val configuration = NodeDaConfiguration(apiKey = "test")

        assertThat(configuration.organizationId)
            .isEqualTo(NodeDaConfiguration.DEFAULT_ORGANIZATION_ID)
        val unified = "https://api.nodeda.com"
        // OkHttp HttpUrl always normalizes a host-only URL to include a trailing `/`.
        assertThat(configuration.endpoints.distribution.toString().trimEnd('/')).isEqualTo(unified)
        assertThat(configuration.endpoints.support.toString().trimEnd('/')).isEqualTo(unified)
        assertThat(configuration.endpoints.sales.toString().trimEnd('/')).isEqualTo(unified)
        assertThat(configuration.endpoints.careers.toString().trimEnd('/')).isEqualTo(unified)
        assertThat(configuration.endpoints.newsroom.toString().trimEnd('/')).isEqualTo(unified)
        assertThat(configuration.endpoints.developer.toString().trimEnd('/')).isEqualTo(unified)
        assertThat(configuration.endpoints.systemStatus.toString().trimEnd('/')).isEqualTo(unified)
        assertThat(configuration.endpoints.legalPolicies.toString().trimEnd('/')).isEqualTo(unified)
        assertThat(configuration.endpoints.llmHub.toString().trimEnd('/')).isEqualTo(unified)
        assertThat(ServiceEndpoints.UNIFIED_API_BASE).isEqualTo(unified)
    }

    @Test
    fun `client exposes every service`() {
        val client = NodeDaClient(apiKey = "test", transport = MockTransport())
        assertThat(client.distribution).isNotNull()
        assertThat(client.support).isNotNull()
        assertThat(client.sales).isNotNull()
        assertThat(client.careers).isNotNull()
        assertThat(client.newsroom).isNotNull()
        assertThat(client.featureFlags).isNotNull()
        assertThat(client.systemStatus).isNotNull()
        assertThat(client.legal).isNotNull()
        assertThat(client.llmHub).isNotNull()
    }

    // -----------------------------------------------------------------------
    // Distribution wiring
    // -----------------------------------------------------------------------

    @Test
    fun `distribution listApplications sends the expected request`() = runTest {
        val orgId = NodeDaConfiguration.DEFAULT_ORGANIZATION_ID
        val mock = MockTransport { request ->
            assertThat(request.method).isEqualTo("GET")
            assertThat(request.url.toString()).isEqualTo(
                "https://api.nodeda.com/v1/organizations/$orgId/applications"
            )
            assertThat(request.header("Authorization")).isEqualTo("Bearer test-key")
            assertThat(request.header("X-API-Key")).isEqualTo("test-key")

            val json = """
                {
                  "schema": "nrova.distribution.v1",
                  "orgId": "$orgId",
                  "applications": [
                    {
                      "id": "acme-notes",
                      "slug": "acme-notes",
                      "name": "Acme Notes",
                      "platforms": ["macos", "windows"],
                      "createdAt": "2026-04-09T12:00:00.000Z",
                      "updatedAt": "2026-06-01T14:00:00.000Z"
                    }
                  ]
                }
            """.trimIndent()
            json.toByteArray() to 200
        }

        val client = NodeDaClient(apiKey = "test-key", transport = mock)
        val response = client.distribution.listApplications()
        assertThat(response.applications).hasSize(1)
        assertThat(response.applications.first().id).isEqualTo("acme-notes")
        assertThat(response.applications.first().platforms)
            .containsExactly(DistributionPlatform.MACOS, DistributionPlatform.WINDOWS)
            .inOrder()
    }

    @Test
    fun `distribution latest encodes query and decodes payload`() = runTest {
        val orgId = NodeDaConfiguration.DEFAULT_ORGANIZATION_ID
        val mock = MockTransport { request ->
            assertThat(request.method).isEqualTo("GET")
            assertThat(request.url.encodedPath)
                .isEqualTo("/v1/organizations/$orgId/applications/acme-notes/latest")
            assertThat(request.url.queryParameter("platform")).isEqualTo("macos")
            assertThat(request.url.queryParameter("channel")).isEqualTo("stable")
            assertThat(request.url.queryParameter("purpose")).isEqualTo("install")

            val json = """
                {
                  "schema": "nrova.distribution.v1",
                  "appId": "acme-notes",
                  "channel": "stable",
                  "platform": "macos",
                  "release": {
                    "id": "rel_abc",
                    "version": "1.2.3",
                    "channel": "stable",
                    "isYanked": false,
                    "artifacts": [
                      {
                        "platform": "macos",
                        "fileName": "Acme-Notes-1.2.3.dmg",
                        "downloadUrl": "https://example.com/file.dmg",
                        "sizeBytes": 100,
                        "contentType": "application/x-apple-diskimage",
                        "installPurpose": "install"
                      }
                    ]
                  },
                  "artifact": {
                    "platform": "macos",
                    "fileName": "Acme-Notes-1.2.3.dmg",
                    "downloadUrl": "https://example.com/file.dmg",
                    "sizeBytes": 100,
                    "contentType": "application/x-apple-diskimage",
                    "installPurpose": "install"
                  }
                }
            """.trimIndent()
            json.toByteArray() to 200
        }

        val client = NodeDaClient(apiKey = "test-key", transport = mock)
        val latest = client.distribution.latest(
            appId = "acme-notes",
            platform = DistributionPlatform.MACOS,
            channel = DistributionChannel.STABLE,
            purpose = DistributionArtifactPurpose.INSTALL,
        )
        assertThat(latest.appId).isEqualTo("acme-notes")
        assertThat(latest.platform).isEqualTo(DistributionPlatform.MACOS)
        assertThat(latest.artifact.fileName).isEqualTo("Acme-Notes-1.2.3.dmg")
        assertThat(latest.artifact.installPurpose).isEqualTo(DistributionArtifactPurpose.INSTALL)
    }

    @Test
    fun `distribution publishRelease sends body`() = runTest {
        val mock = MockTransport { request ->
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.header("Content-Type")).isEqualTo("application/json")

            val buffer = Buffer().apply { request.body!!.writeTo(this) }
            val sent: JsonObject = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
            assertThat(sent["version"]!!.jsonPrimitive.content).isEqualTo("1.2.4")
            assertThat(sent["channel"]!!.jsonPrimitive.content).isEqualTo("stable")

            val json = """
                {
                  "schema": "nrova.distribution.v1",
                  "release": {
                    "id": "rel_new",
                    "version": "1.2.4",
                    "channel": "stable",
                    "isYanked": false,
                    "artifacts": [
                      {
                        "platform": "macos",
                        "fileName": "Acme.zip",
                        "downloadUrl": "https://example.com/file.zip",
                        "sizeBytes": 200,
                        "contentType": "application/zip"
                      }
                    ]
                  }
                }
            """.trimIndent()
            json.toByteArray() to 200
        }

        val client = NodeDaClient(apiKey = "test-key", transport = mock)
        val request = PublishReleaseRequest(
            version = "1.2.4",
            channel = DistributionChannel.STABLE,
            artifacts = listOf(
                DistributionArtifact(
                    platform = DistributionPlatform.MACOS,
                    fileName = "Acme.zip",
                    downloadUrl = "https://example.com/file.zip",
                    sizeBytes = 200,
                    contentType = "application/zip",
                ),
            ),
        )
        val release = client.distribution.publishRelease(appId = "acme-notes", request = request)
        assertThat(release.id).isEqualTo("rel_new")
    }

    // -----------------------------------------------------------------------
    // Error mapping
    // -----------------------------------------------------------------------

    @Test
    fun `api error is surfaced`() = runTest {
        val mock = MockTransport { _ ->
            """{"error":"invalid_api_key","message":"Missing or unrecognized key."}"""
                .toByteArray() to 401
        }

        val client = NodeDaClient(apiKey = "bad", transport = mock)
        val thrown = try {
            client.distribution.listApplications()
            null
        } catch (e: NodeDaError.Api) {
            e
        }
        assertThat(thrown).isNotNull()
        assertThat(thrown!!.error.status).isEqualTo(401)
        assertThat(thrown.error.code).isEqualTo("invalid_api_key")
        assertThat(thrown.error.message).isEqualTo("Missing or unrecognized key.")
    }

    // -----------------------------------------------------------------------
    // Feature flags
    // -----------------------------------------------------------------------

    @Test
    fun `feature flags evaluate posts body`() = runTest {
        val orgId = NodeDaConfiguration.DEFAULT_ORGANIZATION_ID
        val mock = MockTransport { request ->
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.url.toString()).isEqualTo(
                "https://api.nodeda.com/v1/organizations/$orgId/evaluate"
            )

            val buffer = Buffer().apply { request.body!!.writeTo(this) }
            val sent = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
            assertThat(sent["subjectId"]!!.jsonPrimitive.content).isEqualTo("user-1")
            assertThat(sent["countryCode"]!!.jsonPrimitive.content).isEqualTo("US")

            val json = """
                {
                  "orgId": "$orgId",
                  "subjectId": "user-1",
                  "countryCode": "US",
                  "evaluatedAt": "2026-06-09T00:00:00.000Z",
                  "results": { "dark_mode": true }
                }
            """.trimIndent()
            json.toByteArray() to 200
        }

        val client = NodeDaClient(apiKey = "test-key", transport = mock)
        val enabled = client.featureFlags.isEnabled(
            flagKey = "dark_mode",
            subjectId = "user-1",
            countryCode = "US",
        )
        assertThat(enabled).isTrue()
    }

    // -----------------------------------------------------------------------
    // LLM Hub
    // -----------------------------------------------------------------------

    @Test
    fun `llm hub createChatCompletion encodes OpenAI body`() = runTest {
        val orgId = NodeDaConfiguration.DEFAULT_ORGANIZATION_ID
        val mock = MockTransport { request ->
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.url.encodedPath)
                .isEqualTo("/v1/organizations/$orgId/llm/chat/completions")
            assertThat(request.header("Authorization")).isEqualTo("Bearer test-key")
            assertThat(request.header("Content-Type")).isEqualTo("application/json")

            val buffer = Buffer().apply { request.body!!.writeTo(this) }
            val sent = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
            assertThat(sent["model"]!!.jsonPrimitive.content).isEqualTo("gemini-3.1-flash-lite")
            assertThat(sent["temperature"]!!.jsonPrimitive.double).isEqualTo(0.2)
            assertThat(sent["max_tokens"]!!.jsonPrimitive.int).isEqualTo(512)
            val messages = sent["messages"]!!.jsonArray
            assertThat(messages).hasSize(2)
            assertThat(messages[0].jsonObject["role"]!!.jsonPrimitive.content).isEqualTo("system")
            assertThat(messages[1].jsonObject["role"]!!.jsonPrimitive.content).isEqualTo("user")

            val json = """
                {
                  "id": "chatcmpl_test",
                  "object": "chat.completion",
                  "created": 1752240000,
                  "model": "gemini-3.1-flash-lite",
                  "choices": [
                    {
                      "index": 0,
                      "message": { "role": "assistant", "content": "Ship it." },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 24,
                    "completion_tokens": 3,
                    "total_tokens": 27
                  }
                }
            """.trimIndent()
            json.toByteArray() to 200
        }

        val client = NodeDaClient(apiKey = "test-key", transport = mock)
        val completion = client.llmHub.createChatCompletion(
            ChatCompletionRequest(
                messages = listOf(
                    ChatMessage(role = ChatMessageRole.SYSTEM, content = "You are a helpful assistant."),
                    ChatMessage(role = ChatMessageRole.USER, content = "Summarize our release notes."),
                ),
                model = LLMHubModelID.GEMINI_31_FLASH_LITE,
                temperature = 0.2,
                maxTokens = 512,
            ),
        )
        assertThat(completion.id).isEqualTo("chatcmpl_test")
        assertThat(completion.model).isEqualTo("gemini-3.1-flash-lite")
        assertThat(completion.firstContent).isEqualTo("Ship it.")
        assertThat(completion.usage?.totalTokens).isEqualTo(27)
        assertThat(completion.choices.first().finishReason).isEqualTo("stop")
    }

    @Test
    fun `llm hub chat sugar posts same endpoint`() = runTest {
        val orgId = NodeDaConfiguration.DEFAULT_ORGANIZATION_ID
        val mock = MockTransport { request ->
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.url.encodedPath)
                .isEqualTo("/v1/organizations/$orgId/llm/chat/completions")
            """{"id":"c1","choices":[{"message":{"role":"assistant","content":"Hi"}}]}"""
                .toByteArray() to 200
        }

        val client = NodeDaClient(apiKey = "test-key", transport = mock)
        val completion = client.llmHub.chat(
            messages = listOf(ChatMessage(role = ChatMessageRole.USER, content = "Hello")),
        )
        assertThat(completion.firstContent).isEqualTo("Hi")
    }

    // -----------------------------------------------------------------------
    // Manifest loader (analog of the Info.plist tests)
    // -----------------------------------------------------------------------

    @Test
    fun `manifest map loader uses provided key and org`() {
        val configuration = ManifestConfiguration.fromMap(
            mapOf(
                "com.nodeda.sdk.ApiKey" to "sk_test_abc",
                "com.nodeda.sdk.OrganizationId" to "TenantXYZ",
            )
        )
        assertThat(configuration.apiKey).isEqualTo("sk_test_abc")
        assertThat(configuration.organizationId).isEqualTo("TenantXYZ")
    }

    @Test
    fun `manifest map loader trims whitespace and falls back to default org`() {
        val configuration = ManifestConfiguration.fromMap(
            mapOf("com.nodeda.sdk.ApiKey" to "  sk_test_abc  ")
        )
        assertThat(configuration.apiKey).isEqualTo("sk_test_abc")
        assertThat(configuration.organizationId)
            .isEqualTo(NodeDaConfiguration.DEFAULT_ORGANIZATION_ID)
    }

    @Test
    fun `manifest map loader honours custom key names`() {
        val keys = ManifestConfiguration.ManifestKeys(
            apiKey = "myapp.NodeDaKey",
            organizationId = "myapp.NodeDaOrg",
        )
        val configuration = ManifestConfiguration.fromMap(
            metadata = mapOf(
                "myapp.NodeDaKey" to "sk_test_abc",
                "myapp.NodeDaOrg" to "TenantXYZ",
            ),
            keys = keys,
        )
        assertThat(configuration.apiKey).isEqualTo("sk_test_abc")
        assertThat(configuration.organizationId).isEqualTo("TenantXYZ")
    }

    @Test
    fun `manifest map loader throws when api key missing`() {
        val ex = assertThrows(ManifestConfiguration.ManifestError.MissingApiKey::class.java) {
            ManifestConfiguration.fromMap(emptyMap())
        }
        assertThat(ex.manifestKey).isEqualTo("com.nodeda.sdk.ApiKey")
    }

    @Test
    fun `manifest map loader throws when api key empty`() {
        assertThrows(ManifestConfiguration.ManifestError.MissingApiKey::class.java) {
            ManifestConfiguration.fromMap(mapOf("com.nodeda.sdk.ApiKey" to "   "))
        }
    }

    @Test
    fun `manifest map loader throws when organization empty`() {
        assertThrows(ManifestConfiguration.ManifestError.EmptyOrganizationId::class.java) {
            ManifestConfiguration.fromMap(
                mapOf(
                    "com.nodeda.sdk.ApiKey" to "sk_test_abc",
                    "com.nodeda.sdk.OrganizationId" to "",
                )
            )
        }
    }

    @Test
    fun `NodeDaClient fromMap wires everything`() {
        val client = NodeDaClient.fromMap(
            metadata = mapOf("com.nodeda.sdk.ApiKey" to "sk_test_abc"),
            transport = MockTransport(),
        )
        assertThat(client.configuration.apiKey).isEqualTo("sk_test_abc")
        assertThat(client.configuration.organizationId)
            .isEqualTo(NodeDaConfiguration.DEFAULT_ORGANIZATION_ID)
    }

    // -----------------------------------------------------------------------
    // Version constant
    // -----------------------------------------------------------------------

    @Test
    fun `SDK version is exposed`() {
        assertThat(NodeDa.VERSION).isNotEmpty()
        assertThat(NodeDa.VERSION).isEqualTo("1.1")
    }

    // -----------------------------------------------------------------------
    // Health
    // -----------------------------------------------------------------------

    @Test
    fun `health endpoint skips auth`() = runTest {
        val mock = MockTransport { request ->
            assertThat(request.header("Authorization")).isNull()
            assertThat(request.header("X-API-Key")).isNull()
            """{"ok":true,"service":"distribution-api"}""".toByteArray() to 200
        }
        val client = NodeDaClient(apiKey = "test-key", transport = mock)
        val health = client.distribution.health()
        assertThat(health.ok).isTrue()
        assertThat(health.service).isEqualTo("distribution-api")
    }

    // -----------------------------------------------------------------------
    // Endpoint smoke
    // -----------------------------------------------------------------------

    @Test
    fun `service endpoints are usable as okhttp HttpUrls`() {
        val custom = "https://staging.example.com".toHttpUrl()
        assertThat(custom.host).isEqualTo("staging.example.com")
    }
}
