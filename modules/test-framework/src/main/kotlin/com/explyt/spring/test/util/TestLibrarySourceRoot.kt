/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.test.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.IndexingTestUtil
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

/**
 * A library whose **sources** root holds real files, the way a dependency's attached source jar holds the code
 * a developer reads in the editor.
 *
 * The root is a directory outside every project content root, because that is what makes the platform report
 * its files through [com.intellij.openapi.roots.ProjectFileIndex.isInLibrarySource] *and* resolve no module for
 * them. A directory inside the fixture's own content root is both a library root and module content, so
 * `ModuleUtilCore.findModuleForPsiElement` still answers and a defect specific to library PSI stays hidden.
 */
class TestLibrarySourceRoot private constructor(
    private val project: Project,
    private val root: VirtualFile,
    private val name: String
) {

    /** Writes [text] to [relativePath] inside this library's sources root. */
    fun addFile(relativePath: String, text: String): PsiFile {
        val file = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val directoryPath = relativePath.substringBeforeLast('/', "")
            val directory = if (directoryPath.isEmpty()) root else VfsUtil.createDirectoryIfMissing(root, directoryPath)
            val created = directory.createChildData(root, relativePath.substringAfterLast('/'))
            VfsUtil.saveText(created, text)
            created
        }

        IndexingTestUtil.waitUntilIndexesAreReady(project)
        return PsiManager.getInstance(project).findFile(file)
            ?: error("No PSI for library source '$relativePath'")
    }

    /** Attaches this library to [module] as well, mirroring two modules that depend on the same artifact. */
    fun attachTo(module: Module) {
        val library = LibraryTablesRegistrar.getInstance().getLibraryTable(project).getLibraryByName(name)
            ?: error("Library '$name' is gone")
        WriteAction.runAndWait<RuntimeException> { ModuleRootModificationUtil.addDependency(module, library) }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    companion object {

        /**
         * Creates a library named [libraryName] attached to [module], with an empty sources root on disk. The
         * directory and the library are removed when [parentDisposable] is disposed.
         */
        @OptIn(ExperimentalPathApi::class)
        fun create(module: Module, parentDisposable: Disposable, libraryName: String): TestLibrarySourceRoot {
            val rootPath = Files.createTempDirectory(libraryName).toRealPath()
            Disposer.register(parentDisposable) { rootPath.deleteRecursively() }
            VfsRootAccess.allowRootAccess(parentDisposable, rootPath.toString())

            val root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(rootPath)
                ?: error("No VirtualFile for library root '$rootPath'")

            val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(module.project)
            WriteAction.runAndWait<RuntimeException> {
                val library = libraryTable.createLibrary(libraryName)
                library.modifiableModel.apply {
                    addRoot(root, OrderRootType.SOURCES)
                    addRoot(root, OrderRootType.CLASSES)
                    commit()
                }
                ModuleRootModificationUtil.addDependency(module, library)
            }
            Disposer.register(parentDisposable) {
                WriteAction.runAndWait<RuntimeException> {
                    libraryTable.getLibraryByName(libraryName)?.let { libraryTable.removeLibrary(it) }
                }
            }

            IndexingTestUtil.waitUntilIndexesAreReady(module.project)
            return TestLibrarySourceRoot(module.project, root, libraryName)
        }
    }
}
