package com.github.atm1020.tuilaunch.ui

import com.github.atm1020.tuilaunch.model.TuiAppConfig
import com.github.atm1020.tuilaunch.services.TuiLauncherSettings
import com.github.atm1020.tuilaunchmodel.TuiAppTableModel
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.TableModelEvent
import javax.swing.table.AbstractTableModel


class TuiLauncherConfiguration : Configurable {
    private companion object {
        const val BUILT_IN_SHORTCUT_COUNT = 8
    }

    private var tuiLauncherPanel: JPanel? = null
    private var tableModel: TuiAppTableModel? = null
    private var appsTable: JBTable? = null
    private val settings = TuiLauncherSettings.getInstance()

    private var tmuxKeybindingsEnabledCheckBox: JBCheckBox? = null
    private var modifierCombo: JComboBox<String>? = null
    private val tmuxShortcutComponents = mutableListOf<JComponent>()
    private var shortcutsTable: JBTable? = null
    private var shortcutsModel: ShortcutTableModel? = null

    /** The captured prefix key (an AWT [KeyEvent] VK_ code), combined with the modifier. */
    private var escapeKeyCode: Int? = settings.state.escapeKeyCode
    private var focusEditorKeyCode: Int? = settings.state.focusEditorKeyCode
    private var closeTuiKeyCode: Int? = settings.state.closeTuiKeyCode
    private var nextTuiKeyCode: Int? = settings.state.nextTuiKeyCode
    private var previousTuiKeyCode: Int? = settings.state.previousTuiKeyCode
    private var toggleToolWindowKeyCode: Int? = settings.state.toggleToolWindowKeyCode
    private var nextTuiWithoutFocusKeyCode: Int? = settings.state.nextTuiWithoutFocusKeyCode
    private var previousTuiWithoutFocusKeyCode: Int? = settings.state.previousTuiWithoutFocusKeyCode

    override fun getDisplayName(): String = "TUI Launcher"

    override fun createComponent(): JComponent {
        tmuxShortcutComponents.clear()

        val panel = JPanel(BorderLayout(0, 12)).apply {
            border = JBUI.Borders.empty(10)
        }

        tableModel = TuiAppTableModel(settings.state.tuiApps.map { it.copy() }.toMutableList())
        val table = createAppsTable()
        val tablePanel = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                tableModel?.addRow(TuiAppConfig())
                refreshShortcutBindings()
                selectLastShortcutRow()
            }
            .setRemoveAction {
                val selectedRow = table.selectedRow
                if (selectedRow >= 0) {
                    val modelRow = table.convertRowIndexToModel(selectedRow)
                    tableModel?.removeRow(modelRow)
                    refreshShortcutBindings()
                }
            }
            .disableUpDownActions()
            .createPanel()
            .apply {
                preferredSize = Dimension(640, 250)
            }

        panel.add(tablePanel, BorderLayout.CENTER)
        panel.add(createTmuxKeybindingsPanel(), BorderLayout.SOUTH)

