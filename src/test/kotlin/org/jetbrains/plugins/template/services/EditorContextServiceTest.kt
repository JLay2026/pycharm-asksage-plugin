package org.jetbrains.plugins.template.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class EditorContextServiceTest : BasePlatformTestCase() {

    fun testDetectLanguageKotlin() {
        val lang = EditorContextService.detectLanguageFromFileName("Main.kt")
        assertEquals("Kotlin", lang)
    }

    fun testDetectLanguageJava() {
        val lang = EditorContextService.detectLanguageFromFileName("App.java")
        assertEquals("Java", lang)
    }

    fun testDetectLanguagePython() {
        val lang = EditorContextService.detectLanguageFromFileName("script.py")
        assertEquals("Python", lang)
    }

    fun testDetectLanguageTypeScript() {
        val lang = EditorContextService.detectLanguageFromFileName("component.tsx")
        assertEquals("TypeScript (React)", lang)
    }

    fun testDetectLanguageUnknown() {
        val lang = EditorContextService.detectLanguageFromFileName("data.xyz")
        assertEquals("xyz", lang)
    }

    fun testDetectLanguageNoExtension() {
        val lang = EditorContextService.detectLanguageFromFileName("Dockerfile")
        assertEquals("Unknown", lang)
    }

    fun testBuildFileContextPromptWithSelection() {
        val context = EditorContext(
            fileName = "App.kt",
            filePath = "/src/App.kt",
            language = "Kotlin",
            selectedText = "fun main() {}",
            fullContent = "package test\nfun main() {}",
            selectionStartLine = 2,
            selectionEndLine = 2,
        )
        val prompt = EditorContextService.buildFileContextPrompt(context)
        assertTrue(prompt.contains("File: App.kt (Kotlin)"))
        assertTrue(prompt.contains("Selected code (lines 2-2):"))
        assertTrue(prompt.contains("fun main() {}"))
        assertTrue(prompt.contains("```kotlin"))
    }

    fun testBuildFileContextPromptWithoutSelection() {
        val context = EditorContext(
            fileName = "App.kt",
            filePath = "/src/App.kt",
            language = "Kotlin",
            selectedText = null,
            fullContent = "package test\nfun main() {}",
            selectionStartLine = null,
            selectionEndLine = null,
        )
        val prompt = EditorContextService.buildFileContextPrompt(context)
        assertTrue(prompt.contains("File: App.kt (Kotlin)"))
        assertTrue(prompt.contains("Full file content:"))
        assertTrue(prompt.contains("package test"))
    }
}
