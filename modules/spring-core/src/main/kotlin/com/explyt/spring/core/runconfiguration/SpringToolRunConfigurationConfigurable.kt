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

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.action.UastModelTrackerInvalidateAction
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent


class SpringToolRunConfigurationConfigurable : SearchableConfigurable {
    private val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()

    private val propertyGraph = PropertyGraph()

    private val isAutoDetection = propertyGraph.property(false)
    private val isBeanFilterEnabled = propertyGraph.property(false)
    private val sqlLanguageIdBind = propertyGraph.property("")

    override fun getId(): String = ID

    override fun createComponent(): JComponent {
        return panel {
            group {
                row {
                    checkBox(message("explyt.spring.settings.config.auto.detect.label"))
                        .align(AlignX.FILL)
                        .bindSelected(isAutoDetection)
                        .resizableColumn()
                }

                row {
                    checkBox(message("explyt.spring.settings.enableBeanFiltering.label"))
                        .align(AlignX.FILL)
                        .bindSelected(isBeanFilterEnabled)
                        .applyToComponent {
                            toolTipText = message("explyt.spring.settings.enableBeanFiltering.tooltip")
                        }
                        .resizableColumn()
                }

                row(message("explyt.spring.settings.sql.language.id.label")) {
                    textField()
                        .align(AlignX.FILL)
                        .applyToComponent {
                            toolTipText = message("explyt.spring.settings.sql.language.id.tooltip")
                            emptyText.text = "SQL Language ID"
                        }
                        .bindText(sqlLanguageIdBind)
                        .resizableColumn()
                }
            }
        }
    }

    override fun reset() {
        isAutoDetection.set(settingsState.isAutoDetectConfigurations)
        isBeanFilterEnabled.set(settingsState.isBeanFilterEnabled)
        sqlLanguageIdBind.set(settingsState.sqlLanguageId ?: "")
    }

    override fun isModified(): Boolean {
        if (settingsState.isAutoDetectConfigurations != isAutoDetection.get()) return true
        if (settingsState.isBeanFilterEnabled != isBeanFilterEnabled.get()) return true
        if (settingsState.sqlLanguageId != sqlLanguageIdBind.get()) return true
        return false
    }

    override fun apply() {
        settingsState.isAutoDetectConfigurations = isAutoDetection.get()
        settingsState.isBeanFilterEnabled = isBeanFilterEnabled.get()
        settingsState.sqlLanguageId = sqlLanguageIdBind.get()

        ProjectUtil.getActiveProject()?.let { project -> UastModelTrackerInvalidateAction.invalidate(project) }
    }

    override fun getDisplayName(): String = "Run Configurations"

    companion object {
        const val ID = "com.explyt.spring.runConfigurations"
    }
}
