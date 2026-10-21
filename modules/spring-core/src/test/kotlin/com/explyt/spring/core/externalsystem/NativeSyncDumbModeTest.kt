/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.util.Computable
import com.intellij.testFramework.DumbModeTestUtils

/**
 * A native sync builds the project while it resolves, and the build output triggers a VFS refresh that puts every
 * open project into dumb mode. The resolver keeps reading PSI afterwards, so its index queries have to survive that
 * window: the bean context has already been collected by then, and losing it discards a completed application run.
 *
 * Waiting for smart mode is not available here — the platform suspends the dumb queue for the whole resolution — so
 * these tests pin the behaviour of the read action the resolver actually uses.
 */
class NativeSyncDumbModeTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    /** Guards the premise of [testLibraryClassLookupSurvivesDumbMode]: an always-absent class proves nothing. */
    fun testLibraryClassIsResolvableInSmartMode() {
        assertNotNull(
            "the fixture must provide ${SpringCoreClasses.COMPONENT} in smart mode",
            withIndexAccessDuringSync {
                LibraryClassCache.searchForLibraryClass(project, SpringCoreClasses.COMPONENT)
            }
        )
    }

    fun testLibraryClassLookupSurvivesDumbMode() {
        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            assertNotNull(
                "a library class the sync needs must stay resolvable while indexing runs",
                withIndexAccessDuringSync {
                    LibraryClassCache.searchForLibraryClass(project, SpringCoreClasses.COMPONENT)
                }
            )
        }
    }

    /**
     * The defect this suite exists for: the same lookup under the resolver's previous plain read action dies on the
     * first stub-index query, which is what aborted the sync after the beans had been read.
     */
    fun testPlainReadActionFailsOnTheSameLookupInDumbMode() {
        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            assertThrows(IndexNotReadyException::class.java) {
                ApplicationManager.getApplication().runReadAction(Computable {
                    LibraryClassCache.searchForLibraryClass(project, SpringCoreClasses.COMPONENT)
                })
            }
        }
    }

    /** An absent class must still be reported as absent, not masked into a resolvable one. */
    fun testMissingClassStaysUnresolvedInDumbMode() {
        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            assertNull(
                withIndexAccessDuringSync {
                    LibraryClassCache.searchForLibraryClass(project, "missing.Application")
                }
            )
        }
    }
}
