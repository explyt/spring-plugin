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
import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.language.profiles.ProfilesLanguage
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.ide.impl.ProjectUtil
import com.intellij.lang.Language
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.psi.injection.Injectable
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import org.intellij.plugins.intelliLang.inject.InjectedLanguage
import javax.swing.JComponent
import javax.swing.JList


class SpringToolRunConfigurationConfigurable : SearchableConfigurable {
    private val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()

    private val propertyGraph = PropertyGraph()

    private val isAutoDetection = propertyGraph.property(false)
    private val isBeanFilterEnabled = propertyGraph.property(false)
    private val isCollectStatisticBind = propertyGraph.property(false)
    private val isShowFloatingRefreshActionBind = propertyGraph.property(false)
    private val sqlLanguageIdModel = CollectionComboBoxModel(getAvailableLanguages())

    override fun getId(): String = ID

    override fun createComponent(): JComponent {
        StatisticService.getInstance().addActionUsage(StatisticActionId.SETTINGS_OPEN)
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

                row {
                    checkBox(message("explyt.spring.settings.collect.statistic.label"))
                        .align(AlignX.FILL)
                        .bindSelected(isCollectStatisticBind)
                        .resizableColumn()
                }

                row {
                    checkBox(message("explyt.spring.settings.floating.action.show.label"))
                        .align(AlignX.FILL)
                        .bindSelected(isShowFloatingRefreshActionBind)
                        .resizableColumn()
                }

                row(message("explyt.spring.settings.sql.language.id.label")) {
                    comboBox(sqlLanguageIdModel, getLanguageCellRenderer())
                        .resizableColumn()
                        .comment(message("explyt.spring.settings.sql.language.id.tooltip"))
                }
            }
        }
    }

    override fun reset() {
        isAutoDetection.set(settingsState.isAutoDetectConfigurations)
        isBeanFilterEnabled.set(settingsState.isBeanFilterEnabled)
        isCollectStatisticBind.set(settingsState.isCollectStatistic)
        isShowFloatingRefreshActionBind.set(settingsState.isShowFloatingRefreshAction)
        sqlLanguageIdModel.selectedItem = getCurrentLanguageId()
    }

    override fun isModified(): Boolean {
        if (settingsState.isAutoDetectConfigurations != isAutoDetection.get()) return true
        if (settingsState.isBeanFilterEnabled != isBeanFilterEnabled.get()) return true
        if (settingsState.isCollectStatistic != isCollectStatisticBind.get()) return true
        if (settingsState.isShowFloatingRefreshAction != isShowFloatingRefreshActionBind.get()) return true
        if ((settingsState.sqlLanguageId ?: "") != (sqlLanguageIdModel.selected?.id ?: "")) return true
        return false
    }

    override fun apply() {
        StatisticService.getInstance().addActionUsage(StatisticActionId.SETTINGS_CHANGED)
        settingsState.isAutoDetectConfigurations = isAutoDetection.get()
        settingsState.isBeanFilterEnabled = isBeanFilterEnabled.get()
        settingsState.isCollectStatistic = isCollectStatisticBind.get()
        settingsState.isShowFloatingRefreshAction = isShowFloatingRefreshActionBind.get()
        settingsState.sqlLanguageId = sqlLanguageIdModel.selected?.id

        ProjectUtil.getActiveProject()?.let { project -> UastModelTrackerInvalidateAction.invalidate(project) }
    }

    override fun getDisplayName(): String = Constants.SYSTEM_ID.readableName

    private fun getCurrentLanguageId(): Injectable? {
        if (settingsState.sqlLanguageId.isNullOrEmpty()) return null
        return sqlLanguageIdModel.items.find { it?.id == settingsState.sqlLanguageId }
    }

    private fun getAvailableLanguages(): List<Injectable?> {
        val languages = InjectedLanguage.getAvailableLanguages()
        val list: MutableList<Injectable> = ArrayList()
        for (language in languages) {
            if (skipLanguage(language)) continue
            list.add(Injectable.fromLanguage(language))
        }

        return listOf<Injectable?>(null) + list.sortedBy { getSortKey(it) }
    }

    private fun getSortKey(it: Injectable): String {
        val lowercase = it.displayName.lowercase()
        val prefixKey = when {
            lowercase.contains("sql") -> "0"
            lowercase.contains("ql") -> "1"
            else -> "2"
        }
        return "${prefixKey}${it.displayName}"
    }

    private fun skipLanguage(language: Language): Boolean {
        if (language.id == ProfilesLanguage.INSTANCE.id) return true

        val name = language.displayName.lowercase()
        if (name.contains("dtd")) return true
        if (name.contains("xml")) return true
        if (name.contains("html")) return true
        if (name.contains("yaml")) return true
        if (name.contains("gradle")) return true
        if (name.contains("toml")) return true
        if (name.contains("svg")) return true
        if (name.contains("regexp")) return true
        if (name.contains("markdown")) return true
        if (name.contains("json")) return true
        if (name.contains("java")) return true
        if (name.contains("kotlin")) return true
        if (name.contains("groovy")) return true
        if (name.startsWith("properties")) return true
        if (name.startsWith(".gitignore")) return true
        if (name.startsWith(".hgignore")) return true
        if (name.startsWith(".ignore")) return true
        if (name.startsWith("exclude")) return true
        if (name.startsWith("textmate")) return true
        return false
    }

    private fun getLanguageCellRenderer(): ColoredListCellRenderer<Injectable> {
        return object : ColoredListCellRenderer<Injectable>() {
            override fun customizeCellRenderer(
                list: JList<out Injectable>, language: Injectable?, index: Int, selected: Boolean, hasFocus: Boolean
            ) {
                if (language == null) {
                    append("Choose Custom SQL Language ID", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    return
                }
                icon = language.icon
                append(language.displayName)
                val description = language.additionalDescription
                if (description != null) {
                    append(description, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        }
    }

    companion object {
        const val ID = "com.explyt.spring.runConfigurations"
    }
}
