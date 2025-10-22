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

package com.explyt.spring.core.util

import com.explyt.spring.core.notifications.SpringToolNotificationGroup
import com.intellij.execution.wsl.WslPath
import com.intellij.ide.impl.ProjectUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.ZipUtil
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

object ZipDownloader {
    fun download(urlString: String, targetPath: Path): Path? {
        return downloadFile(urlString, targetPath)
    }

    private fun downloadFile(
        urlString: String, targetPath: Path, indicator: BackgroundableProcessIndicator? = null
    ): Path? {
        return try {
            indicator?.apply { text = "Downloading from: $urlString" }
            HttpRequests.request(urlString)
                .forceHttps(false)
                .connectTimeout(30_000)
                .readTimeout(30_000)
                .saveToFile(targetPath, indicator)
            unzip(targetPath)
        } catch (t: Throwable) {
            val errorMessage = t.message ?: "Download is cancelled"
            SpringToolNotificationGroup
                .createNotification(errorMessage, NotificationType.WARNING).notify(ProjectUtil.getActiveProject())
            null
        }
    }

    private fun unzip(zipPath: Path): Path? {
        val extractDirectory = zipPath.parent.resolve("extracted")
        try {
            FileUtil.deleteRecursively(extractDirectory)
        } catch (_: Exception) {
        }
        try {
            ZipUtil.extract(zipPath, extractDirectory, null)
            if (!SystemInfo.isWindows || WslPath.isWslUncPath(extractDirectory.absolutePathString())) {
                makeRunnable(extractDirectory)
            }
            return extractDirectory
        } catch (e: IOException) {
            val notification = e.message?.let {
                Notification(
                    "explytSpringInitializrNotificationGroup",
                    "Unzipping problem",
                    it,
                    NotificationType.ERROR
                )
            }
            notification?.notify(ProjectUtil.getActiveProject())
        } finally {
            FileUtil.delete(zipPath)
        }
        return null
    }

    private fun makeRunnable(extractPath: Path) {
        val path = extractPath.resolve("ijhttp").resolve("ijhttp").takeIf { it.exists() } ?: return
        val permissions = PosixFilePermissions.fromString("rwxr-xr-x")
        Files.setPosixFilePermissions(path, permissions)
    }
}