package com.github.atm1020.tuilaunch.services

import com.github.atm1020.tuilaunch.model.TuiAppConfig
import com.github.atm1020.tuilaunch.terminal.JediTermSessionFactory
import com.github.atm1020.tuilaunch.terminal.TerminalSession
import com.github.atm1020.tuilaunch.terminal.TerminalSessionFactory
import com.github.atm1020.tuilaunch.toolwindow.IdeToolWindowHost
import com.github.atm1020.tuilaunch.toolwindow.ToolWindowSize
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ToolWindowManager

const val TUI_TOOL_WINDOW_ID = "TUILaunch"

@Service(Service.Level.PROJECT)
class TuiAppLaunchService(private val project: Project) {

    var sessionFactory: TerminalSessionFactory = JediTermSessionFactory(
        project = project,
        prefixCommandActions = { prefixCommandActions() },
    )
    var host: IdeToolWindowHost? = null
    var activeToolWindowIdProvider: () -> String? = {
        ToolWindowManager.getInstance(project).activeToolWindowId
    }
    var editorFocusRequest: () -> Unit = {
        val component = FileEditorManager.getInstance(project).selectedTextEditor?.contentComponent
        if (component != null) IdeFocusManager.getInstance(project).requestFocus(component, true)
    }
    private var sizeListenerInstalled = false
    private var windowRevealedByLaunch = false

    private data class OpenTab(
        val actionId: String,
        val session: TerminalSession,
        val handle: Any,
        val disposable: Disposable,
        var openedFromTui: Boolean,
    )

    private val tabs = mutableMapOf<String, OpenTab>()
    private val pendingTabs = mutableMapOf<String, Disposable>()

    private fun resolveHost(): IdeToolWindowHost? {
        host?.let { return it }
        val tw = ToolWindowManager.getInstance(project).getToolWindow(TUI_TOOL_WINDOW_ID)
        if (tw == null) {
            thisLogger().warn("TUILaunch tool window not registered")
            return null
        }
        return IdeToolWindowHost(tw).also { host = it }
    }

    private fun ensureSizeListener(host: IdeToolWindowHost) {
        if (sizeListenerInstalled) return
        sizeListenerInstalled = true
        host.onSizeChanged { recordActiveTabSize(host) }
        host.onTabSelected { handle -> onTabSelected(host, handle) }
    }

    private fun onTabSelected(host: IdeToolWindowHost, handle: Any) {
        val tab = tabs.values.firstOrNull { it.handle == handle } ?: return
        savedSize(tab.actionId)?.let { host.applySize(it) }
    }

    fun toggle(actionId: String, command: String, title: String) {
        val host = resolveHost() ?: return
        ensureSizeListener(host)
        val existing = tabs[actionId]
        if (existing != null) {
            selectTuiTab(host, existing)
            return
        }
        if (pendingTabs.containsKey(actionId)) return

        windowRevealedByLaunch = !host.isVisible()
        openNewTab(host, actionId, command, title)
    }

    fun focusTui() {
        if (tabs.isEmpty()) return
        val host = resolveHost() ?: return
        ensureSizeListener(host)
        val activeHandle = host.activeTab()
        val tab = tabs.values.firstOrNull { it.handle == activeHandle } ?: tabs.values.last()
        selectTuiTab(host, tab)
    }

    fun focusEditor() {
        editorFocusRequest()
    }

    fun toggleFocus() {
        val onTui = isTuiFocused()
        if (onTui) focusEditor() else focusTui()
    }

    fun toggleToolWindow() {
        if (tabs.isEmpty()) return
        val host = resolveHost() ?: return
        ensureSizeListener(host)
        if (host.isVisible()) {
            host.hide()
        } else {
            host.show()
        }
    }

    fun closeActiveTui() {
        val host = resolveHost() ?: return
        ensureSizeListener(host)
        val activeHandle = host.activeTab()
        val actionId = tabs.entries.firstOrNull { it.value.handle == activeHandle }?.key
            ?: tabs.entries.lastOrNull()?.key
            ?: return
        closeTab(actionId)
    }

    fun nextTuiTab() {
        selectRelativeTuiTab(1, requestFocus = true)
    }

    fun previousTuiTab() {
        selectRelativeTuiTab(-1, requestFocus = true)
    }

    fun nextTuiTabWithoutFocus() {
        selectRelativeTuiTab(1, requestFocus = false)
    }

    fun previousTuiTabWithoutFocus() {
        selectRelativeTuiTab(-1, requestFocus = false)
    }

