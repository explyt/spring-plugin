/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

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
