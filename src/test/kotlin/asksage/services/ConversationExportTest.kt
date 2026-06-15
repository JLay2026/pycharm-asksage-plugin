package asksage.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import junit.framework.TestCase

class ConversationExportTest : TestCase() {

    private val gson = Gson()

    private fun createTestMessages(): List<ChatMessage> {
        return listOf(
            ChatMessage(MessageRole.USER, "Hello", null, 1700000000000L),
            ChatMessage(MessageRole.ASSISTANT, "Hi there!", "gpt-4", 1700000001000L),
            ChatMessage(MessageRole.USER, "How are you?", null, 1700000002000L),
            ChatMessage(MessageRole.ASSISTANT, "I'm good!", "gpt-4", 1700000003000L),
            ChatMessage(MessageRole.ERROR, "Network timeout", null, 1700000004000L),
        )
    }

    // --- Markdown Export Tests ---

    fun testMarkdownExportContainsHeader() {
        val messages = createTestMessages()
        val md = exportAsMarkdown(messages, "TestProject")
        assertTrue("Markdown should contain header", md.contains("# AskSage Conversation"))
    }

    fun testMarkdownExportContainsProjectName() {
        val messages = createTestMessages()
        val md = exportAsMarkdown(messages, "MyProject")
        assertTrue("Markdown should contain project name", md.contains("MyProject"))
    }

    fun testMarkdownExportContainsExportDate() {
        val messages = createTestMessages()
        val md = exportAsMarkdown(messages, "TestProject")
        assertTrue("Markdown should contain 'Exported:'", md.contains("Exported:"))
    }

    fun testMarkdownExportContainsUserMessages() {
        val messages = createTestMessages()
        val md = exportAsMarkdown(messages, "TestProject")
        assertTrue(md.contains("Hello"))
        assertTrue(md.contains("How are you?"))
    }

    fun testMarkdownExportContainsAssistantMessages() {
        val messages = createTestMessages()
        val md = exportAsMarkdown(messages, "TestProject")
        assertTrue(md.contains("Hi there!"))
        assertTrue(md.contains("I'm good!"))
    }

    fun testMarkdownExportContainsModelTag() {
        val messages = createTestMessages()
        val md = exportAsMarkdown(messages, "TestProject")
        assertTrue("Should contain model tag", md.contains("[gpt-4]"))
    }

    fun testMarkdownExportContainsErrorMessages() {
        val messages = createTestMessages()
        val md = exportAsMarkdown(messages, "TestProject")
        assertTrue(md.contains("Error"))
        assertTrue(md.contains("Network timeout"))
    }

    fun testMarkdownExportContainsTimestamps() {
        val messages = createTestMessages()
        val md = exportAsMarkdown(messages, "TestProject")
        // Timestamps should be in parentheses
        assertTrue("Should contain timestamp formatting", md.contains("*("))
    }

    fun testMarkdownExportUserSectionHeaders() {
        val messages = createTestMessages()
        val md = exportAsMarkdown(messages, "TestProject")
        assertTrue("Should have 'You' headers", md.contains("## You"))
    }

    fun testMarkdownExportAssistantSectionHeaders() {
        val messages = createTestMessages()
        val md = exportAsMarkdown(messages, "TestProject")
        assertTrue("Should have 'AskSage' headers", md.contains("## AskSage"))
    }

    // --- JSON Export Tests ---

    fun testJsonExportIsValidJson() {
        val messages = createTestMessages()
        val json = exportAsJson(messages, "TestProject")
        val parsed = gson.fromJson(json, JsonObject::class.java)
        assertNotNull("JSON should be parseable", parsed)
    }

    fun testJsonExportContainsProjectName() {
        val messages = createTestMessages()
        val json = exportAsJson(messages, "TestProject")
        val parsed = gson.fromJson(json, JsonObject::class.java)
        assertEquals("TestProject", parsed.get("project").asString)
    }

    fun testJsonExportContainsExportedAt() {
        val messages = createTestMessages()
        val json = exportAsJson(messages, "TestProject")
        val parsed = gson.fromJson(json, JsonObject::class.java)
        assertTrue("Should have exportedAt field", parsed.has("exportedAt"))
        assertTrue(parsed.get("exportedAt").asString.isNotBlank())
    }

