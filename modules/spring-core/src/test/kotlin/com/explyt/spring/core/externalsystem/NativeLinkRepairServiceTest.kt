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
import com.intellij.testFramework.PlatformTestUtil

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

    /**
     * The scheduled pass must run entirely off the EDT. `RunManager` is requested by the very first step of the pass,
     * and requesting a project service that has not been created yet blocks the calling thread for the whole service
     * initialization — on the EDT that is the freeze reported in issue #294. `startRepair` asserts the thread, so a
     * regression back to `invokeLater` turns this test red instead of reaching users.
     *
     * The link points at a plain `main()` file rather than at the `@SpringBootApplication` one used elsewhere in this
     * class, because `SpringRunConfigurationDetectService` auto-creates a configuration for every Spring Boot entry
     * point it finds. Waiting for an asynchronous pass gives that detection time to run, and a second configuration
     * on the same main class would make the match ambiguous — which the repair pass deliberately leaves alone.
     */
    fun testScheduledRepairRunsOffEdt() {
        val mainFilePath = configurePlainMainFile()
        linkProject(mainFilePath, storedName = "Dashboard VPC")
        addSpringBootConfiguration("Dashboard Prod", mainClassName = "com.demo.PlainAppKt")

        NativeLinkRepairService.getInstance(project).scheduleRepair()

        PlatformTestUtil.waitWithEventsDispatching(
            "Dangling link must be repaired by the scheduled pass",
            { linkedName(mainFilePath) == "Dashboard Prod" },
            10
        )
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

    /**
     * A link stores `null` when it was created without a run configuration: that is a deliberate "unbound" state
     * meaning "discover by main-class path or by the selected configuration", which `RunConfigurationExtractor`
     * relies on. Binding it to the only matching configuration would silently disable that discovery.
     */
    fun testUnboundLinkIsNotBound() {
        val mainFilePath = configureDemoApplication()
        linkProject(mainFilePath, storedName = null)

        addSpringBootConfiguration("Dashboard Prod")

        NativeLinkRepairService.getInstance(project).repairNow()

        assertNull(linkedName(mainFilePath))
    }

    /**
     * Removing a configuration must keep the now-stale name: it is what marks the link as repairable. Clearing it
     * would both hide the link from the repair pass and downgrade it to an unbound link, making
     * `RunConfigurationExtractor` fabricate a default configuration instead of reporting the broken link.
     */
    fun testRemovedConfigurationKeepsStaleNameForRepair() {
        val mainFilePath = configureDemoApplication()
        linkProject(mainFilePath, storedName = "Dashboard VPC")

        project.messageBus.syncPublisher(RunManagerListener.TOPIC)
            .runConfigurationRemoved(detachedConfiguration())

        NativeLinkRepairService.getInstance(project).repairNow()

        assertEquals("Dashboard VPC", linkedName(mainFilePath))
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

    private fun linkProject(mainFilePath: String, storedName: String?) {
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

    /** A `main()` that `SpringRunConfigurationDetectService` does not recognize, so no configuration is added for it. */
    private fun configurePlainMainFile(): String = myFixture.configureByText(
        "PlainApp.kt",
        """
        package com.demo

        fun main(args: Array<String>) {
            println(args.size)
        }
        """.trimIndent()
    ).virtualFile.canonicalPath!!

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

    private fun addSpringBootConfiguration(
        name: String,
        mainClassName: String = "com.demo.DemoApplicationKt"
    ): RunnerAndConfigurationSettingsImpl {
        val runManager = runManager()
        val runConfiguration = SpringBootConfigurationFactory.createTemplateConfiguration(project)
        runConfiguration.name = name
        runConfiguration.mainClassName = mainClassName
        val rcSettings = RunnerAndConfigurationSettingsImpl(runManager, runConfiguration)
        runManager.addConfiguration(rcSettings)
        return rcSettings
    }
}
