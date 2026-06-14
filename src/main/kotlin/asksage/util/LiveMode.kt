package asksage.util

enum class LiveMode(val value: Int, val displayName: String, val description: String) {
    NO_LIVE(0, "No Live", "Offline mode - model uses only training data + datasets"),
    LIVE(1, "Live", "Basic internet augmentation - web search for current info"),
    LIVE_PLUS(2, "Live+", "Enhanced research mode - deeper internet research");
    companion object {
        fun fromValue(value: Int): LiveMode = entries.firstOrNull { it.value == value } ?: NO_LIVE
    }
}
