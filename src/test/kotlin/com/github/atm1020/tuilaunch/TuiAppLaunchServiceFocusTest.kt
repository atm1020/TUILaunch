package com.github.atm1020.tuilaunch

import com.github.atm1020.tuilaunch.model.TuiAppConfig
import com.github.atm1020.tuilaunch.services.TuiLauncherSettings
import com.github.atm1020.tuilaunch.services.TUI_TOOL_WINDOW_ID
import com.github.atm1020.tuilaunch.services.TuiAppLaunchService
import com.github.atm1020.tuilaunch.terminal.TerminalSession
import com.github.atm1020.tuilaunch.terminal.TerminalSessionFactory
import com.github.atm1020.tuilaunch.toolwindow.IdeToolWindowHost
import com.github.atm1020.tuilaunch.toolwindow.ToolWindowSize
import com.intellij.openapi.Disposable
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JPanel

class TuiAppLaunchServiceFocusTest : BasePlatformTestCase() {

    private class FakeSession {
        val component: JComponent = JPanel()
        var focusCount = 0
        fun requestFocus() {
            focusCount++
        }

        fun asTerminalSession(): TerminalSession = TerminalSession(
            component = component,
            requestFocus = { requestFocus() },
            registerTerminationCallback = {},
        )
    }

    private class FakeFactory(private val sessions: List<FakeSession>) : TerminalSessionFactory {
        private var index = 0

        constructor(session: FakeSession) : this(listOf(session))

        override fun create(parent: Disposable, command: String): TerminalSession = sessions[index++].asTerminalSession()
    }

    private class DeferredFactory(private val session: FakeSession) : TerminalSessionFactory {
        private var onCreated: ((TerminalSession) -> Unit)? = null
        var createCount = 0

        override fun create(parent: Disposable, command: String): TerminalSession = error("Use async creation")

        override fun createAsync(
            parent: Disposable,
            command: String,
            onCreated: (TerminalSession) -> Unit,
            onFailed: (Throwable) -> Unit,
        ) {
            createCount++
            this.onCreated = onCreated
        }

        fun finish() {
            onCreated?.invoke(session.asTerminalSession())
        }
    }

    private class FakeHost : IdeToolWindowHost(null) {
        var visible = false
        var pinned = true
        var showCount = 0
        private var selected: Any? = null
        val tabs = mutableListOf<Any>()
        var size: ToolWindowSize? = null
        val appliedSizes = mutableListOf<ToolWindowSize>()
        private var sizeChanged: (() -> Unit)? = null
        private var tabSelected: ((Any) -> Unit)? = null

        override fun isVisible(): Boolean = visible
        override fun isPinned(): Boolean = pinned
        override fun show() {
            visible = true
            showCount++
        }

        override fun hide() {
            visible = false
        }

        override fun addTab(component: JComponent, title: String, disposable: Disposable): Any {
            val handle = Any()
            tabs.add(handle)
            // ContentManager auto-selects the first content, firing selectionChanged.
            if (selected == null) {
                selected = handle
                tabSelected?.invoke(handle)
            }
            return handle
        }

        override fun selectTab(handle: Any) {
            selected = handle
            tabSelected?.invoke(handle)
        }

        override fun activeTab(): Any? = selected
        override fun removeTab(handle: Any) {
            tabs.remove(handle)
            if (selected === handle) selected = null
        }

        override fun currentSize(): ToolWindowSize? = size

        override fun applySize(size: ToolWindowSize) {
            appliedSizes.add(size)
            this.size = size
        }

        override fun onSizeChanged(listener: () -> Unit) {
            sizeChanged = listener
        }

        override fun onTabSelected(listener: (Any) -> Unit) {
            tabSelected = listener
        }

        fun triggerSizeChanged() {
            sizeChanged?.invoke()
        }

        fun triggerTabSelected(handle: Any) {
            tabSelected?.invoke(handle)
        }
    }

    private fun configureApps(vararg apps: TuiAppConfig) {
        TuiLauncherSettings.getInstance().state.tuiApps = apps.toMutableList()
    }

    fun testFocusTuiDoesNothingWhenNoAppOpen() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        service.host = host

        service.focusTui()

