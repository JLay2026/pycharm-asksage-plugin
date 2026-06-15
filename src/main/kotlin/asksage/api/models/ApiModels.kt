package asksage.api.models

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// --- Authentication ---

data class TokenRequest(
    val email: String,
    @SerializedName("api_key") val apiKey: String,
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String?,
    val status: Int?,
    // The Ask Sage token endpoint is undocumented in the OpenAPI spec and varies
    // by instance: `response` may be a bare token string OR an object containing
    // the token. Keep it as a raw JsonElement so parsing never fails on shape,
    // and resolve the token defensively via resolveToken().
    val response: JsonElement? = null,
    val message: String? = null,
) {
    /** Resolve the access token across known Ask Sage response shapes. */
    fun resolveToken(): String? {
        if (!accessToken.isNullOrBlank()) return accessToken
        val r = response ?: return null
        if (r.isJsonPrimitive && r.asJsonPrimitive.isString) {
            return r.asString.takeIf { it.isNotBlank() }
        }
        if (r.isJsonObject) {
            val obj = r.asJsonObject
            for (key in listOf("access_token", "token", "api_token", "accessToken")) {
                if (obj.has(key)) {
                    val el = obj.get(key)
                    if (el != null && el.isJsonPrimitive && el.asJsonPrimitive.isString) {
                        return el.asString.takeIf { it.isNotBlank() }
                    }
                }
            }
        }
        return null
    }
}

// --- Models ---

data class ModelInfo(
    val created: String?,
    val id: String,
    val name: String?,
    @SerializedName("owned_by") val ownedBy: String?,
)

// The /server/get-models response shape (per the Ask Sage OpenAPI spec):
//   { "response": ["model-name", ...],      // array of model-name strings
//     "object": "list",
//     "data": [ { id, object, created, name, owned_by }, ... ],  // rich objects
//     "status": 200 }
data class ModelsResponse(
    val response: List<String>? = null,
    val data: List<ModelInfo>? = null,
    val status: Int? = null,
) {
    /** Rich model objects when present; otherwise synthesize from the name list. */
    fun resolveModels(): List<ModelInfo> {
        data?.let { if (it.isNotEmpty()) return it }
        return response?.map { name ->
            ModelInfo(created = null, id = name, name = name, ownedBy = null)
        } ?: emptyList()
    }
}

// --- Personas ---

data class PersonaInfo(
    val id: Int,
    val name: String,
    val description: String?,
    val prompt: String?,
)

data class PersonasResponse(
    val response: List<PersonaInfo>?,
)

// --- Datasets ---

data class DatasetsResponse(
    val response: List<String>?,
)

// --- Query ---

data class QueryRequest(
    val model: String,
    val message: String,
    val live: Int = 0,
    val dataset: String? = null,
    val persona: Int? = null,
    @SerializedName("system_prompt") val systemPrompt: String? = null,
    val temperature: Double = 0.7,
    @SerializedName("limit_references") val limitReferences: Int = 0,
    val mode: String = "chat",
    @SerializedName("reasoning_effort") val reasoningEffort: String = "medium",
    val streaming: Boolean = false,
)

// /server/query returns a CompletionResponse: the generated answer is in
// `message`; `response` carries a status/error string. Read the answer via answer().
data class QueryResponse(
    val response: String?,
    val status: Int?,
    val message: String?,
) {
    fun answer(): String? = message?.takeIf { it.isNotBlank() } ?: response?.takeIf { it.isNotBlank() }
}

// --- Follow-up Questions ---

data class FollowUpRequest(
    val message: String,
    val model: String,
    val dataset: String = "all",
)

// follow-up-questions returns a CompletionResponse; the questions are in the
// generated text (`message`). questions() parses a JSON array or line list.
data class FollowUpResponse(
    val response: String? = null,
    val message: String? = null,
    val status: Int? = null,
) {
    fun questions(): List<String> {
        val text = (message?.takeIf { it.isNotBlank() } ?: response ?: "").trim()
        if (text.isEmpty()) return emptyList()
        // The server returns a sentinel (e.g. "Disabled") when follow-up
        // question generation is turned off for the account/instance. Treat
        // any such sentinel as "no questions" so it is not shown as a clickable
        // suggestion that would just re-send the sentinel word as a prompt.
        if (text.lowercase() in SENTINELS) return emptyList()
        if (text.startsWith("[")) {
            try {
                val arr = Gson().fromJson(text, Array<String>::class.java)
                if (arr != null) return arr.filter { it.isNotBlank() && it.trim().lowercase() !in SENTINELS }
            } catch (e: Exception) {
                // fall through to line parsing
            }
        }
        return text.lines()
            .map { it.trim().removePrefix("-").removePrefix("*").trim().trimStart('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', ')', ' ') }
            .filter { it.isNotBlank() && it.lowercase() !in SENTINELS }
    }

    companion object {
        private val SENTINELS = setOf("disabled", "not enabled", "feature disabled", "n/a", "none", "null")
    }
}

