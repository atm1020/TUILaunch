# TUILaunch

![Build](https://github.com/atm1020/TUILaunch/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/com.github.atm1020.tuilaunch.svg)](https://plugins.jetbrains.com/plugin/com.github.atm1020.tuilaunch)

[![Downloads](https://img.shields.io/jetbrains/plugin/d/com.github.atm1020.tuilaunch.svg)](https://plugins.jetbrains.com/plugin/com.github.atm1020.tuilaunch)

<!-- Plugin description -->
**TUILaunch** launches TUI applications, such as `lazygit`, inside IntelliJ-based IDEs.
It is not limited to Git tools: you can also open your favorite terminal-based agent harness,
such as `claude`, `opencode`, `pi`, etc.
Every saved launch command gets an auto-generated IDE Action, so it can be opened from the
IDE keymap, the Tools menu, or IdeaVim's `<Action>(...)` mapping.

The plugin is inspired by Neovim's [ToggleTerm plugin](https://github.com/akinsho/toggleterm.nvim?tab=readme-ov-file#custom-terminals) custom terminal feature.
<!-- Plugin description end -->

<p align="center">
  <img src="assets/opencode_tuilaunch.png" alt="OpenCode running in the TUILaunch tool window">
</p>

## Features

Open the settings and add/edit commands:
<kbd>Settings/Preferences</kbd> > <kbd>Tools</kbd> > <kbd>TUI Launcher</kbd>

<p align="center">
  <img src="assets/tui_app_table.png" alt="TUI app table">
</p>

### Create actions for TUI apps

Add an installed application to the table and save it. TUILaunch registers an action with the format `TUILauncher.{name}`.

Saved apps are registered automatically in the built-in IDE keymap, so you can assign normal IntelliJ shortcuts to them.

<p align="center">
  <img src="assets/keymap.png" alt="Saved app actions in the keymap">
</p>

You can also call the generated action ID from IdeaVim with `<Action>(TUILauncher.{name})`.
Running the same app action again focuses the already-open tab instead of creating a duplicate.

### Dedicated TUILaunch tool window

TUI apps open in a dedicated `TUILaunch` tool window instead of the built-in Terminal tool window.
Multiple TUI apps can stay open at the same time as separate tabs, and each tab closes automatically when its command exits.

TUILaunch remembers the last tool window size used by each app and restores it when that app tab is selected again.

### Focus and tab actions

TUILaunch registers global actions that can be bound in the IDE keymap or called from IdeaVim:

<p align="center">
  <img src="assets/keymap_global_actions.png" alt="Global TUILaunch actions in the keymap">
</p>

- `TUILauncher.FocusTui` — focus the active TUI session.
- `TUILauncher.FocusEditor` — return focus to the editor.
- `TUILauncher.ToggleFocus` — switch focus between the editor and the active TUI session.
- `TUILauncher.ToggleToolWindow` — show or hide the `TUILaunch` tool window.
- `TUILauncher.ToggleToolWindowAndFocus` — show and focus the tool window, or hide it.
- `TUILauncher.CloseActiveTui` — close the selected TUI tab.
- `TUILauncher.NextTuiTab` / `TUILauncher.PreviousTuiTab` — switch TUI tabs and focus the terminal.
- `TUILauncher.NextTuiTabWithoutFocus` / `TUILauncher.PreviousTuiTabWithoutFocus` — switch TUI tabs without moving keyboard focus into the terminal.

### IdeaVim + tmux-like workflow

TUILaunch works well with both IntelliJ keymaps and IdeaVim. Standard IntelliJ keymap shortcuts continue to work while focus is inside a TUI app because the IDE handles those shortcuts first. This is enough if you rely mainly on IntelliJ keybindings.

If you mainly use IdeaVim, you can combine IdeaVim mappings in the editor with TUILaunch's tmux-like prefix shortcuts inside TUI sessions. For example, configure <kbd>Ctrl</kbd> + <kbd>Space</kbd> as the TUILaunch prefix key, then reuse familiar follow-up keys from your IdeaVim mappings.

```vim
" Launch a saved app. The action ID is TUILauncher.{name}.
nmap <Space>gg <Action>(TUILauncher.lazygit)

" Toggle focus between the editor and the active TUI tab.
nmap <Space>tt <Action>(TUILauncher.ToggleFocus)

" Focus the active TUI tab, then return to the editor.
nmap <Space>tj <Action>(TUILauncher.FocusTui)
nmap <Space>tk <Action>(TUILauncher.FocusEditor)

" Make Ctrl+Space available to IdeaVim in normal mode.
sethandler <C-Space> n:vim

" Ctrl+Space prefix-style mappings for TUILaunch.
nmap <C-Space><Space> <Action>(TUILauncher.ToggleToolWindow)
nmap <C-Space>e <Action>(TUILauncher.FocusTui)
nmap <C-Space>n <Action>(TUILauncher.NextTuiTab)
nmap <C-Space>p <Action>(TUILauncher.PreviousTuiTab)
```

Then register matching shortcuts in the TUILaunch tmux-like keybindings table, such as <kbd>Ctrl</kbd> + <kbd>Space</kbd>, then <kbd>G</kbd> to launch or focus `lazygit`.

### Tmux-like prefix keybindings

When focus is inside a TUI session, you can configure a prefix key that runs TUILaunch actions without sending the keys to the TUI app.

In the TUILaunch settings you can:

- Enable or disable tmux-like keybindings.
- Choose the prefix modifier: <kbd>Ctrl</kbd> or <kbd>Alt</kbd>.
- Record the prefix key.
- Assign prefix commands for focusing the editor, closing the active TUI, switching tabs, toggling the tool window, and launching saved apps.
- Clear assigned shortcuts with <kbd>Delete</kbd> or <kbd>Backspace</kbd>.

<p align="center">
  <img src="assets/tmux_keybindings.png" alt="Tmux-like keybindings">
</p>

Defined TUI apps automatically appear in the prefix-key table as launch actions, so they can be called from inside an active TUI session too.

<p align="center">
  <img src="assets/tmux_app_shortcuts.png" alt="Per-app tmux-like shortcuts">
</p>

Example workflow after configuring <kbd>Ctrl</kbd> + <kbd>Space</kbd> as the prefix and <kbd>E</kbd> as “Focus editor”:

1. Focus a TUI tab.
2. Press <kbd>Ctrl</kbd> + <kbd>Space</kbd>.
3. Press <kbd>E</kbd>.
4. Focus returns to the editor, and the key sequence is not sent to the TUI app.

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "TUILaunch"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/atm1020/TUILaunch/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
