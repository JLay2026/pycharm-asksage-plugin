package asksage.ui.toolwindow

import com.intellij.openapi.ui.ComboBox
import asksage.api.models.ModelInfo
import java.awt.Component
import java.awt.Dimension
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class ModelSelector(
    private val onModelChanged: (String) -> Unit,
) : ComboBox<ModelInfo>() {

    private val comboModel = DefaultComboBoxModel<ModelInfo>()

    init {
        model = comboModel
        renderer = ModelCellRenderer()
        preferredSize = Dimension(200, 28)
        isEditable = false
        addActionListener {
            val selected = selectedItem as? ModelInfo
            if (selected != null) {
                onModelChanged(selected.id)
            }
        }
    }

    fun updateModels(models: List<ModelInfo>) {
        val previousSelection = (selectedItem as? ModelInfo)?.id
        comboModel.removeAllElements()
        for (model in models) {
            comboModel.addElement(model)
        }
        // Restore previous selection if possible
        if (previousSelection != null) {
            val match = models.find { it.id == previousSelection }
            if (match != null) {
                selectedItem = match
            }
        }
    }

    fun setSelectedModelId(modelId: String) {
        for (i in 0 until comboModel.size) {
            val item = comboModel.getElementAt(i)
            if (item.id == modelId) {
                selectedItem = item
                return
            }
        }
    }

    private class ModelCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is ModelInfo) {
                text = value.id
                toolTipText = "Provider: ${value.ownedBy ?: "Unknown"}"
            }
            return this
        }
    }
}
