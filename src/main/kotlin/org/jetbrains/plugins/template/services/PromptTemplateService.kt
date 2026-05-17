package org.jetbrains.plugins.template.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(
    name = "org.jetbrains.plugins.template.services.PromptTemplateService",
    storages = [Storage("PymaticAskSagePromptTemplates.xml")]
)
class PromptTemplateService : PersistentStateComponent<PromptTemplateService> {

    var customTemplateNames: MutableList<String> = mutableListOf()
    var customTemplatePrompts: MutableList<String> = mutableListOf()

    override fun getState(): PromptTemplateService = this

    override fun loadState(state: PromptTemplateService) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun getBuiltInTemplates(): List<PromptTemplate> = BUILT_IN_TEMPLATES

    fun getCustomTemplates(): List<PromptTemplate> {
        return customTemplateNames.zip(customTemplatePrompts).map { (name, prompt) ->
            PromptTemplate(name, prompt, builtin = false)
        }
    }

    fun getAllTemplates(): List<PromptTemplate> {
        return getBuiltInTemplates() + getCustomTemplates()
    }

    fun addCustomTemplate(name: String, prompt: String) {
        customTemplateNames.add(name)
        customTemplatePrompts.add(prompt)
    }

    fun removeCustomTemplate(name: String) {
        val idx = customTemplateNames.indexOf(name)
        if (idx >= 0) {
            customTemplateNames.removeAt(idx)
            customTemplatePrompts.removeAt(idx)
        }
    }

    companion object {
        fun getInstance(): PromptTemplateService {
            return ApplicationManager.getApplication().getService(PromptTemplateService::class.java)
        }

        private val BUILT_IN_TEMPLATES = listOf(
            PromptTemplate(
                name = "(None)",
                systemPrompt = "",
                builtin = true,
            ),
            PromptTemplate(
                name = "Code Review",
                systemPrompt = "You are a senior code reviewer. Analyze the provided code for bugs, " +
                    "security vulnerabilities, performance issues, and adherence to best practices. " +
                    "Provide specific, actionable feedback with line references where applicable.",
                builtin = true,
            ),
            PromptTemplate(
                name = "Security Audit",
                systemPrompt = "You are a cybersecurity expert specializing in application security. " +
                    "Analyze the provided code or system description for security vulnerabilities " +
                    "including OWASP Top 10, injection attacks, authentication flaws, data exposure, " +
                    "and insecure configurations. Rate each finding by severity (Critical/High/Medium/Low).",
                builtin = true,
            ),
            PromptTemplate(
                name = "Performance Analysis",
                systemPrompt = "You are a performance engineering expert. Analyze the provided code " +
                    "for performance bottlenecks, memory leaks, inefficient algorithms, and scalability " +
                    "concerns. Suggest optimizations with estimated impact and trade-offs.",
                builtin = true,
            ),
            PromptTemplate(
                name = "Unit Test Generator",
                systemPrompt = "You are a testing expert. Generate comprehensive unit tests for the " +
                    "provided code. Cover edge cases, boundary conditions, error paths, and happy paths. " +
                    "Use the testing framework appropriate for the language. Include clear test names " +
                    "that describe the behavior being tested.",
                builtin = true,
            ),
            PromptTemplate(
                name = "Architecture Review",
                systemPrompt = "You are a software architect. Analyze the provided code or design for " +
                    "architectural concerns including separation of concerns, coupling, cohesion, " +
                    "SOLID principles, and design patterns. Suggest improvements with rationale.",
                builtin = true,
            ),
            PromptTemplate(
                name = "Documentation Writer",
                systemPrompt = "You are a technical writer. Generate clear, comprehensive documentation " +
                    "for the provided code including purpose, parameters, return values, exceptions, " +
                    "usage examples, and any important notes. Follow the documentation conventions " +
                    "of the programming language.",
                builtin = true,
            ),
            PromptTemplate(
                name = "Explain Like I'm Junior",
                systemPrompt = "You are a patient senior developer mentoring a junior. Explain the " +
                    "provided code in simple terms, breaking down complex concepts. Use analogies " +
                    "where helpful. Highlight patterns and idioms that are important to understand.",
                builtin = true,
            ),
        )
    }
}

data class PromptTemplate(
    val name: String,
    val systemPrompt: String,
    val builtin: Boolean = true,
)
