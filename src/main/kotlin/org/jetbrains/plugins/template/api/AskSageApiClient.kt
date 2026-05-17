package org.jetbrains.plugins.template.api

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.plugins.template.api.models.DatasetsResponse
import org.jetbrains.plugins.template.api.models.FollowUpRequest
import org.jetbrains.plugins.template.api.models.FollowUpResponse
import org.jetbrains.plugins.template.api.models.ModelsResponse
import org.jetbrains.plugins.template.api.models.PersonasResponse
import org.jetbrains.plugins.template.api.models.QueryRequest
import org.jetbrains.plugins.template.api.models.QueryResponse
import org.jetbrains.plugins.template.api.models.TokenRequest
import org.jetbrains.plugins.template.api.models.TokenResponse
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.function.Consumer

class AskSageApiClient(
    private var baseUrl: String = AskSageEndpoints.DEFAULT_BASE_URL,
) {
    private val gson = Gson()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    fun updateBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    fun getToken(email: String, apiKey: String): TokenResponse {
        val request = TokenRequest(email, apiKey)
        return post(AskSageEndpoints.GET_TOKEN, request, null, TokenResponse::class.java)
    }

    fun getModels(token: String): ModelsResponse {
        return post(AskSageEndpoints.GET_MODELS, null, token, ModelsResponse::class.java)
    }

    fun getPersonas(token: String): PersonasResponse {
        return post(AskSageEndpoints.GET_PERSONAS, null, token, PersonasResponse::class.java)
    }

    fun getDatasets(token: String): DatasetsResponse {
        return post(AskSageEndpoints.GET_DATASETS, null, token, DatasetsResponse::class.java)
    }

    fun query(token: String, queryRequest: QueryRequest): QueryResponse {
        return post(AskSageEndpoints.QUERY, queryRequest, token, QueryResponse::class.java)
    }

    fun queryStreaming(token: String, queryRequest: QueryRequest, onChunk: Consumer<String>) {
        val streamingRequest = queryRequest.copy(streaming = true)
        val url = "$baseUrl${AskSageEndpoints.QUERY}"
        val jsonBody = gson.toJson(streamingRequest)

        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .header("x-access-tokens", token)
            .timeout(Duration.ofSeconds(300))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))

        val httpRequest = requestBuilder.build()

        try {
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream())

            if (response.statusCode() !in 200..299) {
                val body = response.body().bufferedReader().readText()
                LOG.warn("Streaming request failed with status ${response.statusCode()}: $body")
                throw AskSageApiException("API error (${response.statusCode()})")
            }

            BufferedReader(InputStreamReader(response.body())).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    if (currentLine.startsWith("data: ")) {
                        val data = currentLine.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        onChunk.accept(data)
                    } else if (currentLine.isNotBlank() && !currentLine.startsWith(":")) {
                        // Non-SSE response — treat as plain text chunk
                        onChunk.accept(currentLine)
                    }
                }
            }
        } catch (e: IOException) {
            LOG.error("Streaming network error", e)
            throw AskSageApiException("Network error: ${e.message}", e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw AskSageApiException("Request interrupted", e)
        }
    }

    fun getFollowUpQuestions(token: String, message: String, response: String, model: String): FollowUpResponse {
        val request = FollowUpRequest(message = message, response = response, model = model)
        return post(AskSageEndpoints.FOLLOW_UP_QUESTIONS, request, token, FollowUpResponse::class.java)
    }

    private fun <T> post(endpoint: String, body: Any?, token: String?, responseType: Class<T>): T {
        val url = "$baseUrl$endpoint"
        val jsonBody = if (body != null) gson.toJson(body) else "{}"

        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(120))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))

        if (token != null) {
            requestBuilder.header("x-access-tokens", token)
        }

        val httpRequest = requestBuilder.build()

        try {
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            val responseBody = response.body()

            if (response.statusCode() !in 200..299) {
                LOG.warn("API request to $endpoint failed with status ${response.statusCode()}: $responseBody")
            }

            return gson.fromJson(responseBody, responseType)
        } catch (e: IOException) {
            LOG.error("Network error calling $endpoint", e)
            throw AskSageApiException("Network error: ${e.message}", e)
        } catch (e: JsonSyntaxException) {
            LOG.error("Failed to parse response from $endpoint", e)
            throw AskSageApiException("Invalid response format: ${e.message}", e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw AskSageApiException("Request interrupted", e)
        }
    }

    companion object {
        private val LOG = logger<AskSageApiClient>()
    }
}

class AskSageApiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
