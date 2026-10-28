/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem

import com.explyt.spring.core.externalsystem.utils.NativeRunJdkValidator
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil

/**
 * A sync that reaches a module without a JDK must fail with a message the user can act on, before the platform
 * launcher reports it as an unhandled error and the sync waits for an application that never starts.
 */
class NativeRunJdkValidatorTest : ExplytKotlinLightTestCase() {

    override val realJdk = true

    fun testModuleWithJdkPasses() {
        NativeRunJdkValidator.requireJdkToRun(applicationConfiguration())
    }

    fun testModuleWithoutJdkFailsTheSyncWithActionableMessage() {
        withoutModuleJdk {
            val message = runCatching { NativeRunJdkValidator.requireJdkToRun(applicationConfiguration()) }
                .exceptionOrNull()
                .let { assertInstanceOf(it, ExternalSystemException::class.java) }
                .message.orEmpty()

            assertTrue("the message must name the module, but was: $message", message.contains(module.name))
            assertTrue("the message must say how to recover, but was: $message", message.contains("Project Structure"))
        }
    }

    fun testAlternativeJreDoesNotNeedModuleJdk() {
        withoutModuleJdk {
            val configuration = applicationConfiguration().apply {
                isAlternativeJrePathEnabled = true
                alternativeJrePath = System.getProperty("java.home")
            }

            NativeRunJdkValidator.requireJdkToRun(configuration)
        }
    }

    private fun applicationConfiguration() = ApplicationConfiguration("native-sync", project).apply { setModule(module) }

    private fun withoutModuleJdk(test: () -> Unit) {
        val moduleJdk = ModuleRootManager.getInstance(module).sdk
        assertNotNull("the fixture must start with a module JDK", moduleJdk)
        ModuleRootModificationUtil.setModuleSdk(module, null)
        try {
            assertNull("the fixture must remove the module JDK", ModuleRootManager.getInstance(module).sdk)
            test()
        } finally {
            ModuleRootModificationUtil.setModuleSdk(module, moduleJdk)
        }
    }
}
