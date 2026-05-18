package org.jetbrains.plugins.template.ui.toolwindow

import org.jetbrains.plugins.template.services.PromptTemplate
import org.jetbrains.plugins.template.services.PromptTemplateService
import java.awt.Component
import java.awt.Dimension
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

class PromptTemplateSelector(
    private val onTemplateChanged: (PromptTemplate?) -> Unit,
) : JComboBox<PromptTemplate>() {

    private val templateService = PromptTemplateService.getInstance()

    init {
        preferredSize = Dimension(160, 28)
        maximumSize = Dimension(200, 28)
        renderer = TemplateRenderer()

        refreshTemplates()

        addActionListener {
            val selected = selectedItem as? PromptTemplate
            val effective = if (selected?.systemPrompt.isNullOrBlank()) null else selected
            onTemplateChanged(effective)
        }
    }

    fun refreshTemplates() {
        val templates = templateService.getAllTemplates()
        model = DefaultComboBoxModel(templates.toTypedArray())
        selectedIndex = 0
    }

    private class TemplateRenderer : ListCellRenderer<PromptTemplate> {
        private val label = JLabel()

        override fun getListCellRendererComponent(
            list: JList<out PromptTemplate>?,
            value: PromptTemplate?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            label.text = value?.name ?: "(None)"
            if (list != null) {
                label.background = if (isSelected) list.selectionBackground else list.background
                label.foreground = if (isSelected) list.selectionForeground else list.foreground
            }
            label.isOpaque = true
            return label
        }
    }
}
