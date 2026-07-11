## 1.3

- LLM Hub: document **server-owned routing** (`nrova` / `byo` / `prefer_byo` from Hub config). Clients do not pick the provider.
- Docs: prefer omitting `model`; list gateway error slugs for Hub / BYO / spend-cap failures.
- Nil `model` / temperature / maxTokens remain omitted on the wire (`encodeDefaults = false`).

## 1.2

- Added Vertex LLM Hub client (`client.llmHub`) with OpenAI-compatible
  chat completions (`createChatCompletion` / `chat`).
- Catalog model ids on `LLMHubModelID`; scope constant `LLMHubScope.INVOKE`.
- Included `llmHub` in `ServiceEndpoints` and `healthAll()`.

## 1.1

- Previous release (pre–LLM Hub).
