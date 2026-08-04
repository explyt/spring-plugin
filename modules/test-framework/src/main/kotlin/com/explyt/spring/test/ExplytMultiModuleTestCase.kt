/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.test

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase

/**
 * Base class for tests that need **several** modules with real dependencies between them.
 *
 * The light fixtures ([ExplytBaseLightTestCase], [ExplytInspectionBaseTestCase]) always create a
 * single module, so they cannot express "the configuration file lives in the application module
 * while the consuming code lives in a dependency module". Anything that resolves through a
 * module-based [com.intellij.psi.search.GlobalSearchScope] behaves differently in that layout, and
 * a single-module fixture silently passes.
 *
 * This is a heavy fixture: it creates a real project on disk, so it is noticeably slower than the
 * light ones. Prefer a light fixture and only reach for this class when the scenario genuinely
 * needs more than one module.
 *
 * Typical usage:
 * ```
 * class MyTest : ExplytMultiModuleTestCase() {
 *     override val libraries = arrayOf(TestLibrary.springContext_6_0_7)
 *
 *     fun testSomething() {
 *         val library = addDependencyModule("library")
 *         addFileToModule(library, "com/example/LibraryBean.kt", "...")
 *         // `module` (the main module) already depends on `library`
 *     }
 * }
 * ```
 */
abstract class ExplytMultiModuleTestCase : JavaCodeInsightFixtureTestCase() {

    /** Libraries attached to the main module and to every module created by [addDependencyModule]. */
    open val libraries: Array<TestLibrary> = arrayOf()

    override fun setUp() {
        super.setUp()
        attachLibraries(module)
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    /**
     * Creates a module named [name] with a single source root and makes the main [module] depend on
     * it, mirroring `implementation(project(":name"))`. The new module gets the same [libraries] and
     * inherits the project SDK, so Spring annotations resolve inside it.
     *
     * @return the created module, to be passed to [addFileToModule].
     */
    protected fun addDependencyModule(name: String): Module {
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("$name/src")
        val dependency = PsiTestUtil.addModule(project, JavaModuleType.getModuleType(), name, sourceRoot)

        ModuleRootModificationUtil.setSdkInherited(dependency)
        attachLibraries(dependency)
        // `exported = true` so the main module also sees the dependency's libraries, which is what
        // Gradle's `api`/`implementation` graph gives the application module at compile time.
        ModuleRootModificationUtil.addDependency(module, dependency, DependencyScope.COMPILE, true)

        IndexingTestUtil.waitUntilIndexesAreReady(project)
        return dependency
    }

    /**
     * Writes [text] to [relativePath] inside the source root of [module] and returns the created
     * [PsiFile]. [relativePath] is relative to that source root, e.g. `com/example/Bean.kt`.
     */
    protected fun addFileToModule(module: Module, relativePath: String, text: String): PsiFile {
        val sourceRoot = sourceRootOf(module)
        val file = createFile(sourceRoot, relativePath, text)

        IndexingTestUtil.waitUntilIndexesAreReady(project)
        return PsiManager.getInstance(project).findFile(file)
            ?: error("No PSI for '$relativePath' in module '${module.name}'")
    }

    private fun createFile(sourceRoot: VirtualFile, relativePath: String, text: String): VirtualFile =
        WriteAction.computeAndWait<VirtualFile, Throwable> {
            val directoryPath = relativePath.substringBeforeLast('/', "")
            val directory = if (directoryPath.isEmpty()) {
                sourceRoot
            } else {
                VfsUtil.createDirectoryIfMissing(sourceRoot, directoryPath)
            }
            val file = directory.createChildData(this, relativePath.substringAfterLast('/'))
            VfsUtil.saveText(file, text)
            file
        }

    private fun sourceRootOf(module: Module): VirtualFile =
        ModuleRootManager.getInstance(module).sourceRoots.firstOrNull()
            ?: error("Module '${module.name}' has no source root")

    private fun attachLibraries(module: Module) {
        if (libraries.isEmpty()) return

        ModuleRootModificationUtil.updateModel(module) { model ->
            libraries.forEach { addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies) }
        }
    }
}
