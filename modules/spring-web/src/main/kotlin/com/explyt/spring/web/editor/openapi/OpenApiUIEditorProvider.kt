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

package com.explyt.spring.web.editor.openapi

import com.intellij.json.JsonFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefApp
import org.jetbrains.yaml.YAMLFileType

class OpenApiUIEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (file.fileType !is YAMLFileType && file.fileType !is JsonFileType) return false

        return ApplicationManager.getApplication().runReadAction(
            Computable { OpenApiUtils.isOpenApiFile(project, file) }
        )
    }

    override fun acceptRequiresReadAction() = false

    override fun getEditorTypeId(): String {
        return OpenApiUtils.OPENAPI_EDITOR_TYPE_ID
    }

    override fun getPolicy(): FileEditorPolicy {
        @Suppress("UnstableApiUsage")
        return FileEditorPolicy.HIDE_OTHER_EDITORS
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val textEditor = createTextEditor(project, file)
        if (!JBCefApp.isSupported()) return textEditor
        val preview = OpenApiCefBrowser(file)

        return OpenApiUIEditor(textEditor, preview)
    }

    private fun createTextEditor(project: Project, file: VirtualFile) =
        TextEditorProvider.getInstance().createEditor(project, file) as TextEditor

}