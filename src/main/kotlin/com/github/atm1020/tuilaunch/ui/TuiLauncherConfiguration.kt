package com.github.atm1020.tuilaunch.ui

import com.github.atm1020.tuilaunch.model.TuiAppConfig
import com.github.atm1020.tuilaunch.services.TuiLauncherSettings
import com.github.atm1020.tuilaunchmodel.TuiAppTableModel
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel


class TuiLauncherConfiguration : Configurable {
    private var tuiLauncherPanel: JPanel? = null
    private var tableModel: TuiAppTableModel? = null
    private val settings = TuiLauncherSettings.getInstance()

    private var modifierCombo: JComboBox<String>? = null
    /** The captured prefix key (an AWT [KeyEvent] VK_ code), combined with the modifier. */
    private var escapeKeyCode: Int = settings.state.escapeKeyCode
    private var focusEditorKeyCode: Int = settings.state.focusEditorKeyCode
    private var closeTuiKeyCode: Int = settings.state.closeTuiKeyCode
    private var nextTuiKeyCode: Int = settings.state.nextTuiKeyCode
    private var previousTuiKeyCode: Int = settings.state.previousTuiKeyCode

    override fun getDisplayName(): String = "TUI Launcher"

    override fun createComponent(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))

        panel.add(createFocusShortcutPanel(), BorderLayout.NORTH)

        tableModel = TuiAppTableModel(settings.state.tuiApps)
        val table = JBTable(tableModel).apply {
            fillsViewportHeight = true
            preferredScrollableViewportSize = Dimension(500, 200)

        }

        val scrollPane = JBScrollPane(table)

        val buttonPanel = JPanel().apply {
            add(
                JButton("Add").apply {
                    addActionListener {
                        tableModel?.addRow(TuiAppConfig())
                    }
                })

            add(JButton("Remove").apply {
                addActionListener {
                    val selectedRow = table.selectedRow
                    if (selectedRow >= 0) {
                        val actionId = tableModel?.getValueAt(selectedRow, 3).toString()
                        tableModel?.removeRow(selectedRow)
                        ActionManager.getInstance().unregisterAction(actionId)
                    }
                }
            })
        }

        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)

        tuiLauncherPanel = panel
        return panel
    }

    private fun createFocusShortcutPanel(): JComponent {
        val combo = JComboBox(arrayOf("Ctrl", "Alt")).apply {
            selectedItem = if (settings.state.escapeModifier == "ALT") "Alt" else "Ctrl"
        }
        modifierCombo = combo

        val escapeField = captureField(escapeKeyCode) { escapeKeyCode = it }
        val focusEditorField = captureField(focusEditorKeyCode) { focusEditorKeyCode = it }
        val closeTuiField = captureField(closeTuiKeyCode) { closeTuiKeyCode = it }
        val nextTuiField = captureField(nextTuiKeyCode) { nextTuiKeyCode = it }
        val previousTuiField = captureField(previousTuiKeyCode) { previousTuiKeyCode = it }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(shortcutRow(JBLabel("Escape combo:"), combo, JBLabel("+"), escapeField))
            add(shortcutRow(JBLabel("Focus editor:"), focusEditorField))
            add(shortcutRow(JBLabel("Close:"), closeTuiField))
            add(shortcutRow(JBLabel("Next:"), nextTuiField))
            add(shortcutRow(JBLabel("Previous:"), previousTuiField))
        }
    }

    private fun shortcutRow(vararg components: JComponent): JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
        components.forEach { add(it) }
    }

    /** A read-only field that records the next non-modifier key pressed while it has focus. */
    private fun captureField(initialKeyCode: Int, onCapture: (Int) -> Unit): JBTextField =
        JBTextField(KeyEvent.getKeyText(initialKeyCode), 10).apply {
            isEditable = false
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (isModifierKey(e.keyCode)) return
                    onCapture(e.keyCode)
                    text = KeyEvent.getKeyText(e.keyCode)
                    e.consume()
                }
            })
        }

    private fun isModifierKey(keyCode: Int): Boolean = keyCode == KeyEvent.VK_CONTROL ||
        keyCode == KeyEvent.VK_ALT ||
        keyCode == KeyEvent.VK_SHIFT ||
        keyCode == KeyEvent.VK_META

    override fun isModified(): Boolean = true

    override fun apply() {
        settings.state.escapeModifier = if (modifierCombo?.selectedItem == "Alt") "ALT" else "CTRL"
        settings.state.escapeKeyCode = escapeKeyCode
        settings.state.focusEditorKeyCode = focusEditorKeyCode
        settings.state.closeTuiKeyCode = closeTuiKeyCode
        settings.state.nextTuiKeyCode = nextTuiKeyCode
        settings.state.previousTuiKeyCode = previousTuiKeyCode
        settings.loadActions()
    }
}
