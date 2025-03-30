package com.github.atm1020.tuilaunch.listeners

import com.github.atm1020.tuilaunch.services.TuiLauncherSettings
import com.github.atm1020.tuilaunch.ui.TuiLauncherConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ToolWindowType
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import org.jetbrains.plugins.terminal.TerminalToolWindowManager


class TerminalContextManagerListener(private val project: Project, private val initToolWindowType: ToolWindowType) :
    ContentManagerListener {

    private var initIsVisible: Boolean = false

    override fun selectionChanged(event: ContentManagerEvent) {
        val service = TuiLauncherSettings.getInstance()
        if (event.operation == ContentManagerEvent.ContentOperation.add) {
            this.selectionChangedWhenEventIsAdd(service, event)
        }
        if (event.operation == ContentManagerEvent.ContentOperation.remove) {
            this.selectionChangedWhenEvenIsRemove(service, event)
        }
        super.selectionChanged(event)
    }

    override fun contentAdded(event: ContentManagerEvent) {
        val service = TuiLauncherSettings.getInstance()
        if (service.checkIsTuiCommand(event.content.tabName)) {
            val window = this.getTerminalToolWindow()
            this.initIsVisible = window.isVisible
        }
        super.contentAdded(event)
    }

    override fun contentRemoved(event: ContentManagerEvent) {
        val service = TuiLauncherSettings.getInstance()
        if (service.checkIsTuiCommand(event.content.tabName)) {
            val window = this.getTerminalToolWindow()
            window.setType(this.initToolWindowType, null)
            if (!initIsVisible) {
                window.hide()
            }
        }
        super.contentRemoved(event)
    }

    private fun selectionChangedWhenEventIsAdd(service: TuiLauncherSettings, event: ContentManagerEvent) {
        val window = this.getTerminalToolWindow()
        if (service.checkIsTuiCommand(event.content.tabName)) {
            if (service.state.terminalViewMode == TuiLauncherConfiguration.SelectToolWindowType.DEFAULT) {
                window.setType(window.type, null)
            } else {
                window.setType(service.state.terminalViewMode?.type!!, null)
            }
            window.component.requestFocus()
        } else {
            window.setType(initToolWindowType, null)
        }
    }

    private fun selectionChangedWhenEvenIsRemove(service: TuiLauncherSettings, event: ContentManagerEvent) {
        if (service.checkIsTuiCommand(event.content.tabName)) {
            val terminalToolWindowManager = TerminalToolWindowManager.getInstance(project)
            terminalToolWindowManager.closeTab(event.content)
        }
    }

    private fun getTerminalToolWindow(): ToolWindow {
        return ToolWindowManager.getInstance(this.project).getToolWindow("Terminal")!!
    }
}