/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.process

import com.explyt.spring.core.runconfiguration.SpringBootConfigurationFactory
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class UnlinkedProjectAwareTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    fun testBuildFileIsRecognisedOutsideReadAction() {
        val mainFile = mainApplicationFile()
        val settings = addSpringBootRunConfiguration("com.demo.DemoApplication")
        try {
            assertTrue(
                "Precondition: inside a read action the run configuration's main class file is a build file",
                ApplicationManager.getApplication()
                    .runReadAction(Computable { UnlinkedProjectAware().isBuildFile(project, mainFile) })
            )
            PsiManager.getInstance(project).dropPsiCaches()
            val outsideReadAction = ApplicationManager.getApplication().executeOnPooledThread(Callable {
                val readAccessAllowed = ApplicationManager.getApplication().isReadAccessAllowed
                readAccessAllowed to UnlinkedProjectAware().isBuildFile(project, mainFile)
            })
            val (readAccessAllowed, isBuildFile) = outsideReadAction.get(30, TimeUnit.SECONDS)
            assertFalse("Precondition: the call runs outside a read action", readAccessAllowed)
            assertTrue(isBuildFile)
        } finally {
            RunManager.getInstance(project).removeConfiguration(settings)
        }
    }

    private fun mainApplicationFile(): VirtualFile = myFixture.addClass(
        """
        package com.demo;

        public class DemoApplication {
            public static void main(String[] args) {}
        }
        """.trimIndent()
    ).containingFile.virtualFile

    private fun addSpringBootRunConfiguration(mainClassName: String): RunnerAndConfigurationSettings {
        val runManager = RunManager.getInstance(project) as RunManagerImpl
        val configuration = SpringBootConfigurationFactory.createTemplateConfiguration(project)
        configuration.name = "Demo"
        configuration.mainClassName = mainClassName
        return RunnerAndConfigurationSettingsImpl(runManager, configuration).also(runManager::addConfiguration)
    }
}
