package ai.bigbear.pymatic.asksage.api.models

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

data class QueryResponse(
    val response: String?,
    val status: Int?,
    val message: String?,
)

// --- Follow-up Questions ---

data class FollowUpRequest(
    val message: String,
    val response: String,
    val model: String,
)

data class FollowUpResponse(
    val response: List<String>?,
    val status: Int?,
)

// --- Plugins ---

data class PluginInfo(
    val id: String?,
    val name: String,
    val description: String?,
    val category: String?,
)

data class PluginsResponse(
    val response: List<PluginInfo>?,
    val status: Int?,
)

data class ExecutePluginRequest(
    val plugin: String,
    val message: String,
    val model: String,
    val live: Int = 0,
)

data class ExecutePluginResponse(
    val response: String?,
    val status: Int?,
    val message: String?,
)

// --- Agents ---

data class AgentInfo(
    val id: String?,
    val name: String,
    val description: String?,
)

data class AgentsResponse(
    val response: List<AgentInfo>?,
    val status: Int?,
)

data class ExecuteAgentRequest(
    val agent: String,
    val message: String,
    val model: String,
    val live: Int = 0,
)

data class ExecuteAgentResponse(
    val response: String?,
    val status: Int?,
    val message: String?,
)

// --- Training ---

data class TrainRequest(
    val dataset: String,
    val url: String? = null,
    val content: String? = null,
    val title: String? = null,
)

data class TrainResponse(
    val response: String?,
    val status: Int?,
    val message: String?,
)

// --- Token Usage ---

data class TokenUsageResponse(
    val response: TokenUsageData?,
    val status: Int?,
)

data class TokenUsageData(
    @SerializedName("total_tokens") val totalTokens: Long?,
    @SerializedName("monthly_tokens") val monthlyTokens: Long?,
    @SerializedName("daily_tokens") val dailyTokens: Long?,
    @SerializedName("remaining_tokens") val remainingTokens: Long?,
)

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
