package org.jetbrains.plugins.template.api

object AskSageEndpoints {
    const val DEFAULT_BASE_URL = "https://api.asksage.ai"

    // User API
    const val GET_TOKEN = "/user/get-token-with-api-key"

    // Server API
    const val GET_MODELS = "/server/get-models"
    const val GET_PERSONAS = "/server/get-personas"
    const val GET_DATASETS = "/server/get-datasets"
    const val QUERY = "/server/query"
    const val FOLLOW_UP_QUESTIONS = "/server/follow-up-questions"
}
