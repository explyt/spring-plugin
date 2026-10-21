/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.test.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.PsiTestUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.ServiceLoader
import java.util.jar.JarOutputStream
import javax.tools.DiagnosticCollector
import javax.tools.JavaCompiler
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.io.path.writeText

/**
 * A library that ships **compiled classes only**, the way a company starter arrives from a Maven repository.
 *
 * Java sources are compiled with the running JDK's `javac` against the libraries [Module] already has, packed into a
 * jar in a directory outside every content root, and attached to the module as a classes root. The classes are then
 * outside `ProjectScope`, resolve through stubs and have no attached sources - the shape that
 * [TestLibrarySourceRoot] cannot express, because its root is a sources root too.
 */
class TestCompiledLibrary private constructor(
    val jarRoot: VirtualFile,
    private val libraryName: String,
    private val directory: Path,
    private val jarName: String,
) {

    /** Attaches the same jar to [module] as well, the way two modules depending on one artifact see it. */
    fun attachTo(module: Module, parentDisposable: Disposable) {
        PsiTestUtil.addLibrary(parentDisposable, module, libraryName, directory.toString(), jarName)
    }

    companion object {

        /**
         * Compiles [sources] (relative path to Java text), packs the classes with [resources] (relative path to text)
         * and attaches the jar to [module] as [libraryName]. The library, the jar and its directory are removed when
         * [parentDisposable] is disposed.
         */
        @OptIn(ExperimentalPathApi::class)
        fun attach(
            module: Module,
            parentDisposable: Disposable,
            libraryName: String,
            sources: Map<String, String>,
            resources: Map<String, String> = emptyMap(),
        ): TestCompiledLibrary {
            val rootPath = Files.createTempDirectory(libraryName).toRealPath()
            Disposer.register(parentDisposable) { rootPath.deleteRecursively() }
            VfsRootAccess.allowRootAccess(parentDisposable, rootPath.toString())

            val classes = compile(sources, rootPath, classpathOf(module))
            resources.forEach { (relativePath, text) ->
                classes.resolve(relativePath).also { it.parent.createDirectories() }.writeText(text)
            }
            val jarName = "$libraryName.jar"
            pack(classes, rootPath.resolve(jarName))

            PsiTestUtil.addLibrary(parentDisposable, module, libraryName, rootPath.toString(), jarName)
            val jarRoot = JarFileSystem.getInstance().refreshAndFindFileByPath("${rootPath.resolve(jarName)}!/")
                ?: error("No jar root for library '$libraryName'")
            return TestCompiledLibrary(jarRoot, libraryName, rootPath, jarName)
        }

        private fun classpathOf(module: Module): String = ReadAction.compute<String, RuntimeException> {
            OrderEnumerator.orderEntries(module).withoutSdk().librariesOnly().classes().roots
                .map { JarFileSystem.getInstance().getVirtualFileForJar(it) ?: it }
                .joinToString(File.pathSeparator) { VfsUtilCore.virtualToIoFile(it).path }
        }

        private fun compile(sources: Map<String, String>, rootPath: Path, classpath: String): Path {
            val sourceRoot = rootPath.resolve("src")
            val classes = rootPath.resolve("classes").createDirectories()
            val sourceFiles = sources.map { (relativePath, text) ->
                sourceRoot.resolve(relativePath).also {
                    it.parent.createDirectories()
                    it.writeText(text)
                }.toFile()
            }

            val compiler = ToolProvider.getSystemJavaCompiler()
                ?: ServiceLoader.load(ModuleLayer.boot(), JavaCompiler::class.java).firstOrNull()
                ?: error("The test JDK at ${System.getProperty("java.home")} provides no Java compiler")
            val diagnostics = DiagnosticCollector<JavaFileObject>()
            compiler.getStandardFileManager(diagnostics, null, Charsets.UTF_8).use { files ->
                val options = listOf("-d", classes.toString(), "-classpath", classpath, "--release", "17", "-proc:none")
                val compiled = compiler
                    .getTask(null, files, diagnostics, options, null, files.getJavaFileObjectsFromFiles(sourceFiles))
                    .call()
                check(compiled) { "Library sources do not compile: ${diagnostics.diagnostics.joinToString("\n")}" }
            }
            return classes
        }

        @OptIn(ExperimentalPathApi::class)
        private fun pack(classes: Path, jar: Path) {
            JarOutputStream(Files.newOutputStream(jar)).use { out ->
                classes.walk().filter { it.isRegularFile() }.sorted().forEach { file ->
                    out.putNextEntry(JarEntry(file.relativeTo(classes).joinToString("/")))
                    Files.copy(file, out)
                    out.closeEntry()
                }
            }
        }
    }
}
