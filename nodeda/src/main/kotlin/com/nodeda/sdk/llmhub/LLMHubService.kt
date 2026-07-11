package com.nodeda.sdk.llmhub

import com.nodeda.sdk.core.HealthResponse
import com.nodeda.sdk.core.HttpClient

/**
 * Client for the **NodeDa Vertex LLM Hub API**
 * (`https://api.nodeda.com` — path prefix `/v1/organizations/{orgId}/llm/`).
 *
 * OpenAI-compatible chat completions. The **server** chooses the upstream
 * (Nrova Gemini vs BYO) from Developer → LLM Hub `routingMode`
 * (`nrova` | `byo` | `prefer_byo`). Clients keep the same request shape;
 * `model` is an optional hint. Requires a developer API key with the
 * [LLMHubScope.INVOKE] (`llm:invoke`) scope.
 *
 * ```kotlin
 * // Prefer omitting model — Hub / BYO defaults apply.
 * val completion = client.llmHub.chat(
 *     messages = listOf(
 *         ChatMessage(role = ChatMessageRole.SYSTEM, content = "You are a helpful assistant."),
 *         ChatMessage(role = ChatMessageRole.USER, content = "Summarize our release notes."),
 *     ),
 *     temperature = 0.2,
 *     maxTokens = 512,
 * )
 * println(completion.firstContent)
 * ```
 */
public class LLMHubService internal constructor(
    private val http: HttpClient,
    private val orgId: String,
) {
    private fun base(): String = "v1/organizations/$orgId/llm"

    /** `GET /health` — does not require an API key. */
    public suspend fun health(): HealthResponse =
        http.get("health", HealthResponse.serializer(), authenticated = false)

    /** `POST …/llm/chat/completions` — requires `llm:invoke`. */
    public suspend fun createChatCompletion(
        request: ChatCompletionRequest,
    ): ChatCompletionResponse =
        http.post(
            path = "${base()}/chat/completions",
            bodySerializer = ChatCompletionRequest.serializer(),
            body = request,
            deserializer = ChatCompletionResponse.serializer(),
        )

    /** Sugar for [createChatCompletion]. */
    public suspend fun chat(
        messages: List<ChatMessage>,
        model: String? = null,
        temperature: Double? = null,
        maxTokens: Int? = null,
    ): ChatCompletionResponse = createChatCompletion(
        ChatCompletionRequest(
            messages = messages,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
        ),
    )
}