    fun testJsonExportContainsMessagesArray() {
        val messages = createTestMessages()
        val json = exportAsJson(messages, "TestProject")
        val parsed = gson.fromJson(json, JsonObject::class.java)
        assertTrue("Should have messages array", parsed.has("messages"))
        val msgArray = parsed.getAsJsonArray("messages")
        assertEquals(5, msgArray.size())
    }

    fun testJsonExportMessageFields() {
        val messages = createTestMessages()
        val json = exportAsJson(messages, "TestProject")
        val parsed = gson.fromJson(json, JsonObject::class.java)
        val firstMsg = parsed.getAsJsonArray("messages").get(0).asJsonObject
        assertTrue("Message should have 'role'", firstMsg.has("role"))
        assertTrue("Message should have 'content'", firstMsg.has("content"))
        assertTrue("Message should have 'timestamp'", firstMsg.has("timestamp"))
        assertEquals("user", firstMsg.get("role").asString)
        assertEquals("Hello", firstMsg.get("content").asString)
    }

    fun testJsonExportMessageRoles() {
        val messages = createTestMessages()
        val json = exportAsJson(messages, "TestProject")
        val parsed = gson.fromJson(json, JsonObject::class.java)
        val msgArray = parsed.getAsJsonArray("messages")
        assertEquals("user", msgArray.get(0).asJsonObject.get("role").asString)
        assertEquals("assistant", msgArray.get(1).asJsonObject.get("role").asString)
        assertEquals("error", msgArray.get(4).asJsonObject.get("role").asString)
    }

    fun testJsonExportMessageModel() {
        val messages = createTestMessages()
        val json = exportAsJson(messages, "TestProject")
        val parsed = gson.fromJson(json, JsonObject::class.java)
        val secondMsg = parsed.getAsJsonArray("messages").get(1).asJsonObject
        assertEquals("gpt-4", secondMsg.get("model").asString)
    }

    fun testJsonExportMessageTimestamp() {
        val messages = createTestMessages()
        val json = exportAsJson(messages, "TestProject")
        val parsed = gson.fromJson(json, JsonObject::class.java)
        val firstMsg = parsed.getAsJsonArray("messages").get(0).asJsonObject
        val ts = firstMsg.get("timestamp").asString
        assertTrue("Timestamp should contain 'T'", ts.contains("T"))
    }

    fun testJsonExportEmptyMessages() {
        val json = exportAsJson(emptyList(), "TestProject")
        val parsed = gson.fromJson(json, JsonObject::class.java)
        assertEquals(0, parsed.getAsJsonArray("messages").size())
    }

    // --- Helper methods that mirror ChatPanel export logic ---

    private fun exportAsMarkdown(messages: List<ChatMessage>, projectName: String): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return buildString {
            appendLine("# AskSage Conversation")
            appendLine("*Exported: ${dateFormat.format(java.util.Date())}*")
            appendLine("*Project: $projectName*")
            appendLine()
            for (msg in messages) {
                val ts = dateFormat.format(java.util.Date(msg.timestamp))
                when (msg.role) {
                    MessageRole.USER -> {
                        appendLine("## You *($ts)*")
                        appendLine(msg.content)
                        appendLine()
                    }
                    MessageRole.ASSISTANT -> {
                        val modelTag = if (msg.model != null) " [${msg.model}]" else ""
                        appendLine("## AskSage$modelTag *($ts)*")
                        appendLine(msg.content)
                        appendLine()
                    }
                    MessageRole.ERROR -> {
                        appendLine("> **Error** *($ts)*: ${msg.content}")
                        appendLine()
                    }
                }
            }
        }
    }

    private fun exportAsJson(messages: List<ChatMessage>, projectName: String): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val export = mapOf(
            "project" to projectName,
            "exportedAt" to dateFormat.format(java.util.Date()),
            "messages" to messages.map { msg ->
                mapOf(
                    "role" to msg.role.name.lowercase(),
                    "content" to msg.content,
                    "model" to msg.model,
                    "timestamp" to dateFormat.format(java.util.Date(msg.timestamp)),
                )
            },
        )
        return com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(export)
    }
}
