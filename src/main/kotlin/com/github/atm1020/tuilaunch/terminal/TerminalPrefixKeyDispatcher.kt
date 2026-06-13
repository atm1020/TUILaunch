package com.github.atm1020.tuilaunch.terminal

import java.awt.Component
import java.awt.KeyEventDispatcher
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities

/**
 * Lets the user control a focused TUI app with a modifier prefix combo (e.g. Ctrl+Space).
 *
 * Registered globally on the `KeyboardFocusManager`, this sees key events before the terminal
 * component's own key handling, so it can swallow the prefix and command keys — the TUI never
 * receives them — and run plugin actions.
 *
 * The combo only acts while focus is inside [terminalComponent]: the guard checks the *event's own
 * component*, which is the component that had focus when the key was generated. Every other key, and
 * the combo pressed elsewhere in the IDE, passes through untouched.
 *
 * A key press produces a `KEY_PRESSED` *and* a separate `KEY_TYPED` carrying the character (the space
 * of Ctrl+Space). Consuming only the `KEY_PRESSED` would still let the `KEY_TYPED` reach the terminal,
 * typing a stray space. So after consuming the combo, the trailing typed char is swallowed too, until
 * the next key press.
 *
 * @param escapeModifierMask the required AWT extended-modifier mask (e.g. [KeyEvent.CTRL_DOWN_MASK]).
 * @param escapeKeyCode the AWT `VK_` key code combined with the modifier (e.g. [KeyEvent.VK_SPACE]).
 */
class TerminalPrefixKeyDispatcher(
    private val terminalComponent: Component,
    private val escapeModifierMask: Int,
    private val escapeKeyCode: Int,
    private val prefixCommandActions: Map<Int, () -> Unit>,
) : KeyEventDispatcher {

    // IntelliJ delivers each KEY_PRESSED to the dispatcher twice; remember the handled press so the
    // duplicate is swallowed without invoking the action a second time.
    private var lastFiredWhen = -1L
    private var lastFiredKeyCode = KeyEvent.VK_UNDEFINED

    // True after consuming a key press: consume the trailing KEY_TYPED char so it never lands in the
    // terminal or editor. Cleared by the next unhandled key press.
    private var consumeNextTypedEvent = false

    private var prefixArmed = false

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.id == KeyEvent.KEY_TYPED) {
            if (!consumeNextTypedEvent) return false
            e.consume()
            return true
        }
        if (e.id != KeyEvent.KEY_PRESSED) return false

        // IntelliJ can deliver the same handled press twice; swallow the duplicate before it reaches
        // the terminal, without firing the command again.
        if (e.`when` == lastFiredWhen && e.keyCode == lastFiredKeyCode) {
            e.consume()
            return true
        }

        val source = e.component
        if (source == null || !SwingUtilities.isDescendingFrom(source, terminalComponent)) {
            prefixArmed = false
            consumeNextTypedEvent = false
            return false
        }

        if (prefixArmed) {
            prefixArmed = false
            val action = prefixCommandActions[e.keyCode]
            if (action == null) {
                consumeNextTypedEvent = false
                e.consume()
                return true
            }

            action()

            e.consume()
            lastFiredWhen = e.`when`
            lastFiredKeyCode = e.keyCode
            consumeNextTypedEvent = true
            return true
        }

        val isCombo = e.keyCode == escapeKeyCode && e.modifiersEx == escapeModifierMask
        if (!isCombo) {
            consumeNextTypedEvent = false // any other press cancels a pending typed-event consume
            return false
        }

        prefixArmed = true
        e.consume()
        lastFiredWhen = e.`when`
        lastFiredKeyCode = e.keyCode
        consumeNextTypedEvent = true
        return true
    }
}
