package ai.bigbear.pymatic.asksage.api

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
    const val GET_PLUGINS = "/server/get-plugins"
    const val EXECUTE_PLUGIN = "/server/execute-plugin"
    const val LIST_AGENTS = "/server/list-agents"
    const val EXECUTE_AGENT = "/server/execute-agent"
    const val TRAIN = "/server/train"
    const val COUNT_MONTHLY_TOKENS = "/user/count-monthly-tokens"

    // OpenAI-compatible endpoint
    const val OPENAI_CHAT_COMPLETIONS = "/openai/v1/chat/completions"

    // Anthropic-compatible endpoint
    const val ANTHROPIC_MESSAGES = "/anthropic/v1/messages"
}
