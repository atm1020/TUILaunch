package com.github.atm1020.tuilaunch.services

import com.github.atm1020.tuilaunch.action.DynamicUserAction
import com.github.atm1020.tuilaunch.model.TuiAppConfig
import com.github.atm1020.tuilaunch.ui.TuiLauncherConfiguration
import com.github.atm1020.tuilaunchmodel.TuiAppTableModel
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.*

@Service
@State(
    name = "TuiLauncherSettings",
    storages = [Storage("tuiLauncherSettings.xml")]
)
class TuiLauncherSettings : PersistentStateComponent<TuiLauncherSettings.State> {
    private var settingsState = TuiLauncherSettings.State()

    data class State(
        var tuiApps: MutableList<TuiAppConfig> = mutableListOf(),
        var terminalViewMode: TuiLauncherConfiguration.SelectToolWindowType? = null
    )

    override fun loadState(state: TuiLauncherSettings.State) {
        settingsState = state
    }

    companion object {
        @JvmStatic
        fun getInstance(): TuiLauncherSettings = service()
    }

    override fun getState(): TuiLauncherSettings.State {
        return settingsState
    }

    fun checkIsTuiCommand(commands: Set<String>): Boolean {
        val tableModel = TuiAppTableModel(settingsState.tuiApps)
        tableModel.let { model ->
            for (i in 0..<model.rowCount) {
                if (commands.contains(model.getValueAt(i, 1).toString())) return true
            }
        }
        return false

    }

    fun checkIsTuiCommand(command: String): Boolean {
        val tableModel = TuiAppTableModel(settingsState.tuiApps)
        tableModel.let { model ->
            for (i in 0..<model.rowCount) {
                if (model.getValueAt(i, 1).toString() == command) return true
            }
        }
        return false

    }

    fun loadActions() {
        val tableModel = TuiAppTableModel(settingsState.tuiApps)
        tableModel.let { model ->
            val actionManger = ActionManager.getInstance()
            for (i in 0..<model.rowCount) {
                val command = model.getValueAt(i, 1)
                val actionId = model.getValueAt(i, 3).toString()
                val action = DynamicUserAction(command.toString())
                if (actionManger.getAction(actionId) != null) {
                    actionManger.unregisterAction(actionId)
                }
                actionManger.registerAction(actionId, action)
            }
        }
    }
}
