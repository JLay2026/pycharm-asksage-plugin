package org.jetbrains.plugins.template.ui.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import org.jetbrains.plugins.template.services.AskSageSettingsState
import org.jetbrains.plugins.template.util.LiveMode
import java.awt.event.MouseEvent

class AskSageStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = WIDGET_ID
    override fun getDisplayName(): String = "AskSage Status"
    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return AskSageStatusBarWidget(project)
    }

    companion object {
        const val WIDGET_ID = "AskSageStatus"
    }
}

class AskSageStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {

    private var statusBar: StatusBar? = null

    override fun ID(): String = AskSageStatusBarWidgetFactory.WIDGET_ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        statusBar = null
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String {
        val settings = AskSageSettingsState.getInstance()
        val mode = LiveMode.fromValue(settings.defaultLiveMode)
        val model = settings.defaultModel.ifBlank { "No model" }
        return "AskSage: ${mode.displayName} | $model"
    }

    override fun getTooltipText(): String {
        val settings = AskSageSettingsState.getInstance()
        val mode = LiveMode.fromValue(settings.defaultLiveMode)
        return "AskSage - ${mode.displayName}: ${mode.description}"
    }

    override fun getAlignment(): Float = 0f

    override fun getClickConsumer(): Consumer<MouseEvent>? = null

    fun update() {
        statusBar?.updateWidget(AskSageStatusBarWidgetFactory.WIDGET_ID)
    }
}
