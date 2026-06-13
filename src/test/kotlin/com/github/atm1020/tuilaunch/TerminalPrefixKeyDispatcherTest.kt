package com.github.atm1020.tuilaunch

import com.github.atm1020.tuilaunch.terminal.TerminalPrefixKeyDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Component
import java.awt.event.KeyEvent
import javax.swing.JPanel

/**
 * A [java.awt.KeyEventDispatcher] returns `true` to mean "I handled this; do NOT dispatch further"
 * — i.e. the terminal never receives the event. So "the terminal must not get the combo" is exactly
 * `dispatchKeyEvent(combo)` returning `true`.
 */
class TerminalPrefixKeyDispatcherTest {

    /** Stands in for the focused terminal component and is the source of the synthetic key events. */
    private val terminal = JPanel()

    /** A component that is NOT inside the terminal, for the "combo while not focused" case. */
    private val elsewhere = JPanel()

    private var switchCount = 0
    private var closeCount = 0
    private var nextCount = 0
    private var previousCount = 0

    private fun newDispatcher() = TerminalPrefixKeyDispatcher(
        terminalComponent = terminal,
        escapeModifierMask = KeyEvent.CTRL_DOWN_MASK,
        escapeKeyCode = KeyEvent.VK_SPACE,
        prefixCommandActions = mapOf(
            KeyEvent.VK_E to { switchCount++ },
            KeyEvent.VK_C to { closeCount++ },
            KeyEvent.VK_N to { nextCount++ },
            KeyEvent.VK_P to { previousCount++ },
        ),
    )

    private fun keyPress(
        keyCode: Int,
        modifiers: Int = 0,
        source: Component = terminal,
        whenMs: Long = System.currentTimeMillis(),
    ): KeyEvent = KeyEvent(source, KeyEvent.KEY_PRESSED, whenMs, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED)

    private fun keyTyped(
        keyChar: Char,
        source: Component = terminal,
        whenMs: Long = System.currentTimeMillis(),
    ): KeyEvent = KeyEvent(source, KeyEvent.KEY_TYPED, whenMs, 0, KeyEvent.VK_UNDEFINED, keyChar)

    @Test
    fun comboWhileFocusedEntersPrefixModeAndIsSwallowed() {
        val dispatcher = newDispatcher()
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)))
        assertEquals(0, switchCount)
    }

    @Test
    fun bareSwitchKeyPassesThrough() {
        val dispatcher = newDispatcher()
        assertFalse(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_SPACE, modifiers = 0)))
        assertEquals(0, switchCount)
    }

    @Test
    fun wrongKeyWithModifierPassesThrough() {
        val dispatcher = newDispatcher()
        assertFalse(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK)))
        assertEquals(0, switchCount)
    }

    @Test
    fun comboWhileNotFocusedPassesThrough() {
        val dispatcher = newDispatcher()
        val event = keyPress(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK, source = elsewhere)
        assertFalse(dispatcher.dispatchKeyEvent(event))
        assertEquals(0, switchCount)
    }

    @Test
    fun duplicateDeliveryFiresOnce() {
        val dispatcher = newDispatcher()
        // IntelliJ delivers the same KEY_PRESSED twice (same timestamp). Reuse one event object.
        val event = keyPress(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)
        assertTrue(dispatcher.dispatchKeyEvent(event))
        assertTrue(dispatcher.dispatchKeyEvent(event))
        assertEquals(0, switchCount)
    }

    @Test
    fun comboTrailingTypedCharIsSwallowed() {
        val dispatcher = newDispatcher()
        // The combo's KEY_PRESSED is consumed...
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)))
        // ...and so is the trailing KEY_TYPED space, so the terminal never types a space.
        assertTrue(dispatcher.dispatchKeyEvent(keyTyped(' ')))
    }

    @Test
    fun typedCharWithoutComboPassesThrough() {
        val dispatcher = newDispatcher()
        // A typed char with no preceding combo must reach the terminal untouched.
        assertFalse(dispatcher.dispatchKeyEvent(keyTyped('a')))
    }

    @Test
    fun keyPressAfterComboStopsSwallowingTyped() {
        val dispatcher = newDispatcher()
        dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)) // arms swallow
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_A))) // unrelated prefix command is swallowed
        assertFalse(dispatcher.dispatchKeyEvent(keyTyped('a'))) // its typed char now passes through
    }

    @Test
    fun prefixThenEFocusesEditor() {
        val dispatcher = newDispatcher()
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)))
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_E)))
        assertEquals(1, switchCount)
    }

    @Test
    fun prefixThenCCloseActiveTui() {
        val dispatcher = newDispatcher()
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)))
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_C)))
        assertEquals(1, closeCount)
    }

    @Test
    fun prefixCommandTrailingTypedCharIsSwallowed() {
        val dispatcher = newDispatcher()
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)))
        assertTrue(dispatcher.dispatchKeyEvent(keyTyped(' ')))
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_C)))
        assertTrue(dispatcher.dispatchKeyEvent(keyTyped('c')))
        assertEquals(1, closeCount)
    }

    @Test
    fun duplicatePrefixCommandPressIsSwallowedWithoutRunningTwice() {
        val dispatcher = newDispatcher()
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)))
        val event = keyPress(KeyEvent.VK_C)
        assertTrue(dispatcher.dispatchKeyEvent(event))
        assertTrue(dispatcher.dispatchKeyEvent(event))
        assertEquals(1, closeCount)
    }

    @Test
    fun prefixThenNSwitchesToNextTui() {
        val dispatcher = newDispatcher()
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)))
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_N)))
        assertEquals(1, nextCount)
    }

    @Test
    fun prefixThenPSwitchesToPreviousTui() {
        val dispatcher = newDispatcher()
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)))
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_P)))
        assertEquals(1, previousCount)
    }

    @Test
    fun prefixCommandUsesProvidedActionMap() {
        val dispatcher = TerminalPrefixKeyDispatcher(
            terminalComponent = terminal,
            escapeModifierMask = KeyEvent.CTRL_DOWN_MASK,
            escapeKeyCode = KeyEvent.VK_SPACE,
            prefixCommandActions = mapOf(KeyEvent.VK_F to { switchCount++ }),
        )

        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)))
        assertTrue(dispatcher.dispatchKeyEvent(keyPress(KeyEvent.VK_F)))

        assertEquals(1, switchCount)
    }
}
