package com.nodeda.sdk.llmhub

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** OpenAI-compatible chat message role for LLM Hub completions. */
@Serializable
public enum class ChatMessageRole(public val wire: String) {
    @SerialName("system") SYSTEM("system"),
    @SerialName("user") USER("user"),
    @SerialName("assistant") ASSISTANT("assistant"),
}

/** One turn in an OpenAI-style chat completions request. */
@Serializable
public data class ChatMessage(
    public val role: ChatMessageRole,
    public val content: String,
)

/**
 * Request body for `POST …/llm/chat/completions`.
 *
 * Wire field names follow the OpenAI chat-completions shape (`max_tokens`, …).
 *
 * The gateway chooses Nrova Gemini vs BYO from the org’s Hub `routingMode`.
 * [model] is an optional hint only — omit it for the org default (Nrova) or
 * the Custom LLM configured model (BYO). Do not send a provider URL, API key,
 * or routing mode; those stay server-side.
 */
@Serializable
public data class ChatCompletionRequest(
    public val messages: List<ChatMessage>,
    public val model: String? = null,
    public val temperature: Double? = null,
    @SerialName("max_tokens") public val maxTokens: Int? = null,
)

/** Token usage reported by a chat completion response. */
@Serializable
public data class ChatCompletionUsage(
    @SerialName("prompt_tokens") public val promptTokens: Int? = null,
    @SerialName("completion_tokens") public val completionTokens: Int? = null,
    @SerialName("total_tokens") public val totalTokens: Int? = null,
)

/** Assistant message nested under a completion choice. */
@Serializable
public data class ChatCompletionChoiceMessage(
    public val role: ChatMessageRole? = null,
    public val content: String? = null,
)

/** One choice in an OpenAI-style chat completion response. */
@Serializable
public data class ChatCompletionChoice(
    public val index: Int? = null,
    public val message: ChatCompletionChoiceMessage? = null,
    @SerialName("finish_reason") public val finishReason: String? = null,
)

/** Response body for `POST …/llm/chat/completions`. */
@Serializable
public data class ChatCompletionResponse(
    public val id: String? = null,
    @SerialName("object") public val objectType: String? = null,
    public val created: Long? = null,
    public val model: String? = null,
    public val choices: List<ChatCompletionChoice> = emptyList(),
    public val usage: ChatCompletionUsage? = null,
) {
    /** Convenience: first choice’s assistant text, if present. */
    public val firstContent: String?
        get() = choices.firstOrNull()?.message?.content
}

/**
 * Catalog model ids for **Nrova-routed** Gemini models on LLM Hub.
 *
 * Useful when Hub routing is `nrova` (or `prefer_byo` fell back to Nrova).
 * Prefer these constants — or omit `model` entirely for the org default.
 * When the gateway routes to BYO, pass the provider’s model id (or omit for
 * the Custom LLM configured model). Never send a provider base URL or key.
 */
public object LLMHubModelID {
    /** Cost-efficient default for high-volume completions. */
    public const val GEMINI_31_FLASH_LITE: String = "gemini-3.1-flash-lite"

    /** Balanced speed and quality for general production workloads. */
    public const val GEMINI_25_FLASH: String = "gemini-2.5-flash"

    /** Strong reasoning and coding; elevated rates above 200k input tokens. */
    public const val GEMINI_25_PRO: String = "gemini-2.5-pro"

    /** Frontier Flash-class model (preview). */
    public const val GEMINI_3_FLASH_PREVIEW: String = "gemini-3-flash-preview"

    /** Highest Flash intelligence with search and grounding strength. */
    public const val GEMINI_35_FLASH: String = "gemini-3.5-flash"

    /** Recommended default when the org has no configured default yet. */
    public const val RECOMMENDED_DEFAULT: String = GEMINI_31_FLASH_LITE
}

/** Scope required on Developer API keys to call LLM Hub inference. */
public object LLMHubScope {
    public const val INVOKE: String = "llm:invoke"
}
