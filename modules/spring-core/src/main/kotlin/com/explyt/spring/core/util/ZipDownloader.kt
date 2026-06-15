/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
import com.intellij.openapi.util.io.NioFiles
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
            NioFiles.deleteRecursively(extractDirectory)
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
            FileUtil.delete(zipPath.toFile())
        }
        return null
    }

    private fun makeRunnable(extractPath: Path) {
        val path = extractPath.resolve("ijhttp").resolve("ijhttp").takeIf { it.exists() } ?: return
        val permissions = PosixFilePermissions.fromString("rwxr-xr-x")
        Files.setPosixFilePermissions(path, permissions)
    }
}