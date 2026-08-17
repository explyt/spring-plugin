/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem

import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.setting.NativeSettings
import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.runconfiguration.SpringBootConfigurationFactory
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil

/**
 * Tests for [NativeLinkRepairService] — the self-healing path for links whose stored run configuration name no
 * longer exists, typically because the configuration was renamed on another machine and pulled from version control.
 */
class NativeLinkRepairServiceTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    override fun tearDown() {
        try {
            val nativeSettings = project.getService(NativeSettings::class.java)
            nativeSettings.linkedProjectsSettings.toList().forEach {
                ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).unlinkExternalProject(it.externalProjectPath)
            }
        } finally {
            super.tearDown()
        }
    }

    /**
     * A rename delivered through version control arrives as a removal plus an addition, because the shared `.run`
     * scheme key is the configuration name — `runConfigurationChanged` is never published for it.
     */
    fun testVcsDeliveredRenameHealsLink() {
        val mainFilePath = configureDemoApplication()
        linkProject(mainFilePath, storedName = "Dashboard VPC")

        // A pulled rename is delivered as a removal plus an addition; RunManagerImpl publishes both on this topic.
        val publisher = project.messageBus.syncPublisher(RunManagerListener.TOPIC)
        publisher.runConfigurationRemoved(detachedConfiguration())
        publisher.runConfigurationAdded(addSpringBootConfiguration("Dashboard Prod"))

        NativeLinkRepairService.getInstance(project).repairNow()

        assertEquals("Dashboard Prod", linkedName(mainFilePath))
    }

    /** Configurations sharing a main class differ in profiles and environment, so guessing one would be wrong. */
    fun testAmbiguousMatchIsLeftAlone() {
        val mainFilePath = configureDemoApplication()
        linkProject(mainFilePath, storedName = "Dashboard VPC")

        addSpringBootConfiguration("Dashboard Prod")
        addSpringBootConfiguration("Dashboard Staging")

        NativeLinkRepairService.getInstance(project).repairNow()

        assertEquals("Dashboard VPC", linkedName(mainFilePath))
    }

    /** A stored name that still resolves is not dangling and must never be rewritten. */
    fun testLiveStoredNameIsNotTouched() {
        val mainFilePath = configureDemoApplication()
        linkProject(mainFilePath, storedName = "Dashboard Staging")

        addSpringBootConfiguration("Dashboard Staging")
        addSpringBootConfiguration("Dashboard Prod")

        NativeLinkRepairService.getInstance(project).repairNow()

        assertEquals("Dashboard Staging", linkedName(mainFilePath))
    }

    /** The debug-session link is keyed by a transient session, not by a run configuration name. */
    fun testDebugSessionLinkIsNotTouched() {
        configureDemoApplication()
        val nativeSettings = project.getService(NativeSettings::class.java)
        nativeSettings.linkProject(NativeProjectSettings().apply {
            externalProjectPath = Constants.DEBUG_SESSION_NAME
            runConfigurationId = "Dashboard Staging"
        })

        addSpringBootConfiguration("Dashboard Prod")

        NativeLinkRepairService.getInstance(project).repairNow()

        val debugSettings = nativeSettings.getLinkedProjectSettings(Constants.DEBUG_SESSION_NAME)
        assertNull(debugSettings?.runConfigurationName)
        assertEquals("Dashboard Staging", debugSettings?.runConfigurationId)
    }

    private fun runManager() = RunManager.getInstance(project) as RunManagerImpl

    private fun linkedName(mainFilePath: String): String? =
        project.getService(NativeSettings::class.java).getLinkedProjectSettings(mainFilePath)?.runConfigurationName

    private fun linkProject(mainFilePath: String, storedName: String) {
        project.getService(NativeSettings::class.java).linkProject(NativeProjectSettings().apply {
            externalProjectPath = mainFilePath
            qualifiedMainClassName = "com.demo.DemoApplication"
            runConfigurationName = storedName
        })
    }

    /** The removed configuration no longer exists in RunManager, so the event carries a detached settings object. */
    private fun detachedConfiguration(): RunnerAndConfigurationSettingsImpl {
        val runConfiguration = SpringBootConfigurationFactory.createTemplateConfiguration(project)
        runConfiguration.name = "Dashboard VPC"
        runConfiguration.mainClassName = "com.demo.DemoApplicationKt"
        return RunnerAndConfigurationSettingsImpl(runManager(), runConfiguration)
    }

    private fun configureDemoApplication(): String = myFixture.configureByText(
        "DemoApplication.kt",
        """
        package com.demo

        import org.springframework.boot.autoconfigure.SpringBootApplication
        import org.springframework.boot.runApplication

        @SpringBootApplication
        class DemoApplication

        fun main(args: Array<String>) {
            runApplication<DemoApplication>(*args)
        }
        """.trimIndent()
    ).virtualFile.canonicalPath!!

    private fun addSpringBootConfiguration(name: String): RunnerAndConfigurationSettingsImpl {
        val runManager = runManager()
        val runConfiguration = SpringBootConfigurationFactory.createTemplateConfiguration(project)
        runConfiguration.name = name
        runConfiguration.mainClassName = "com.demo.DemoApplicationKt"
        val rcSettings = RunnerAndConfigurationSettingsImpl(runManager, runConfiguration)
        runManager.addConfiguration(rcSettings)
        return rcSettings
    }
}
