/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.editor.openapi

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefClient
import org.jetbrains.concurrency.runAsync
import java.beans.PropertyChangeListener
import java.util.*

class OpenApiCefBrowser(
    file: VirtualFile
) : UserDataHolderBase(), FileEditor {

    private val specKey = UUID.randomUUID()
    private var disposed = false
    private val browser: JBCefBrowser
    private val jbCefClient: JBCefClient

    init {
        jbCefClient = JBCefApp.getInstance()
            .createClient()

        browser = JBCefBrowser.createBuilder()
            .setClient(jbCefClient)
            .build()

        jbCefClient.addRequestHandler(OpenApiCefRequestHandlerAdapter(), browser.cefBrowser)
        jbCefClient.addDownloadHandler(OpenApiCefDownloadHandler(), browser.cefBrowser)

        OpenApiUtils.cacheFile(specKey, file)
        loadHtml()
    }

    fun loadHtml(anchor: String = "") {
        val path = OpenApiUtils.resourceUrl(specKey, "index.html$anchor")

        runAsync {
            browser.loadURL(path)
        }
    }

    override fun isModified() = false
    override fun isValid() = !disposed
    override fun getComponent() = browser.component
    override fun getPreferredFocusedComponent() = browser.component
    override fun getName() = "OpenAPI UI"

    override fun dispose() {
        Disposer.dispose(browser)
        Disposer.dispose(jbCefClient)
        disposed = true
    }

    override fun setState(state: FileEditorState) {}
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

}