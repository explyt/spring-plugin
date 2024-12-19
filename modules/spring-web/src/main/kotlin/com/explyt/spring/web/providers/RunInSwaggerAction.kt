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

package com.explyt.spring.web.providers

import ai.grazie.utils.mpp.UUID
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.builder.openapi.OpenApiBuilderFactory
import com.explyt.spring.web.editor.openapi.OpenApiUIEditor
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.yaml.YAMLFileType
import java.io.File

class RunInSwaggerAction(private val endpointInfos: List<AddEndpointToOpenApiIntention.EndpointInfo>, private val servers: List<String>) :
    AnAction({ "Open in Swagger UI" }, AllIcons.RunConfigurations.TestState.Run) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_OPENAPI_CONTROLLER_OPEN_IN_SWAGGER)

        val fileType = YAMLFileType.YML

        val file = File.createTempFile(UUID.random().text, ".${fileType.defaultExtension}")
        file.deleteOnExit()
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return

        val openapiBuilder = OpenApiBuilderFactory.getOpenApiFileBuilder(fileType)
        for (server in servers) {
            openapiBuilder.addServer(server)

        }
        for (info in endpointInfos) {
            openapiBuilder.addEndpoint(info)
        }

        runWriteAction {
            VfsUtil.saveText(virtualFile, openapiBuilder.toString())
            val openFileDescriptor = OpenFileDescriptor(project, virtualFile)
            val openapiEditor = FileEditorManager.getInstance(project)
                .openEditor(openFileDescriptor, true)
                .firstNotNullOfOrNull { it as? OpenApiUIEditor }
                ?: return@runWriteAction
            openapiEditor.showPreview("", TextEditorWithPreview.Layout.SHOW_PREVIEW)
        }
    }

}