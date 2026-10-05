/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

/**
 * Where an element lives, in terms that leave the machine.
 *
 * A project file has a [filePath] relative to the project root, which may start with `../` for a module kept outside
 * the project directory; [McpSourceLocation.resolveInputPath] reads such a path back. An element inside a jar, in a
 * library or in the JDK has no path an agent can open and is named by its [library] instead: the jar's file name,
 * else the library's or the JDK's name. Both are `null` for a file the project can relate to neither, since the only
 * remaining description is an absolute path of this machine.
 */
data class McpSourceLocation(val filePath: String?, val library: String?) {

    companion object {
        val UNKNOWN = McpSourceLocation(null, null)

        fun of(element: PsiElement, project: Project): McpSourceLocation =
            element.containingFile?.virtualFile?.let { of(it, project) } ?: UNKNOWN

        fun of(file: VirtualFile, project: Project): McpSourceLocation {
            JarFileSystem.getInstance().getVirtualFileForJar(file)?.let { return McpSourceLocation(null, it.name) }
            val basePath = project.basePath?.let { "$it/" }
            if (basePath != null && file.path.startsWith(basePath)) {
                return McpSourceLocation(file.path.removePrefix(basePath), null)
            }
            val fileIndex = ProjectFileIndex.getInstance(project)
            if (fileIndex.isInContent(file)) {
                return McpSourceLocation(contentRelativePathOf(file, project.basePath, fileIndex), null)
            }
            return McpSourceLocation(null, libraryNameOf(file, fileIndex))
        }

        /** The local file a project-relative [filePath] names, `../` segments included; `null` when there is none. */
        fun resolveInputPath(filePath: String, basePath: String): String =
            FileUtil.toCanonicalPath("$basePath/$filePath")

        private fun contentRelativePathOf(file: VirtualFile, basePath: String?, fileIndex: ProjectFileIndex): String? {
            if (file.fileSystem.protocol == StandardFileSystems.FILE_PROTOCOL) {
                return basePath?.let { FileUtil.getRelativePath(it, file.path, '/') }
            }
            val contentRoot = fileIndex.getContentRootForFile(file, false) ?: return null
            return VfsUtilCore.getRelativePath(file, contentRoot)
        }

        private fun libraryNameOf(file: VirtualFile, fileIndex: ProjectFileIndex): String? =
            fileIndex.getOrderEntriesForFile(file).firstNotNullOfOrNull { entry ->
                when (entry) {
                    is LibraryOrderEntry -> entry.libraryName
                    is JdkOrderEntry -> entry.jdkName
                    else -> null
                }
            }
    }
}
