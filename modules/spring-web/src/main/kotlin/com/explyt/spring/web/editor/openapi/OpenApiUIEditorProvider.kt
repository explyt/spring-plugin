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