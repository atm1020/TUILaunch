package com.github.atm1020.tuilaunch

import com.github.atm1020.tuilaunch.services.TuiLauncherSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class LoadDynamicActions : ProjectActivity {
    override suspend fun execute(project: Project) {
        TuiLauncherSettings.getInstance().loadActions()
    }
}
