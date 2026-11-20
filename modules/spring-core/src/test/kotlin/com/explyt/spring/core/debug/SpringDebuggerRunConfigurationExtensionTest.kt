/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.debug

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.intellij.debugger.impl.GenericDebuggerRunnerSettings
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.execution.configurations.JavaParameters
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpringDebuggerRunConfigurationExtensionTest : ExplytJavaLightTestCase() {

    fun testUpdateJavaParametersWithoutReadAccess() {
        val configuration = ApplicationConfiguration("Debug", project, ApplicationConfigurationType.getInstance())
        configuration.setModule(module)
        assertEquals(listOf(module), configuration.modules.toList())

        val application = ApplicationManager.getApplication()
        val facade = JavaPsiFacade.getInstance(project)
        val scope = module.moduleWithLibrariesScope
        val parameters = JavaParameters()
        val runnerSettings = GenericDebuggerRunnerSettings()
        PsiManager.getInstance(project).dropPsiCaches()

        runBlockingReleasingWriteIntent {
            withContext(Dispatchers.Default) {
                assertFalse("The debugger prepares Java parameters off the EDT", application.isDispatchThread)
                assertFalse("The debugger enters without a read action", application.isReadAccessAllowed)
                val control = runCatching { facade.findClass("debugger.control.UncachedClass", scope) }.exceptionOrNull()
                assertTrue(
                    "Bare JavaPsiFacade.findClass must throw a read-access assertion, but got $control",
                    generateSequence(control) { it.cause }
                        .any { it.message.orEmpty().contains("Read access is allowed from inside read-action only") }
                )

                SpringDebuggerRunConfigurationExtension().updateJavaParameters(configuration, parameters, runnerSettings)
            }
        }
        assertEmpty(parameters.vmParametersList.list)
    }
}
