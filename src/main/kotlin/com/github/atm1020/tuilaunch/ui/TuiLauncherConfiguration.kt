package com.github.atm1020.tuilaunch.ui

import com.github.atm1020.tuilaunch.model.TuiAppConfig
import com.github.atm1020.tuilaunch.services.TuiLauncherSettings
import com.github.atm1020.tuilaunchmodel.TuiAppTableModel
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.ToolWindowType
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ItemEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel


class TuiLauncherConfiguration : Configurable {
    private var tuiLauncherPanel: JPanel? = null
    private var tableModel: TuiAppTableModel? = null
    private val settings = TuiLauncherSettings.getInstance()

    override fun getDisplayName(): String = "TUI Launcher"

    override fun createComponent(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))

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

        val dropdownPanel = JPanel(BorderLayout(5, 5))
        val comboBoxLabel = JBLabel("Select a terminal view mode for TUI application launch:")
        val selectToolWindowTypes = SelectToolWindowType.entries.toTypedArray()
        val optionsComboBox = ComboBox(selectToolWindowTypes)
        optionsComboBox.selectedItem = selectToolWindowTypes.find {
            it == settings.state.terminalViewMode
        }
        dropdownPanel.add(comboBoxLabel, BorderLayout.NORTH);
        dropdownPanel.add(optionsComboBox, BorderLayout.CENTER);
        optionsComboBox.addItemListener { e: ItemEvent ->
            if (e.stateChange == ItemEvent.SELECTED) {
                this.settings.state.terminalViewMode = e.item as SelectToolWindowType
            }
        }

        panel.add(dropdownPanel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)

        tuiLauncherPanel = panel
        return panel
    }

    override fun isModified(): Boolean = true

    override fun apply() {
        settings.loadActions()
    }

    enum class SelectToolWindowType(val show: String, val type: ToolWindowType?) {
        DOCKED("Dock to IDE window edge", ToolWindowType.DOCKED),
        FLOATING("Float as separate window", ToolWindowType.FLOATING),
        SLIDING("Slide over editor area", ToolWindowType.SLIDING),
        WINDOWED("Show in detached window", ToolWindowType.WINDOWED),
        DEFAULT("Use current window mode", null);

        @Override
        override fun toString(): String {
            return this.show
        }

    }
}
