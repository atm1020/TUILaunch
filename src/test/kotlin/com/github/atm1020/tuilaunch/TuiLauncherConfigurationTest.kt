package com.github.atm1020.tuilaunch

import com.github.atm1020.tuilaunch.ui.TuiLauncherConfiguration
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JPanel

class TuiLauncherConfigurationTest : BasePlatformTestCase() {

    fun testKeySelectionPanelUsesVerticalLayout() {
        val component = TuiLauncherConfiguration().createComponent() as JPanel
        val shortcutPanel = (component.layout as BorderLayout).getLayoutComponent(BorderLayout.NORTH) as JPanel
        val layout = shortcutPanel.layout as BoxLayout

        assertEquals(BoxLayout.Y_AXIS, layout.axis)
    }
}
