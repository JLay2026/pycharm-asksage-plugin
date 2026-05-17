package org.jetbrains.plugins.template.api.models

import com.google.gson.annotations.SerializedName

// --- Authentication ---

data class TokenRequest(
    val email: String,
    @SerializedName("api_key") val apiKey: String,
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String?,
    val status: Int?,
    val response: String?,
)

// --- Models ---

data class ModelInfo(
    val created: String?,
    val id: String,
    val name: String?,
    @SerializedName("owned_by") val ownedBy: String?,
)

data class ModelsResponse(
    val response: ModelsData?,
)

data class ModelsData(
    val data: List<ModelInfo>?,
)

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
