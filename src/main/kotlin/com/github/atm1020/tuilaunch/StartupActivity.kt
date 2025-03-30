package com.github.atm1020.tuilaunch

import com.github.atm1020.tuilaunch.listeners.TerminalContextManagerListener
import com.github.atm1020.tuilaunch.services.TuiLauncherSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager


class LoadDynamicActions : ProjectActivity {

    override suspend fun execute(project: Project) {
        TuiLauncherSettings.getInstance().loadActions()
        val window = ToolWindowManager.getInstance(project).getToolWindow("Terminal")!!
        window.addContentManagerListener(TerminalContextManagerListener(project, window.type))
    }
}