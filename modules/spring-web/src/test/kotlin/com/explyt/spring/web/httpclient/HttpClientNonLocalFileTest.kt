/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.httpclient

import com.explyt.spring.web.httpclient.action.HttpRunFileAction
import com.explyt.spring.web.language.http.psi.HttpFile
import com.explyt.spring.web.providers.HttpRunLineMarkerProvider

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

class HttpClientNonLocalFileTest : BasePlatformTestCase() {

    fun testNoRunGutterForFileInsideJar() {
        val file = httpFileInsideJar()
        assertTrue(file.leaves().isNotEmpty())
        assertNull(file.runGutter())
    }

    fun testRunGutterForLocalFile() {
        val file = localHttpFile()
        assertNotNull(file.runGutter())
    }

    fun testNoRunnerPanelForFileInsideJar() {
        val file = httpFileInsideJar()
        assertNull(HttpRunnerEditorPanelProvider().collectNotificationData(project, file.virtualFile))
    }

    fun testRunnerPanelForLocalFile() {
        val file = localHttpFile()
        assertNotNull(HttpRunnerEditorPanelProvider().collectNotificationData(project, file.virtualFile))
    }

    fun testRunCommandIgnoresFileInsideJar() {
        val file = httpFileInsideJar()
        HttpRunFileAction.runHttpCommand(project, file.virtualFile)
    }

    private fun localHttpFile(): PsiFile {
        val path = newTempDirectory().resolve(FILE_NAME)
        path.writeText(REQUESTS)
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)!!
        assertNotNull(virtualFile.fileSystem.getNioPath(virtualFile))
        return httpPsiOf(virtualFile)
    }

    private fun httpFileInsideJar(): PsiFile {
        val jarPath = newTempDirectory().resolve("requests.jar")
        ZipOutputStream(jarPath.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(FILE_NAME))
            zip.write(REQUESTS.toByteArray())
            zip.closeEntry()
        }
        val jar = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(jarPath)!!
        val entry = JarFileSystem.getInstance().getJarRootForLocalFile(jar)!!.findFileByRelativePath(FILE_NAME)!!
        assertNull(entry.fileSystem.getNioPath(entry))
        assertThrows(UnsupportedOperationException::class.java) { entry.toNioPath() }
        return httpPsiOf(entry)
    }

    private fun httpPsiOf(file: VirtualFile): PsiFile =
        PsiManager.getInstance(project).findFile(file).also { assertInstanceOf(it, HttpFile::class.java) }!!

    private fun newTempDirectory(): Path = FileUtil.createTempDirectory("http-client", null, true).toPath()

    private fun PsiFile.leaves(): Collection<LeafPsiElement> =
        PsiTreeUtil.collectElementsOfType(this, LeafPsiElement::class.java)

    private fun PsiFile.runGutter() = leaves().firstNotNullOfOrNull { HttpRunLineMarkerProvider().getInfo(it) }

    private companion object {
        const val FILE_NAME = "requests.http"
        const val REQUESTS = "GET http://localhost:8080/api/items\n"
    }
}
