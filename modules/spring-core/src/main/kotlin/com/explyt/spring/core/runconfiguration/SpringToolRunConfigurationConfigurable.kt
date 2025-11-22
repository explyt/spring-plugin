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
import com.explyt.spring.core.util.ZipDownloader
import com.intellij.execution.RunManager
import com.intellij.ide.impl.ProjectUtil
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.emptyText
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.injection.Injectable
import com.intellij.sh.run.ShConfigurationType
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.concurrency.AppExecutorUtil
import org.intellij.plugins.intelliLang.inject.InjectedLanguage
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JList
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

private const val articleUrl =
    "https://medium.com/@explytspring/stop-playing-catch-up-with-spring-introducing-the-explyt-spring-plugin-for-idea-community-0be380b36a75"
private const val articleJavaagentUrl =
    "https://medium.com/@explytspring/explyt-spring-plugin-patching-spring-bytecode-to-enhance-application-context-recognition-0817fb52b056"

class SpringToolRunConfigurationConfigurable : SearchableConfigurable {
    private val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()

    private val propertyGraph = PropertyGraph()

    private val isAutoDetection = propertyGraph.property(false)
    private val isBeanFilterEnabled = propertyGraph.property(false)
    private val isCollectStatisticBind = propertyGraph.property(false)
    private val isShowFloatingRefreshActionBind = propertyGraph.property(false)
    private val isDebugModeBind = propertyGraph.property(false)
    private val isJavaAgentModeBind = propertyGraph.property(false)
    private val useClassNameForJavaAgentModeBind = propertyGraph.property(true)
    private val httpCliPathBind = propertyGraph.property("")
    private val sqlLanguageIdModel = CollectionComboBoxModel(getAvailableLanguages())
    private val shellScriptEnabledProperty: AtomicBooleanProperty = AtomicBooleanProperty(shellScriptEnabled())
    private val downloadEnabled: AtomicBooleanProperty = AtomicBooleanProperty(true)

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

                row {
                    checkBox(message("explyt.spring.settings.debug.label"))
                        .align(AlignX.FILL)
                        .bindSelected(isDebugModeBind)
                        .resizableColumn()
                }

                row {
                    checkBox(message("explyt.spring.settings.javaagent.label"))
                        .align(AlignX.FILL)
                        .applyToComponent { toolTipText = message("explyt.spring.settings.javaagent.tooltip") }
                        .bindSelected(isJavaAgentModeBind)
                        .comment("<a href=\"$articleUrl\">Article one</a> and <a href=\"$articleJavaagentUrl\">article two</a> about this")
                        .resizableColumn()
                }

                row {
                    checkBox(message("explyt.spring.settings.javaagent.springboot.label"))
                        .align(AlignX.FILL)
                        .bindSelected(useClassNameForJavaAgentModeBind)
                        .resizableColumn()
                }

                row(message("explyt.spring.settings.sql.language.id.label")) {
                    comboBox(sqlLanguageIdModel, getLanguageCellRenderer())
                        .resizableColumn()
                        .comment(message("explyt.spring.settings.sql.language.id.tooltip"))
                }

