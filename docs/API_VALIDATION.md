# Ask Sage API — DTO Validation Matrix

Validation of the plugin's request/response models against the official Ask Sage
OpenAPI specifications (server + user APIs, https://docs.asksage.ai/api-docs/swagger.html),
performed 2026-06-13. Drives the fixes in v1.0.1.

| Endpoint | Spec shape | Original DTO | Status |
|---|---|---|---|
| `POST /user/get-token-with-api-key` | body undocumented; `response` observed as object | `response: String` | Fixed (#22): tolerant `JsonElement` + `resolveToken()` |
| `POST /server/get-models` | `response: string[]`, top-level `data: ModelInfo[]` | `response: { data }` | Fixed (#22): `response: List<String>` + `data` + `resolveModels()` |
| `POST /server/query` | CompletionResponse: answer in `message`, `response` = status | read `response` first | Fixed: `QueryResponse.answer()` prefers `message` |
| `POST /server/follow-up-questions` | CompletionResponse (questions in text) | `response: List<String>` | Fixed: `FollowUpResponse.questions()`; request `message/model/dataset` |
| `POST /server/get-plugins` | items `{ id, plugin_name, title, ... }` | `{ id, name, ... }` | Fixed: `pluginName/title` + `displayName/identifier` |
| `POST /server/execute-plugin` | req `plugin_name` + `plugin_values`; resp bare string | req `plugin/message`; resp object | Fixed: request renamed; client returns `String` |
| `POST /server/list-agents` | items `{ id: int, uuid, ... }` | `id: String` | Fixed: `AgentInfo.id: Int` + `uuid` |
| `POST /server/execute-agent` | req `agent_id: int`; resp nests text in `response.response` | req `agent/model/live`; resp flat | Fixed: `agentId`; `ExecuteAgentResponse.text()` |
| `POST /server/train` | req `content` + `force_dataset`; resp `{response, embedding, status}` | req `dataset/url/title` | Fixed: `content/context/force_dataset` |
| `POST /server/count-monthly-tokens` | `response: integer` | `response: { 4 fields }` | Fixed: `JsonElement` + `count()`; dashboard shows monthly only |
| `POST /server/get-personas` | `response: object[]` | `response: List<PersonaInfo>` | OK (item fields unverified in spec) |
| `POST /server/get-datasets` | `response: string[]` | `response: List<String>` | OK |
| `/openai/v1/chat/completions`, `/anthropic/v1/messages` | standard OpenAI/Anthropic | matching DTOs | OK |

## Items needing live verification

These are implemented to the documented spec but should be confirmed against the
live API during smoke testing, as the exact payloads are environment-specific:

1. **execute-plugin `plugin_values`** — currently sent as `{"message": "<input>"}`.
   Real plugins declare a `fields` schema; the value object may need specific keys.
2. **execute-agent** — `variables` / `conversation_history` are not yet wired; only
   `agent_id` + `message` are sent (sufficient for simple agents).
3. **Streaming `/query`** — SSE chunk format ("delimiter-separated JSON chunks") is
   parsed as text; if the live stream emits JSON envelopes the renderer may need
   to extract the text field. Non-streaming fallback is correct.
4. **follow-up question format** — `questions()` handles a JSON array or a line list;
   confirm which the API returns.