        tuiLauncherPanel = panel
        return panel
    }

    private fun createAppsTable(): JBTable = JBTable(tableModel).apply {
        appsTable = this
        fillsViewportHeight = true
        preferredScrollableViewportSize = Dimension(620, 210)
        rowHeight = JBUI.scale(28)
        setShowGrid(false)
        intercellSpacing = Dimension(0, 0)
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        selectionModel.addListSelectionListener { event ->
            if (!event.valueIsAdjusting && tmuxKeybindingsEnabledCheckBox?.isSelected == true) {
                selectShortcutRowForSelectedApp(requestFocus = false)
            }
        }
        tableHeader.reorderingAllowed = false
        emptyText.text = "Add TUI applications to create IDE actions"
        model.addTableModelListener { event ->
            if (event.type != TableModelEvent.UPDATE || event.column == 0 || event.column == 1) {
                refreshShortcutBindings()
            }
        }
    }

    private fun createTmuxKeybindingsPanel(): JComponent {
        val combo = JComboBox(arrayOf("Ctrl", "Alt")).apply {
            selectedItem = if (settings.state.escapeModifier == "ALT") "Alt" else "Ctrl"
        }
        modifierCombo = combo

        val enabledCheckBox = JBCheckBox("Enable tmux-like prefix keybindings", settings.state.tmuxKeybindingsEnabled)
        tmuxKeybindingsEnabledCheckBox = enabledCheckBox

        val model = ShortcutTableModel(createShortcutBindings())
        shortcutsModel = model

        val table = JBTable(model).apply {
            shortcutsTable = this
            rowHeight = JBUI.scale(26)
            fillsViewportHeight = true
            tableHeader.reorderingAllowed = false
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            preferredScrollableViewportSize = Dimension(620, 170)
            emptyText.text = "Select an action and press a key to assign a shortcut"
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (isModifierKey(e.keyCode)) return
                    if (e.keyCode == KeyEvent.VK_DELETE || e.keyCode == KeyEvent.VK_BACK_SPACE) {
                        clearSelectedShortcut()
                    } else {
                        assignSelectedShortcut(e.keyCode)
                    }
                    e.consume()
                }
            })
        }

        val recordButton = JButton("Record Shortcut").apply {
            addActionListener {
                if (shortcutsTable?.selectedRow == -1) {
                    selectShortcutRowForSelectedApp(requestFocus = false)
                }
                shortcutsTable?.requestFocusInWindow()
            }
        }
        val clearButton = JButton("Remove Shortcut").apply {
            addActionListener { clearSelectedShortcut() }
        }
        combo.addActionListener { shortcutsModel?.fireTableDataChanged() }

        val shortcutPanel = JPanel(BorderLayout(8, 6)).apply {
            border = BorderFactory.createTitledBorder("Tmux-like keybindings")
            add(shortcutRow(JBLabel("Prefix modifier:"), combo, JBLabel("Select a row, then press a key. Delete/Backspace clears.")), BorderLayout.NORTH)
            add(JBScrollPane(table), BorderLayout.CENTER)
            add(shortcutRow(recordButton, clearButton), BorderLayout.SOUTH)
        }

        tmuxShortcutComponents.addAll(listOf(combo, table, recordButton, clearButton))
        enabledCheckBox.addActionListener { updateTmuxShortcutComponentsEnabled() }
        updateTmuxShortcutComponentsEnabled()

        return JPanel(BorderLayout(0, 6)).apply {
            add(enabledCheckBox, BorderLayout.NORTH)
            add(shortcutPanel, BorderLayout.CENTER)
        }
    }

    private fun assignSelectedShortcut(keyCode: Int) {
        val row = shortcutsTable?.selectedRow ?: return
        val modelRow = shortcutsTable?.convertRowIndexToModel(row) ?: return
        val model = shortcutsModel ?: return
        val conflictRow = model.findConflict(modelRow, keyCode)
        if (conflictRow != null) {
            Messages.showErrorDialog(
                "Shortcut '${model.shortcutText(modelRow, keyCode)}' is already assigned to '${model.actionName(conflictRow)}'. Remove that shortcut first.",
                "Shortcut Already Assigned",
            )
            return
        }
        model.setShortcut(modelRow, keyCode)
    }

    private fun clearSelectedShortcut() {
        val row = shortcutsTable?.selectedRow ?: return
        val modelRow = shortcutsTable?.convertRowIndexToModel(row) ?: return
        shortcutsModel?.setShortcut(modelRow, null)
    }

    private fun updateTmuxShortcutComponentsEnabled() {
        val enabled = tmuxKeybindingsEnabledCheckBox?.isSelected == true
        tmuxShortcutComponents.forEach { it.isEnabled = enabled }
    }

    private fun shortcutRow(vararg components: JComponent): JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2)).apply {
        components.forEach { add(it) }
    }

    private fun createShortcutBindings(): List<ShortcutBinding> = buildList {
        add(ShortcutBinding("Prefix", true, { escapeKeyCode }, { escapeKeyCode = it }))
        add(ShortcutBinding("Focus editor", false, { focusEditorKeyCode }, { focusEditorKeyCode = it }))
        add(ShortcutBinding("Close active TUI", false, { closeTuiKeyCode }, { closeTuiKeyCode = it }))
        add(ShortcutBinding("Next TUI tab", false, { nextTuiKeyCode }, { nextTuiKeyCode = it }))
        add(ShortcutBinding("Previous TUI tab", false, { previousTuiKeyCode }, { previousTuiKeyCode = it }))
        add(ShortcutBinding("Toggle tool window", false, { toggleToolWindowKeyCode }, { toggleToolWindowKeyCode = it }))
        add(ShortcutBinding("Next TUI tab without focus", false, { nextTuiWithoutFocusKeyCode }, { nextTuiWithoutFocusKeyCode = it }))
        add(ShortcutBinding("Previous TUI tab without focus", false, { previousTuiWithoutFocusKeyCode }, { previousTuiWithoutFocusKeyCode = it }))
        val model = tableModel ?: return@buildList
        for (index in 0 until model.rowCount) {
            val app = model.appAt(index)
            val label = when {
                app.name.isNotBlank() -> "Launch ${app.name}"
                app.command.isNotBlank() -> "Launch ${app.command}"
                else -> "Launch app row ${index + 1}"
            }
            add(ShortcutBinding(label, false, { app.shortcutKeyCode }, { app.shortcutKeyCode = it }))
        }
    }

    private fun refreshShortcutBindings() {
        shortcutsModel?.setBindings(createShortcutBindings())
    }

    private fun selectLastShortcutRow() {
        val table = shortcutsTable ?: return
        if (tmuxKeybindingsEnabledCheckBox?.isSelected != true || table.rowCount == 0) return
        selectShortcutRow(table.rowCount - 1, requestFocus = true)
    }

    private fun selectShortcutRowForSelectedApp(requestFocus: Boolean) {
        val appRow = appsTable?.selectedRow ?: return
        if (appRow < 0) return
        val modelRow = appsTable?.convertRowIndexToModel(appRow) ?: return
        selectShortcutRow(BUILT_IN_SHORTCUT_COUNT + modelRow, requestFocus)
    }

    private fun selectShortcutRow(row: Int, requestFocus: Boolean) {
        val table = shortcutsTable ?: return
        if (row !in 0 until table.rowCount) return
        table.selectionModel.setSelectionInterval(row, row)
        table.scrollRectToVisible(table.getCellRect(row, 0, true))
        if (requestFocus) table.requestFocusInWindow()
    }

    private data class ShortcutBinding(
        val actionName: String,
        val includeModifier: Boolean,
        val getKeyCode: () -> Int?,
        val setKeyCode: (Int?) -> Unit,
    )

    private inner class ShortcutTableModel(private var bindings: List<ShortcutBinding>) : AbstractTableModel() {
        override fun getRowCount(): Int = bindings.size
        override fun getColumnCount(): Int = 2
        override fun getColumnName(column: Int): String = if (column == 0) "Action" else "Shortcut"
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val binding = bindings[rowIndex]
            if (columnIndex == 0) return binding.actionName
            val keyCode = binding.getKeyCode() ?: return "Not set"
            val keyText = KeyEvent.getKeyText(keyCode)
            return if (binding.includeModifier) "${modifierCombo?.selectedItem ?: "Ctrl"}+$keyText" else keyText
        }

        fun setShortcut(rowIndex: Int, keyCode: Int?) {
            bindings[rowIndex].setKeyCode(keyCode)
            fireTableRowsUpdated(rowIndex, rowIndex)
        }

        fun setBindings(newBindings: List<ShortcutBinding>) {
            bindings = newBindings
            fireTableDataChanged()
        }

        fun findConflict(rowIndex: Int, keyCode: Int): Int? {
            val binding = bindings[rowIndex]
            val conflictRow = bindings.withIndex().indexOfFirst { (index, other) ->
                index != rowIndex &&
                    other.includeModifier == binding.includeModifier &&
                    other.getKeyCode() == keyCode
            }
            return conflictRow.takeIf { it >= 0 }
        }

        fun actionName(rowIndex: Int): String = bindings[rowIndex].actionName

        fun shortcutText(rowIndex: Int, keyCode: Int): String {
            val keyText = KeyEvent.getKeyText(keyCode)
            return if (bindings[rowIndex].includeModifier) "${modifierCombo?.selectedItem ?: "Ctrl"}+$keyText" else keyText
        }

        fun duplicateShortcutMessage(): String? {
            bindings.forEachIndexed { index, binding ->
                val keyCode = binding.getKeyCode() ?: return@forEachIndexed
                val conflictRow = findConflict(index, keyCode) ?: return@forEachIndexed
                return "Shortcut '${shortcutText(index, keyCode)}' is already assigned to '${actionName(index)}' and '${actionName(conflictRow)}'."
            }
            return null
        }
    }

    private fun isModifierKey(keyCode: Int): Boolean = keyCode == KeyEvent.VK_CONTROL ||
        keyCode == KeyEvent.VK_ALT ||
        keyCode == KeyEvent.VK_SHIFT ||
        keyCode == KeyEvent.VK_META

    override fun isModified(): Boolean = true

    override fun apply() {
        appsTable?.cellEditor?.stopCellEditing()
        validateTuiApps()
        validateShortcuts()

        val newApps = tableModel?.snapshot() ?: mutableListOf()
        unregisterRemovedActions(newApps)
        settings.state.tuiApps = newApps
        settings.state.tmuxKeybindingsEnabled = tmuxKeybindingsEnabledCheckBox?.isSelected == true
        settings.state.escapeModifier = if (modifierCombo?.selectedItem == "Alt") "ALT" else "CTRL"
        settings.state.escapeKeyCode = escapeKeyCode
        settings.state.focusEditorKeyCode = focusEditorKeyCode
        settings.state.closeTuiKeyCode = closeTuiKeyCode
        settings.state.nextTuiKeyCode = nextTuiKeyCode
        settings.state.previousTuiKeyCode = previousTuiKeyCode
        settings.state.toggleToolWindowKeyCode = toggleToolWindowKeyCode
        settings.state.nextTuiWithoutFocusKeyCode = nextTuiWithoutFocusKeyCode
        settings.state.previousTuiWithoutFocusKeyCode = previousTuiWithoutFocusKeyCode
        settings.loadActions()
    }

    private fun unregisterRemovedActions(newApps: List<TuiAppConfig>) {
        val newActionIds = newApps.map { "TUILauncher.${it.name}" }.toSet()
        settings.state.tuiApps
            .map { "TUILauncher.${it.name}" }
            .filterNot { it in newActionIds }
            .forEach { settings.unregisterAction(it) }
    }

    private fun validateShortcuts() {
        shortcutsModel?.duplicateShortcutMessage()?.let { throw ConfigurationException(it) }
    }

    private fun validateTuiApps() {
        tableModel?.snapshot()?.forEachIndexed { index, app ->
            val rowNumber = index + 1
            if (app.name.isBlank()) {
                throw ConfigurationException("TUI app name cannot be empty (row $rowNumber).")
            }
            if (app.command.isBlank()) {
                throw ConfigurationException("TUI app command cannot be empty (row $rowNumber).")
            }
        }
    }
}
