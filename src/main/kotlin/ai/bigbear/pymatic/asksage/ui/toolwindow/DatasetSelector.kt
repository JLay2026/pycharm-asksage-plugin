package ai.bigbear.pymatic.asksage.ui.toolwindow

import com.intellij.openapi.ui.ComboBox
import java.awt.Component
import java.awt.Dimension
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class DatasetSelector(
    private val onDatasetChanged: (String?) -> Unit,
) : ComboBox<String?>() {

    private val comboModel = DefaultComboBoxModel<String?>()

    init {
        model = comboModel
        renderer = DatasetCellRenderer()
        preferredSize = Dimension(160, 28)
        isEditable = false
        comboModel.addElement(null) // "None" option
        addActionListener {
            val selected = selectedItem as? String
            onDatasetChanged(selected)
        }
    }

    fun updateDatasets(datasets: List<String>) {
        val previousSelection = selectedItem as? String
        comboModel.removeAllElements()
        comboModel.addElement(null) // "None" option
        for (dataset in datasets) {
            comboModel.addElement(dataset)
        }
        if (previousSelection != null && datasets.contains(previousSelection)) {
            selectedItem = previousSelection
        }
    }

    private class DatasetCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is String) {
                // Shorten long dataset names for display
                text = if (value.length > 40) "${value.take(37)}..." else value
                toolTipText = value
            } else {
                text = "(None)"
                toolTipText = "No dataset selected"
            }
            return this
        }
    }
}
