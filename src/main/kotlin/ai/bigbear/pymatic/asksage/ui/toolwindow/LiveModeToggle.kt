package ai.bigbear.pymatic.asksage.ui.toolwindow

import com.intellij.ui.JBColor
import ai.bigbear.pymatic.asksage.util.LiveMode
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionListener
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JPanel
import javax.swing.JToggleButton

class LiveModeToggle(
    initialMode: LiveMode = LiveMode.NO_LIVE,
    private val onModeChanged: (LiveMode) -> Unit,
) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {

    private val buttons = mutableMapOf<LiveMode, JToggleButton>()
    private val buttonGroup = ButtonGroup()
    var selectedMode: LiveMode = initialMode
        private set

    init {
        for (mode in LiveMode.entries) {
            val button = createModeButton(mode)
            buttons[mode] = button
            buttonGroup.add(button)
            add(button)
        }
        buttons[initialMode]?.isSelected = true
        updateButtonStyles()
    }

    private fun createModeButton(mode: LiveMode): JToggleButton {
        return JToggleButton(mode.displayName).apply {
            toolTipText = mode.description
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            isFocusPainted = false
            preferredSize = Dimension(80, 28)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
            )
            addActionListener(ActionListener {
                selectedMode = mode
                updateButtonStyles()
                onModeChanged(mode)
            })
        }
    }

    private fun updateButtonStyles() {
        for ((mode, button) in buttons) {
            if (mode == selectedMode) {
                button.background = getColorForMode(mode)
                button.foreground = JBColor.WHITE
            } else {
                button.background = JBColor.PanelBackground
                button.foreground = JBColor.foreground()
            }
        }
    }

    private fun getColorForMode(mode: LiveMode): JBColor {
        return when (mode) {
            LiveMode.NO_LIVE -> MODE_GRAY
            LiveMode.LIVE -> MODE_GREEN
            LiveMode.LIVE_PLUS -> MODE_BLUE
        }
    }

    companion object {
        private val MODE_GRAY = JBColor(0x6C757D, 0x6C757D)
        private val MODE_GREEN = JBColor(0x28A745, 0x28A745)
        private val MODE_BLUE = JBColor(0x007BFF, 0x007BFF)
    }

    fun setMode(mode: LiveMode) {
        selectedMode = mode
        buttons[mode]?.isSelected = true
        updateButtonStyles()
    }
}
