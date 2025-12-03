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

import com.intellij.ide.actions.RevealFileAction
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import org.cef.browser.CefBrowser
import org.cef.callback.CefBeforeDownloadCallback
import org.cef.callback.CefDownloadItem
import org.cef.callback.CefDownloadItemCallback
import org.cef.handler.CefDownloadHandlerAdapter
import java.io.File

class OpenApiCefDownloadHandler : CefDownloadHandlerAdapter() {

    override fun onBeforeDownload(
        browser: CefBrowser?,
        downloadItem: CefDownloadItem?,
        suggestedName: String?,
        callback: CefBeforeDownloadCallback?
    ): Boolean {
        callback ?: return false
        if (Registry.`is`("explyt.openapi.download.silent.mode")) {
            callback.Continue("", false)
            return false
        } else if (SystemInfo.isMac || SystemInfo.isWindows) {
            callback.Continue("", true)
            return true
        } else {
            callback.Continue("", false)
            return false
        }
    }

    override fun onDownloadUpdated(
        browser: CefBrowser?,
        downloadItem: CefDownloadItem?,
        callback: CefDownloadItemCallback?
    ) {
        if (downloadItem?.isComplete == true) {
            val file = File(downloadItem.fullPath)
            Notification(
                "com.explyt.spring.notification.web",
                "Download completed successfully",
                NotificationType.INFORMATION
            )
                .addAction(NotificationAction.create(file.name) { _ -> RevealFileAction.openFile(file) })
                .notify(null)
        }
    }
}