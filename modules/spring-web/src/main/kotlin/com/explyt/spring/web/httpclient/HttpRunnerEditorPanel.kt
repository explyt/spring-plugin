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

package com.explyt.spring.web.httpclient

import com.explyt.spring.web.httpclient.action.EnvironmentalAddFileAction
import com.explyt.spring.web.httpclient.action.EnvironmentalRemoveFileAction
import com.explyt.spring.web.httpclient.action.HttpRunFileAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI.Borders
import org.jetbrains.kotlin.idea.base.util.onTextChange
import java.awt.BorderLayout
import java.util.function.Function
import kotlin.io.path.exists

class HttpRunnerEditorPanelProvider : EditorNotificationProvider {
    override fun collectNotificationData(
        project: Project, file: VirtualFile
    ): Function<FileEditor, HttpRunnerEditorPanel>? {
        return if (file.name.endsWith(".http") || file.name.endsWith(".rest")) {
            Function { fileEditor: FileEditor -> HttpRunnerEditorPanel(fileEditor, project) }
        } else null
    }
}

class HttpRunnerEditorPanel(fileEditor: FileEditor, val project: Project) :
    EditorNotificationPanel(fileEditor, getToolbarBackground()) {

    private val clientEnvDefault = "http-client.env.json"
    private val clientPrivateEnvDefault = "http-client.private.env.json"

    private val envDataHolder = EnvDataHolder(fileEditor.file.toNioPath())

    init {
        addDefaultEnvFiles(fileEditor.file)
        border = Borders.empty()

        val httpGroup = DefaultActionGroup()
        httpGroup.addAction(HttpRunFileAction())
        httpGroup.addSeparator()
        httpGroup.addAction(EnvironmentalAddFileAction(envDataHolder))
        httpGroup.addAction(EnvironmentalRemoveFileAction(envDataHolder))
        httpGroup.addSeparator()

        removeAll()
        val toolbar = ActionManager.getInstance().createActionToolbar("Explyt.HttpRunnerEditorToolbar", httpGroup, true)
        toolbar.targetComponent = this
        add(toolbar.component, BorderLayout.WEST)

        val panel = panel {
            row {
                panel {
                    row("Environmental files:") {
                        comboBox(envDataHolder.envFiles)
                            .align(AlignX.FILL)
                            .applyToComponent { addActionListener { envDataHolder.selectFile(project) } }
                            .resizableColumn()

                        label("Env:")

                        comboBox(envDataHolder.envModel)
                            .align(AlignX.FILL)
                            .resizableColumn()
                            .applyToComponent { addActionListener { envDataHolder.selectEnv() } }
                            .visibleIf(envDataHolder.envFileIsJson)
                    }
                }.visibleIf(envDataHolder.envPanelVisible)

                textField()
                    .align(AlignX.FILL)
                    .resizableColumn()
                    .applyToComponent { toolTipText = "Additional command line args" }
                    .applyToComponent { emptyText.text = toolTipText }
                    .applyToComponent { this.onTextChange { envDataHolder.setArgs() } }
                    .bindText(envDataHolder.additionalArgsBind)

            }
        }

        myLinksPanel.add(panel)
        add(myLinksPanel, BorderLayout.CENTER)
    }

    private fun addDefaultEnvFiles(file: VirtualFile?) {
        val parentPath = file?.toNioPath()?.parent ?: return
        val envDefault = parentPath.resolve(clientEnvDefault)
        val envPrivateDefault = parentPath.resolve(clientPrivateEnvDefault)
        if (envDefault.exists()) {
            envDataHolder.addFile(envDefault)
        }
        if (envPrivateDefault.exists()) {
            envDataHolder.addFile(envPrivateDefault)
        }
        envDataHolder.init(project)
    }
}