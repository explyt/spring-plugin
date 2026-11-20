/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.debug

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.debugger.impl.GenericDebuggerRunnerSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.JavaParameters
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.PlatformTestUtil
import org.junit.Assert.assertThrows
import java.util.Collections
import javax.swing.Icon

class SpringDebuggerExternalSystemEnvTest : ExplytJavaLightTestCase() {
    override val libraries = arrayOf(TestLibrary.springContext_6_0_7)

    fun testCleanupRemovesAgentFromUnmodifiableEnvironment() {
        val directory = FileUtil.createTempDirectory("debugger-external-project", null).canonicalFile
        VfsRootAccess.allowRootAccess(testRootDisposable, directory.path)
        val root = requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(directory))
        ModuleRootModificationUtil.addContentRoot(module, root)
        try {
            assertSame(module, ModuleUtilCore.findModuleForFile(root, project))
            val configuration = ExternalSystemRunConfiguration(ProjectSystemId("GRADLE"), project, StubFactory, "Debug")
            val originalEnvironment = mapOf("EXPLYT_KEEP" to "keep")
            val settings = configuration.settings
            settings.externalProjectPath = root.path
            settings.env = Collections.unmodifiableMap(originalEnvironment)

            SpringDebuggerRunConfigurationExtension().updateJavaParameters(
                configuration, JavaParameters(), GenericDebuggerRunnerSettings()
            )

            assertTrue("The debugger must add the agent before cleanup", settings.env[JAVA_TOOL_OPTIONS].orEmpty().startsWith("-javaagent:"))
            settings.env = Collections.unmodifiableMap(settings.env)
            assertThrows(UnsupportedOperationException::class.java) { settings.env.remove("EXPLYT_ABSENT") }

            waitForCleanup(configuration)
            assertEquals(originalEnvironment, settings.env)
        } finally {
            ModuleRootModificationUtil.updateModel(module) { model ->
                model.contentEntries.firstOrNull { it.file == root }?.let(model::removeContentEntry)
            }
            FileUtil.delete(directory)
        }
    }

    private fun waitForCleanup(configuration: ExternalSystemRunConfiguration) {
        var cleanupFailure: UnsupportedOperationException? = null
        val processor = LoggedErrorProcessor.executeWith(object : LoggedErrorProcessor() {
            override fun processError(category: String, message: String, details: Array<String>, t: Throwable?): Set<Action> {
                val failure = generateSequence(t) { it.cause }.filterIsInstance<UnsupportedOperationException>().firstOrNull()
                if (failure != null && failure.stackTrace.any { it.methodName.startsWith("scheduleCleanupExternalSystemEnvData") }) {
                    cleanupFailure = failure
                    return emptySet()
                }
                return super.processError(category, message, details, t)
            }
        })
        try {
            PlatformTestUtil.waitWithEventsDispatching(
                "The debugger did not clean up JAVA_TOOL_OPTIONS",
                { cleanupFailure != null || !configuration.settings.env.containsKey(JAVA_TOOL_OPTIONS) },
                20
            )
            cleanupFailure?.let { throw AssertionError("Unmodifiable debugger environment must be cleaned up without throwing", it) }
        } finally {
            processor.finish()
        }
    }

    private object StubType : ConfigurationType {
        override fun getDisplayName() = "Debugger external system test"
        override fun getConfigurationTypeDescription() = displayName
        override fun getIcon(): Icon? = null
        override fun getId() = "DebuggerExternalSystemTest"
        override fun getConfigurationFactories() = arrayOf(StubFactory)
    }

    private object StubFactory : ConfigurationFactory(StubType) {
        override fun getId() = "DebuggerExternalSystemTest"
        override fun createTemplateConfiguration(project: Project) =
            ExternalSystemRunConfiguration(ProjectSystemId("GRADLE"), project, this, "Debug")
    }

    private companion object {
        const val JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS"
    }
}