        assertEquals(0, host.showCount)
        assertFalse(host.visible)
    }

    fun testResizeEventStoresActiveTabSize() {
        configureApps(TuiAppConfig(name = "htop", command = "htop"))
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        service.host = host
        service.sessionFactory = FakeFactory(FakeSession())

        service.toggle("TUILauncher.htop", "htop", "htop")
        host.size = ToolWindowSize(900, 500)
        host.triggerSizeChanged()

        val app = TuiLauncherSettings.getInstance().state.tuiApps.single()
        assertEquals(900, app.windowWidth)
        assertEquals(500, app.windowHeight)
    }

    fun testSelectingTabAppliesItsSavedSize() {
        configureApps(
            TuiAppConfig(name = "first", command = "first", windowWidth = 700, windowHeight = 400),
            TuiAppConfig(name = "second", command = "second", windowWidth = 1100, windowHeight = 800),
        )
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        service.host = host
        service.sessionFactory = FakeFactory(listOf(FakeSession(), FakeSession()))

        service.toggle("TUILauncher.first", "first", "first")
        service.toggle("TUILauncher.second", "second", "second")
        val firstTab = host.tabs[0]
        host.appliedSizes.clear()

        host.triggerTabSelected(firstTab)

        assertEquals(listOf(ToolWindowSize(700, 400)), host.appliedSizes)
    }

    fun testFirstLaunchAppliesSavedSize() {
        configureApps(TuiAppConfig(name = "htop", command = "htop", windowWidth = 900, windowHeight = 600))
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        host.size = ToolWindowSize(500, 300)
        service.host = host
        service.sessionFactory = FakeFactory(FakeSession())

        service.toggle("TUILauncher.htop", "htop", "htop")

        assertEquals(ToolWindowSize(900, 600), host.size)
    }

    fun testTogglingTabAppliesSavedSizeViaSelection() {
        configureApps(TuiAppConfig(name = "htop", command = "htop", windowWidth = 900, windowHeight = 600))
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        service.host = host
        service.sessionFactory = FakeFactory(FakeSession())

        service.toggle("TUILauncher.htop", "htop", "htop")

        assertEquals(ToolWindowSize(900, 600), host.appliedSizes.last())
    }

    fun testSelectingTabWithoutSavedSizeAppliesNothing() {
        configureApps(TuiAppConfig(name = "htop", command = "htop"))
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        service.host = host
        service.sessionFactory = FakeFactory(FakeSession())

        service.toggle("TUILauncher.htop", "htop", "htop")
        host.appliedSizes.clear()

        host.triggerTabSelected(host.tabs[0])

        assertTrue(host.appliedSizes.isEmpty())
    }

    fun testFocusTuiRevealsAndFocusesActiveTab() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        val session = FakeSession()
        service.host = host
        service.sessionFactory = FakeFactory(session)

        service.toggle("TUILauncher.htop", "htop", "htop")
        host.showCount = 0
        session.focusCount = 0

        service.focusTui()

        assertTrue(host.visible)
        assertEquals(1, host.showCount)
        assertEquals(1, session.focusCount)
        assertNotNull(host.activeTab())
    }

    fun testToggleWaitsForAsynchronousSessionCreation() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        val session = FakeSession()
        val factory = DeferredFactory(session)
        service.host = host
        service.sessionFactory = factory

        service.toggle("TUILauncher.htop", "htop", "htop")
        service.toggle("TUILauncher.htop", "htop", "htop")

        assertEquals(1, factory.createCount)
        assertTrue(host.tabs.isEmpty())

        factory.finish()

        assertEquals(1, host.tabs.size)
        assertSame(host.tabs.single(), host.activeTab())
        assertEquals(1, session.focusCount)
    }

    fun testToggleToolWindowShowsHiddenWindowWithoutRequestingFocus() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        val session = FakeSession()
        service.host = host
        service.sessionFactory = FakeFactory(session)

        service.toggle("TUILauncher.htop", "htop", "htop")
        host.hide()
        host.showCount = 0
        session.focusCount = 0

        service.toggleToolWindow()

        assertTrue(host.visible)
        assertEquals(1, host.showCount)
        assertEquals(0, session.focusCount)
    }

    fun testToggleToolWindowHidesVisibleWindow() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        service.host = host
        service.sessionFactory = FakeFactory(FakeSession())

        service.toggle("TUILauncher.htop", "htop", "htop")

        service.toggleToolWindow()

        assertFalse(host.visible)
    }

    fun testCloseActiveTuiRemovesSelectedTab() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        val firstSession = FakeSession()
        val secondSession = FakeSession()
        service.host = host
        service.sessionFactory = FakeFactory(listOf(firstSession, secondSession))

        service.toggle("TUILauncher.first", "first", "first")
        val firstTab = host.activeTab()
        service.toggle("TUILauncher.second", "second", "second")

        service.closeActiveTui()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(listOf(firstTab), host.tabs)
        assertTrue(host.visible)
    }

    fun testCloseActiveTuiHidesWindowAfterLastTab() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        service.host = host
        service.sessionFactory = FakeFactory(FakeSession())

        service.toggle("TUILauncher.htop", "htop", "htop")
        service.closeActiveTui()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(host.tabs.isEmpty())
        assertFalse(host.visible)
    }

    fun testCloseLastTuiKeepsWindowVisibleWhenAlreadyVisibleBeforeLaunch() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        host.visible = true
        service.host = host
        service.sessionFactory = FakeFactory(FakeSession())

        service.toggle("TUILauncher.htop", "htop", "htop")
        service.closeActiveTui()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(host.tabs.isEmpty())
        assertTrue(host.visible)
    }

    fun testCloseLastTuiKeepsWindowVisibleWithMultipleTabsWhenVisibleBeforeLaunch() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        host.visible = true
        service.host = host
        service.sessionFactory = FakeFactory(listOf(FakeSession(), FakeSession()))

        service.toggle("TUILauncher.first", "first", "first")
        service.toggle("TUILauncher.second", "second", "second")
        service.closeActiveTui()
        service.closeActiveTui()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(host.tabs.isEmpty())
        assertTrue(host.visible)
    }

    fun testCloseLastTuiKeepsWindowVisibleWhenUnpinned() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        host.pinned = false
        service.host = host
        service.sessionFactory = FakeFactory(FakeSession())

        service.toggle("TUILauncher.htop", "htop", "htop")
        service.closeActiveTui()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(host.tabs.isEmpty())
        assertTrue(host.visible)
    }

    fun testCloseActiveTuiOpenedFromEditorReturnsFocusToEditor() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        service.host = host
        service.sessionFactory = FakeFactory(FakeSession())
        service.activeToolWindowIdProvider = { "Project" }
        var editorFocusCount = 0
        service.editorFocusRequest = { editorFocusCount++ }

        service.toggle("TUILauncher.htop", "htop", "htop")
        service.closeActiveTui()

        assertEquals(1, editorFocusCount)
    }

    fun testCloseActiveTuiOpenedFromTuiDoesNotReturnFocusToEditor() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        service.host = host
        service.sessionFactory = FakeFactory(FakeSession())
        service.activeToolWindowIdProvider = { TUI_TOOL_WINDOW_ID }
        var editorFocusCount = 0
        service.editorFocusRequest = { editorFocusCount++ }

        service.toggle("TUILauncher.htop", "htop", "htop")
        service.closeActiveTui()

        assertEquals(0, editorFocusCount)
    }

    fun testNextTuiTabSelectsAndFocusesNextTab() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        val firstSession = FakeSession()
        val secondSession = FakeSession()
        service.host = host
        service.sessionFactory = FakeFactory(listOf(firstSession, secondSession))

        service.toggle("TUILauncher.first", "first", "first")
        service.toggle("TUILauncher.second", "second", "second")
        host.selectTab(host.tabs[0])
        secondSession.focusCount = 0

        service.nextTuiTab()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertSame(host.tabs[1], host.activeTab())
        assertEquals(1, secondSession.focusCount)
    }

    fun testNextTuiTabWithoutFocusSelectsWithoutRequestingFocus() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        val firstSession = FakeSession()
        val secondSession = FakeSession()
        service.host = host
        service.sessionFactory = FakeFactory(listOf(firstSession, secondSession))

        service.toggle("TUILauncher.first", "first", "first")
        service.toggle("TUILauncher.second", "second", "second")
        host.selectTab(host.tabs[0])
        secondSession.focusCount = 0

        service.nextTuiTabWithoutFocus()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertSame(host.tabs[1], host.activeTab())
        assertEquals(0, secondSession.focusCount)
    }

    fun testPreviousTuiTabWithoutFocusSelectsWithoutRequestingFocus() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        val firstSession = FakeSession()
        val secondSession = FakeSession()
        service.host = host
        service.sessionFactory = FakeFactory(listOf(firstSession, secondSession))

        service.toggle("TUILauncher.first", "first", "first")
        service.toggle("TUILauncher.second", "second", "second")
        firstSession.focusCount = 0

        service.previousTuiTabWithoutFocus()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertSame(host.tabs[0], host.activeTab())
        assertEquals(0, firstSession.focusCount)
    }

    fun testPreviousTuiTabSelectsAndFocusesPreviousTab() {
        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        val firstSession = FakeSession()
        val secondSession = FakeSession()
        service.host = host
        service.sessionFactory = FakeFactory(listOf(firstSession, secondSession))

        service.toggle("TUILauncher.first", "first", "first")
        service.toggle("TUILauncher.second", "second", "second")
        firstSession.focusCount = 0

        service.previousTuiTab()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertSame(host.tabs[0], host.activeTab())
        assertEquals(1, firstSession.focusCount)
    }

    fun testPrefixCommandActionsUseConfiguredKeys() {
        val state = TuiLauncherSettings.getInstance().state
        state.closeTuiKeyCode = KeyEvent.VK_X
        state.nextTuiKeyCode = KeyEvent.VK_Y
        state.previousTuiKeyCode = KeyEvent.VK_Z

        val service = TuiAppLaunchService(project)
        val host = FakeHost()
        val firstSession = FakeSession()
        val secondSession = FakeSession()
        service.host = host
        service.sessionFactory = FakeFactory(listOf(firstSession, secondSession))

        service.toggle("TUILauncher.first", "first", "first")
        service.toggle("TUILauncher.second", "second", "second")
        val actions = service.prefixCommandActions()

        assertFalse(actions.containsKey(KeyEvent.VK_C))
        assertTrue(actions.containsKey(KeyEvent.VK_X))

        actions.getValue(KeyEvent.VK_X).invoke()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(1, host.tabs.size)
    }
}