    fun prefixCommandActions(): Map<Int, () -> Unit> {
        val state = TuiLauncherSettings.getInstance().state
        return mapOf(
            state.focusEditorKeyCode to { focusEditor() },
            state.closeTuiKeyCode to { closeActiveTui() },
            state.nextTuiKeyCode to { nextTuiTab() },
            state.previousTuiKeyCode to { previousTuiTab() },
        )
    }

    private fun selectRelativeTuiTab(offset: Int, requestFocus: Boolean) {
        if (tabs.size < 2) return
        val host = resolveHost() ?: return
        ensureSizeListener(host)
        val orderedTabs = tabs.values.toList()
        val activeIndex = orderedTabs.indexOfFirst { it.handle == host.activeTab() }.let { index ->
            if (index >= 0) index else orderedTabs.lastIndex
        }
        val nextIndex = Math.floorMod(activeIndex + offset, orderedTabs.size)
        val tab = orderedTabs[nextIndex]
        invokeLater {
            selectTuiTab(host, tab, requestFocus = requestFocus)
        }
    }

    private fun openNewTab(host: IdeToolWindowHost, actionId: String, command: String, title: String) {
        val disposable = Disposer.newCheckedDisposable("TUILaunch-$actionId")
        pendingTabs[actionId] = disposable
        sessionFactory.createAsync(
            parent = disposable,
            command = command,
            onCreated = { session ->
                if (pendingTabs.remove(actionId) !== disposable || disposable.isDisposed) return@createAsync
                val handle = host.addTab(session.component, title, disposable)
                tabs[actionId] = OpenTab(
                    actionId = actionId,
                    session = session,
                    handle = handle,
                    disposable = disposable,
                    openedFromTui = isTuiFocused(),
                )
                session.onTerminated { closeTab(actionId) }
                selectTuiTab(host, tabs.getValue(actionId), recordCurrent = true)
            },
            onFailed = { throwable ->
                pendingTabs.remove(actionId)
                Disposer.dispose(disposable)
                thisLogger().warn("Failed to launch TUI app: $command", throwable)
            },
        )
    }

    private fun selectTuiTab(
        host: IdeToolWindowHost,
        tab: OpenTab,
        recordCurrent: Boolean = true,
        requestFocus: Boolean = true,
    ) {
        // Only record when switching away from a different tab; recording the tab we are
        // about to select (e.g. a freshly-launched tab that is already active) would clobber
        // its saved size with the current window size before we can restore it.
        if (recordCurrent && host.activeTab() != tab.handle) recordActiveTabSize(host)
        if (requestFocus) tab.openedFromTui = isTuiFocused()
        host.show()
        host.selectTab(tab.handle)
        savedSize(tab.actionId)?.let { host.applySize(it) }
        if (requestFocus) tab.session.requestFocus()
    }

    private fun isTuiFocused(): Boolean = activeToolWindowIdProvider() == TUI_TOOL_WINDOW_ID

    private fun recordActiveTabSize(host: IdeToolWindowHost) {
        val activeHandle = host.activeTab() ?: return
        val activeTab = tabs.values.firstOrNull { it.handle == activeHandle } ?: return
        val size = host.currentSize() ?: return
        configFor(activeTab.actionId)?.let { config ->
            config.windowWidth = size.width
            config.windowHeight = size.height
        }
    }

    private fun savedSize(actionId: String): ToolWindowSize? {
        val config = configFor(actionId) ?: return null
        val width = config.windowWidth ?: return null
        val height = config.windowHeight ?: return null
        if (width <= 0 || height <= 0) return null
        return ToolWindowSize(width, height)
    }

    private fun configFor(actionId: String): TuiAppConfig? {
        val name = actionId.removePrefix("TUILauncher.")
        return TuiLauncherSettings.getInstance().state.tuiApps.firstOrNull { it.name == name }
    }

    private fun closeTab(actionId: String) {
        val tab = tabs.remove(actionId) ?: return
        var shouldRestoreEditorFocus = false

        resolveHost()?.let { h ->
            val closingActiveTab = h.activeTab() == tab.handle
            shouldRestoreEditorFocus = closingActiveTab && !tab.openedFromTui
            if (closingActiveTab) {
                h.currentSize()?.let { size ->
                    configFor(actionId)?.let { config ->
                        config.windowWidth = size.width
                        config.windowHeight = size.height
                    }
                }
            }
        }

        if (shouldRestoreEditorFocus) focusEditor()
        resolveHost()?.let { h ->
            invokeLater {
                h.removeTab(tab.handle)
                if (windowRevealedByLaunch && h.isPinned()) {
                    h.hide()
                }

            }
        }
        Disposer.dispose(tab.disposable)
    }
}
