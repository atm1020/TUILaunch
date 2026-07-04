package com.github.atm1020.tuilaunch

import com.github.atm1020.tuilaunch.model.TuiAppConfig
import com.github.atm1020.tuilaunch.services.TuiLauncherSettings
import com.github.atm1020.tuilaunch.ui.TuiLauncherConfiguration
import com.github.atm1020.tuilaunchmodel.TuiAppTableModel
import com.intellij.openapi.options.ConfigurationException
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.BorderLayout
import java.awt.Container
import java.awt.event.KeyEvent
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JTable

class TuiLauncherConfigurationTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        TuiLauncherSettings.getInstance().state.apply {
            tuiApps.clear()
            escapeKeyCode = null
            focusEditorKeyCode = null
            closeTuiKeyCode = null
            nextTuiKeyCode = null
            previousTuiKeyCode = null
            toggleToolWindowKeyCode = null
            nextTuiWithoutFocusKeyCode = null
            previousTuiWithoutFocusKeyCode = null
        }
    }

    fun testKeySelectionPanelUsesKeymapLikeShortcutTable() {
        val component = TuiLauncherConfiguration().createComponent() as JPanel
        val table = findTable(component)!!

        assertEquals("Action", table.columnModel.getColumn(0).headerValue)
        assertEquals("Shortcut", table.columnModel.getColumn(1).headerValue)
        assertEquals("Not set", table.model.getValueAt(0, 1))
    }

    fun testExistingAppsHaveLaunchShortcutRows() {
        val settings = TuiLauncherSettings.getInstance()
        settings.state.tuiApps.add(TuiAppConfig(name = "htop", command = "htop"))

        val component = TuiLauncherConfiguration().createComponent() as JPanel
        val shortcutTable = findTable(component)!!

        assertEquals(9, shortcutTable.rowCount)
        assertEquals("Launch htop", shortcutTable.model.getValueAt(8, 0))
    }

    fun testAddingAppAddsLaunchShortcutRow() {
        val configurable = TuiLauncherConfiguration()
        val component = configurable.createComponent() as JPanel
        val appsTable = findAppsTable(component)!!
        val shortcutTable = findTable(component)!!

        assertEquals(8, shortcutTable.rowCount)
        (appsTable.model as TuiAppTableModel).addRow(TuiAppConfig(name = "lazygit", command = "lazygit"))

        assertEquals(9, shortcutTable.rowCount)
        assertEquals("Launch lazygit", shortcutTable.model.getValueAt(8, 0))
    }

    fun testTmuxKeybindingsCheckboxPersistsEnabledFlag() {
        val settings = TuiLauncherSettings.getInstance()
        settings.state.tmuxKeybindingsEnabled = true
        settings.state.tuiApps.add(TuiAppConfig(name = "lazygit", command = "lazygit"))

        val configurable = TuiLauncherConfiguration()
        val component = configurable.createComponent() as JPanel
        val checkbox = findCheckBox(component, "Enable tmux-like prefix keybindings")!!

        checkbox.isSelected = false
        configurable.apply()

        assertFalse(settings.state.tmuxKeybindingsEnabled)
    }

    fun testApplyRejectsAppShortcutDuplicate() {
        val settings = TuiLauncherSettings.getInstance()
        settings.state.closeTuiKeyCode = KeyEvent.VK_G
        settings.state.tuiApps.add(TuiAppConfig(name = "lazygit", command = "lazygit", shortcutKeyCode = KeyEvent.VK_G))

        val configurable = TuiLauncherConfiguration()
        configurable.createComponent()

        val error = try {
            configurable.apply()
            fail("Expected ConfigurationException")
            return
        } catch (e: ConfigurationException) {
            e
        }
        assertTrue(error.localizedMessage.contains("already assigned"))
    }

    fun testApplyRejectsDuplicateShortcuts() {
        val settings = TuiLauncherSettings.getInstance()
        settings.state.focusEditorKeyCode = KeyEvent.VK_X
        settings.state.closeTuiKeyCode = KeyEvent.VK_X

        val configurable = TuiLauncherConfiguration()
        configurable.createComponent()

        val error = try {
            configurable.apply()
            fail("Expected ConfigurationException")
            return
        } catch (e: ConfigurationException) {
            e
        }
        assertTrue(error.localizedMessage.contains("already assigned"))
    }

    fun testApplyRejectsEmptyName() {
        val settings = TuiLauncherSettings.getInstance()
        settings.state.tuiApps.add(TuiAppConfig(name = "", command = "lazygit"))

        val configurable = TuiLauncherConfiguration()
        configurable.createComponent()

        val error = try {
            configurable.apply()
            fail("Expected ConfigurationException")
            return
        } catch (e: ConfigurationException) {
            e
        }
        assertTrue(error.localizedMessage.contains("name cannot be empty"))
    }

    fun testRejectedInvalidNewAppDoesNotMutatePersistedSettings() {
        val settings = TuiLauncherSettings.getInstance()
        settings.state.tuiApps.add(TuiAppConfig(name = "lazygit", command = "lazygit"))

        val configurable = TuiLauncherConfiguration()
        val component = configurable.createComponent() as JPanel
        val appsTable = findAppsTable(component)!!
        (appsTable.model as TuiAppTableModel).addRow(TuiAppConfig(name = "broken", command = ""))

        try {
            configurable.apply()
            fail("Expected ConfigurationException")
        } catch (_: ConfigurationException) {
            // expected
        }

        assertEquals(1, settings.state.tuiApps.size)
        assertEquals("lazygit", settings.state.tuiApps.single().name)
    }

    fun testApplyRejectsEmptyCommand() {
        val settings = TuiLauncherSettings.getInstance()
        settings.state.tuiApps.add(TuiAppConfig(name = "lazygit", command = ""))

        val configurable = TuiLauncherConfiguration()
        configurable.createComponent()

        val error = try {
            configurable.apply()
            fail("Expected ConfigurationException")
            return
        } catch (e: ConfigurationException) {
            e
        }
        assertTrue(error.localizedMessage.contains("command cannot be empty"))
    }

    private fun findCheckBox(container: Container, text: String): JCheckBox? {
        for (component in container.components) {
            if (component is JCheckBox && component.text == text) return component
            if (component is Container) findCheckBox(component, text)?.let { return it }
        }
        return null
    }

    private fun findAppsTable(container: Container): JTable? {
        for (component in container.components) {
            if (component is JTable && component.columnCount == 4) return component
            if (component is Container) findAppsTable(component)?.let { return it }
        }
        return null
    }

    private fun findTable(container: Container): JTable? {
        for (component in container.components) {
            if (component is JTable && component.columnCount == 2 && component.getColumnName(0) == "Action") return component
            if (component is Container) findTable(component)?.let { return it }
        }
        return null
    }
}