                row(message("explyt.spring.settings.http.cli.path")) {
                    textFieldWithBrowseButton(
                        message("explyt.spring.settings.http.cli.path"), null, { it.path }
                    )
                        .bindText(httpCliPathBind)
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .applyToComponent { toolTipText = message("explyt.spring.settings.http.cli.path") }
                        .applyToComponent { emptyText.text = toolTipText }
                        .comment(message("explyt.spring.settings.http.cli.comment"))
                    button(
                        message("explyt.spring.settings.http.cli.download.button"),
                        CliDownloadAction(downloadEnabled, httpCliPathBind)
                    )
                        .applyToComponent { toolTipText = message("explyt.spring.settings.http.cli.path.tooltip") }
                        .enabledIf(downloadEnabled)

                    label("").align(AlignX.LEFT)
                        //https://plugins.jetbrains.com/docs/intellij/loader.html#when-to-use
                        .applyToComponent { icon = AnimatedIcon.Default() }
                        .visibleIf(downloadEnabled.not())
                }.visibleIf(shellScriptEnabledProperty)
            }
        }
    }

    override fun reset() {
        isAutoDetection.set(settingsState.isAutoDetectConfigurations)
        isBeanFilterEnabled.set(settingsState.isBeanFilterEnabled)
        isCollectStatisticBind.set(settingsState.isCollectStatistic)
        isShowFloatingRefreshActionBind.set(settingsState.isShowFloatingRefreshAction)
        isDebugModeBind.set(settingsState.isDebugMode)
        isJavaAgentModeBind.set(settingsState.isJavaAgentMode)
        useClassNameForJavaAgentModeBind.set(settingsState.useSpringBootClassNameForJavaAgentMode)
        sqlLanguageIdModel.selectedItem = getCurrentLanguageId()
        httpCliPathBind.set(settingsState.httpCliPath ?: "")
    }

    override fun isModified(): Boolean {
        if (settingsState.isAutoDetectConfigurations != isAutoDetection.get()) return true
        if (settingsState.isBeanFilterEnabled != isBeanFilterEnabled.get()) return true
        if (settingsState.isCollectStatistic != isCollectStatisticBind.get()) return true
        if (settingsState.isShowFloatingRefreshAction != isShowFloatingRefreshActionBind.get()) return true
        if (settingsState.isDebugMode != isDebugModeBind.get()) return true
        if (settingsState.isJavaAgentMode != isJavaAgentModeBind.get()) return true
        if (settingsState.useSpringBootClassNameForJavaAgentMode != useClassNameForJavaAgentModeBind.get()) return true
        if ((settingsState.sqlLanguageId ?: "") != (sqlLanguageIdModel.selected?.id ?: "")) return true
        if ((settingsState.httpCliPath ?: "") != (httpCliPathBind.get())) return true
        return false
    }

    override fun apply() {
        StatisticService.getInstance().addActionUsage(StatisticActionId.SETTINGS_CHANGED)
        settingsState.isAutoDetectConfigurations = isAutoDetection.get()
        settingsState.isBeanFilterEnabled = isBeanFilterEnabled.get()
        settingsState.isCollectStatistic = isCollectStatisticBind.get()
        settingsState.isShowFloatingRefreshAction = isShowFloatingRefreshActionBind.get()
        settingsState.isDebugMode = isDebugModeBind.get()
        settingsState.isJavaAgentMode = isJavaAgentModeBind.get()
        settingsState.useSpringBootClassNameForJavaAgentMode = useClassNameForJavaAgentModeBind.get()
        settingsState.sqlLanguageId = sqlLanguageIdModel.selected?.id
        settingsState.httpCliPath = httpCliPathBind.get()

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

        fun shellScriptEnabled(): Boolean {
            val project = ProjectUtil.getActiveProject() ?: return false
            return try {
                RunManager.getInstance(project).getConfigurationTemplate(ShConfigurationType.getInstance())
                true
            } catch (_: NoClassDefFoundError) {
                false
            }
        }
    }
}

private class CliDownloadAction(
    val downloadEnabled: AtomicBooleanProperty, val httpCliPathBind: GraphProperty<String>
) : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        downloadEnabled.set(false)
        AppExecutorUtil.getAppExecutorService().submit {
            val downloadPath = try {
                ZipDownloader.download(
                    Registry.stringValue("explyt.http.cli.url"),
                    Path(PathManager.getTempPath(), "explyt.zip")
                )
            } finally {
                downloadEnabled.set(true)
            }

            getResultPath(downloadPath)?.let { path ->
                ApplicationManager.getApplication().invokeLater {
                    httpCliPathBind.set(path.absolutePathString())
                }
            }
        }
    }

    private fun getResultPath(resultDir: Path?): Path? {
        return resultDir?.toFile()?.listFiles()?.first { it.isDirectory }?.toPath()
    }
}
