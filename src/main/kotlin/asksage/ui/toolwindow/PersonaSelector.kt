package asksage.ui.toolwindow

import com.intellij.openapi.ui.ComboBox
import asksage.api.models.PersonaInfo
import java.awt.Component
import java.awt.Dimension
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class PersonaSelector(
    private val onPersonaChanged: (Int?) -> Unit,
) : ComboBox<PersonaInfo?>() {

    private val comboModel = DefaultComboBoxModel<PersonaInfo?>()

    init {
        model = comboModel
        renderer = PersonaCellRenderer()
        preferredSize = Dimension(160, 28)
        isEditable = false
        comboModel.addElement(null) // "None" option
        addActionListener {
            val selected = selectedItem as? PersonaInfo
            onPersonaChanged(selected?.id)
        }
    }

    fun updatePersonas(personas: List<PersonaInfo>) {
        val previousSelection = (selectedItem as? PersonaInfo)?.id
        comboModel.removeAllElements()
        comboModel.addElement(null) // "None" option
        for (persona in personas) {
            comboModel.addElement(persona)
        }
        if (previousSelection != null) {
            val match = personas.find { it.id == previousSelection }
            if (match != null) {
                selectedItem = match
            }
        }
    }

    fun setSelectedPersonaId(personaId: Int) {
        for (i in 0 until comboModel.size) {
            val item = comboModel.getElementAt(i)
            if (item?.id == personaId) {
                selectedItem = item
                return
            }
        }
    }

    private class PersonaCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is PersonaInfo) {
                text = value.name
                toolTipText = value.description ?: ""
            } else {
                text = "(None)"
                toolTipText = "No persona selected"
            }
            return this
        }
    }
}
