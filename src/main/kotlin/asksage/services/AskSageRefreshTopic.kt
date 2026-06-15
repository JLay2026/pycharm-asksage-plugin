package asksage.services

import com.intellij.util.messages.Topic

/** Fired when credentials/registries change so open tool-window panels reload without an IDE restart. */
fun interface AskSageRefreshListener {
    fun onRefreshRequested()
}

object AskSageRefreshTopic {
    @JvmField
    val TOPIC: Topic<AskSageRefreshListener> = Topic.create("AskSage Refresh", AskSageRefreshListener::class.java)
}
