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

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.jetbrains.concurrency.runAsync
import java.beans.PropertyChangeListener
import java.util.*

class OpenApiCefBrowser(
    file: VirtualFile
) : UserDataHolderBase(), FileEditor {

    private val specKey = UUID.randomUUID()
    private var disposed = false
    private val browser: JBCefBrowser

    init {
        val jbCefClient = JBCefApp.getInstance()
            .createClient()

        browser = JBCefBrowser.createBuilder()
            .setClient(jbCefClient)
            .build()

        jbCefClient.addRequestHandler(OpenApiCefRequestHandlerAdapter(), browser.cefBrowser)

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
        disposed = true
    }

    override fun setState(state: FileEditorState) {}
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

}