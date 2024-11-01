package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.action.UastModelTrackerInvalidateAction
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Dimension
import javax.swing.AbstractButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel


class SpringToolRunConfigurationConfigurable : SearchableConfigurable {
    private val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()

    private val propertyGraph = PropertyGraph()

    private val isAutoDetection = propertyGraph.lazyProperty { settingsState.isAutoDetectConfigurations }
    private val isBeanFilterEnabled = propertyGraph.lazyProperty { settingsState.isBeanFilterEnabled }

    override fun getId(): String = ID

    override fun createComponent(): JComponent {
        return createMainPanel()
    }

    override fun isModified(): Boolean {
        val changeDetection = isAutoDetection.get() != settingsState.isAutoDetectConfigurations
                || isBeanFilterEnabled.get() != settingsState.isBeanFilterEnabled

        return changeDetection
    }

    override fun apply() {
        settingsState.isAutoDetectConfigurations = isAutoDetection.get()

        if (settingsState.isBeanFilterEnabled != isBeanFilterEnabled.get()) {
            settingsState.isBeanFilterEnabled = isBeanFilterEnabled.get()

            ProjectUtil.getActiveProject()?.let { project ->
                UastModelTrackerInvalidateAction.invalidate(project)
            }
        }
    }

    override fun getDisplayName(): String = "Run Configurations"

    companion object {
        const val ID = "com.explyt.spring.runConfigurations"
    }

    private fun createMainPanel(): JPanel {
        val mainPanel = BorderLayoutPanel(10, 10)
        mainPanel.preferredSize = Dimension(550, 400)

        mainPanel.addToTop(configTop())
        mainPanel.addToBottom(configBottom())

        return mainPanel
    }

    private fun configTop(): BorderLayoutPanel {
        val topPanel = BorderLayoutPanel(10, 10)

        val cbAutoDetection = JCheckBox("SpringBoot configurations auto-detection")
        cbAutoDetection.isSelected = isAutoDetection.get()
        cbAutoDetection.addItemListener {
            isAutoDetection.set((it.source as AbstractButton).isSelected)
        }
        topPanel.addToCenter(cbAutoDetection)

        val cbEnableBeanFiltering =
            JCheckBox(SpringCoreBundle.message("explyt.spring.settings.enableBeanFiltering.label"))
        cbEnableBeanFiltering.toolTipText =
            SpringCoreBundle.message("explyt.spring.settings.enableBeanFiltering.tooltip")
        cbEnableBeanFiltering.isSelected = isBeanFilterEnabled.get()
        cbEnableBeanFiltering.addItemListener {
            isBeanFilterEnabled.set((it.source as AbstractButton).isSelected)
        }
        topPanel.addToBottom(cbEnableBeanFiltering)

        return topPanel
    }

    private fun configBottom(): BorderLayoutPanel {
        val bottomPanel = BorderLayoutPanel(0, 300)
        return bottomPanel
    }

}
