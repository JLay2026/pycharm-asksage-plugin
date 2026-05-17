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
