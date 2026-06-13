package com.github.atm1020.tuilaunch.services

import com.github.atm1020.tuilaunch.action.DynamicUserAction
import com.github.atm1020.tuilaunch.model.TuiAppConfig
import com.github.atm1020.tuilaunchmodel.TuiAppTableModel
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.*
import java.awt.event.KeyEvent

@Service
@State(
    name = "TuiLauncherSettings",
    storages = [Storage("tuiLauncherSettings.xml")]
)
class TuiLauncherSettings : PersistentStateComponent<TuiLauncherSettings.State> {
    private var settingsState = TuiLauncherSettings.State()

    data class State(
        var tuiApps: MutableList<TuiAppConfig> = mutableListOf(),
        /** Modifier for the tmux-style escape prefix: "CTRL" or "ALT". */
        var escapeModifier: String = "CTRL",
        /** Prefix key (an AWT [KeyEvent] VK_ code) combined with [escapeModifier], e.g. Ctrl+Space. */
        var escapeKeyCode: Int = KeyEvent.VK_SPACE,
        /** Key pressed after the prefix to return focus to the editor. */
        var focusEditorKeyCode: Int = KeyEvent.VK_E,
        /** Key pressed after the prefix to close the active TUI tab. */
        var closeTuiKeyCode: Int = KeyEvent.VK_C,
        /** Key pressed after the prefix to select the next TUI tab. */
        var nextTuiKeyCode: Int = KeyEvent.VK_N,
        /** Key pressed after the prefix to select the previous TUI tab. */
        var previousTuiKeyCode: Int = KeyEvent.VK_P,
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

    fun loadActions() {
        val tableModel = TuiAppTableModel(settingsState.tuiApps)
        tableModel.let { model ->
            val actionManger = ActionManager.getInstance()
            for (i in 0..<model.rowCount) {
                val name = model.getValueAt(i, 0)
                val command = model.getValueAt(i, 1)
                val actionId = model.getValueAt(i, 3).toString()
                val action = DynamicUserAction(actionId, command.toString(), name.toString())
                if (actionManger.getAction(actionId) != null) {
                    actionManger.unregisterAction(actionId)
                }
                actionManger.registerAction(actionId, action)
            }
        }
    }

}