// --- Plugins ---

// /server/get-plugins items: { id, plugin_name, title, description, category, ... }
data class PluginInfo(
    val id: String? = null,
    @SerializedName("plugin_name") val pluginName: String? = null,
    val title: String? = null,
    val name: String? = null, // legacy/fallback field
    val description: String? = null,
    val category: String? = null,
) {
    val displayName: String get() = title ?: pluginName ?: name ?: id ?: "(plugin)"
    // Some plugins only expose `title`; include it so plugin_name is never empty.
    val identifier: String get() = pluginName ?: title ?: name ?: id ?: ""
}

data class PluginsResponse(
    val response: List<PluginInfo>?,
    val status: Int?,
)

// execute-plugin request: plugin_name + plugin_values (JSON string), model, live.
data class ExecutePluginRequest(
    @SerializedName("plugin_name") val pluginName: String,
    @SerializedName("plugin_values") val pluginValues: String,
    val model: String,
    val live: Int = 0,
)

// --- Agents ---

// /server/list-agents items: { id (int), uuid, name, description, agent_mode, ... }
data class AgentInfo(
    val id: Int?,
    val uuid: String? = null,
    val name: String,
    val description: String?,
)

data class AgentsResponse(
    val response: List<AgentInfo>?,
    val status: Int?,
)

// execute-agent request: agent_id (int) + message (+ optional streaming).
data class ExecuteAgentRequest(
    @SerializedName("agent_id") val agentId: Int,
    val message: String,
    val streaming: Boolean = false,
)

// execute-agent (AgentExecutionResponse): the text is nested in response.response.
data class ExecuteAgentResponse(
    val status: Int? = null,
    @SerializedName("execution_status") val executionStatus: String? = null,
    val response: AgentResponseBody? = null,
    val message: String? = null,
) {
    fun text(): String? = response?.response?.takeIf { it.isNotBlank() } ?: message?.takeIf { it.isNotBlank() }
}

data class AgentResponseBody(
    val response: String? = null,
    val type: String? = null,
    val source: String? = null,
)

// --- Training ---

data class TrainRequest(
    val content: String,
    val context: String? = null,
    @SerializedName("skip_vectordb") val skipVectorDb: Boolean = false,
    @SerializedName("force_dataset") val forceDataset: String? = null,
)

data class TrainResponse(
    val response: String?,
    val embedding: String? = null,
    val status: Int?,
)

// --- Token Usage ---

// /server/count-monthly-tokens returns a single integer count in `response`
// (the API exposes only monthly used tokens; no daily/total/remaining).
data class TokenUsageResponse(
    val response: JsonElement? = null,
    val status: Int? = null,
) {
    fun count(): Long? {
        val r = response ?: return null
        return try {
            if (r.isJsonPrimitive) r.asJsonPrimitive.asString.toLongOrNull() else null
        } catch (e: Exception) {
            null
        }
    }
}

// --- OpenAI-Compatible ---

data class OpenAiChatMessage(
    val role: String,
    val content: String,
)

data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    val temperature: Double = 0.7,
    val stream: Boolean = false,
)

data class OpenAiChatResponse(
    val id: String?,
    val choices: List<OpenAiChoice>?,
    val usage: OpenAiUsage?,
)

data class OpenAiChoice(
    val index: Int?,
    val message: OpenAiChatMessage?,
    @SerializedName("finish_reason") val finishReason: String?,
)

data class OpenAiUsage(
    @SerializedName("prompt_tokens") val promptTokens: Int?,
    @SerializedName("completion_tokens") val completionTokens: Int?,
    @SerializedName("total_tokens") val totalTokens: Int?,
)

// --- Anthropic-Compatible ---

data class AnthropicMessage(
    val role: String,
    val content: String,
)

data class AnthropicRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    val temperature: Double = 0.7,
    val system: String? = null,
)

data class AnthropicResponse(
    val id: String?,
    val type: String?,
    val role: String?,
    val content: List<AnthropicContentBlock>?,
    val usage: AnthropicUsage?,
)

data class AnthropicContentBlock(
    val type: String?,
    val text: String?,
)

data class AnthropicUsage(
    @SerializedName("input_tokens") val inputTokens: Int?,
    @SerializedName("output_tokens") val outputTokens: Int?,
)
