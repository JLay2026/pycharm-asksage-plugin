import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.2.6.2")
        testFramework(TestFrameworkType.Platform)
    }
}

changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

// Render change notes for the current version from CHANGELOG.md eagerly at
// configuration time. Doing this inside a Provider lambda captures the
// changelog extension (and transitively the Project), which cannot be
// serialized to the Gradle configuration cache (fails on Gradle 9.5+).
// The resulting plain String is configuration-cache safe; Gradle tracks
// CHANGELOG.md as a configuration input so the cache invalidates on change.
val renderedChangeNotes = with(changelog) {
    renderItem(
        (getOrNull(providers.gradleProperty("version").get()) ?: getUnreleased())
            .withHeader(false)
            .withEmptySections(false),
        Changelog.OutputType.HTML,
    )
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set("252")
        }

        // Marketplace requires change notes; patched into plugin.xml by patchPluginXml.
        changeNotes.set(renderedChangeNotes)
    }

    signing {
        certificateChainFile.set(
            providers.environmentVariable("CERTIFICATE_CHAIN").map { layout.projectDirectory.file(it) },
        )
        privateKeyFile.set(
            providers.environmentVariable("PRIVATE_KEY").map { layout.projectDirectory.file(it) },
        )
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        channels.set(listOf(version.toString().let { if (it.contains("-")) "beta" else "default" }))
    }
}
