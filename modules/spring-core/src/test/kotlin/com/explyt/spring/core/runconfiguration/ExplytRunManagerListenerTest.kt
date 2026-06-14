/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.setting.NativeSettings
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.execution.RunManager
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil

/**
 * Tests for [ExplytRunManagerListener] — focuses on the link-rename synchronization:
 * when a Spring Boot run configuration is renamed, the stored
 * [NativeProjectSettings.runConfigurationName] for the project linked by the same
 * main-class file should follow the new name.
 */
class ExplytRunManagerListenerTest : ExplytKotlinLightTestCase() {
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

    fun testRenamingSpringBootRunConfigurationUpdatesLinkedProjectName() {
        val mainFile = myFixture.configureByText(
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
        ).virtualFile
        val mainFilePath = mainFile.canonicalPath!!

        // Link Explyt Spring project under the original (stale) configuration name.
        val originalName = "DemoApplicationKt"
        val nativeSettings = project.getService(NativeSettings::class.java)
        val linkedSettings = NativeProjectSettings().apply {
            externalProjectPath = mainFilePath
            runConfigurationName = originalName
        }
        nativeSettings.linkProject(linkedSettings)

        // Create a SpringBootRunConfiguration tied to the same main file, with a different name.
        val runManager = RunManager.getInstance(project) as RunManagerImpl
        val runConfiguration = SpringBootConfigurationFactory.createTemplateConfiguration(project)
        runConfiguration.name = "Renamed Demo"
        runConfiguration.mainClassName = "com.demo.DemoApplicationKt"
        val rcSettings = RunnerAndConfigurationSettingsImpl(runManager, runConfiguration)
        runManager.addConfiguration(rcSettings)

        // Sanity check: link is still pointing at the stale name.
        assertEquals(originalName, nativeSettings.getLinkedProjectSettings(mainFilePath)?.runConfigurationName)

        // Fire a configuration-changed event — this is exactly what RunManager does on rename.
        runManager.fireRunConfigurationChanged(rcSettings)

        // The listener should have updated the stored name.
        assertEquals(
            "Renamed Demo",
            nativeSettings.getLinkedProjectSettings(mainFilePath)?.runConfigurationName
        )
    }

    fun testNoLinkedProjectIsLeftUntouched() {
        val mainFile = myFixture.configureByText(
            "Other.kt",
            """
            package com.demo

            import org.springframework.boot.autoconfigure.SpringBootApplication
            import org.springframework.boot.runApplication

            @SpringBootApplication
            class Other

            fun main(args: Array<String>) {
                runApplication<Other>(*args)
            }
            """.trimIndent()
        ).virtualFile

        val runManager = RunManager.getInstance(project) as RunManagerImpl
        val runConfiguration = SpringBootConfigurationFactory.createTemplateConfiguration(project)
        runConfiguration.name = "Unlinked"
        runConfiguration.mainClassName = "com.demo.OtherKt"
        val rcSettings = RunnerAndConfigurationSettingsImpl(runManager, runConfiguration)
        runManager.addConfiguration(rcSettings)

        // No linked project for this main file — listener should be a no-op (no NPE etc.).
        val nativeSettings = project.getService(NativeSettings::class.java)
        assertNull(nativeSettings.getLinkedProjectSettings(mainFile.canonicalPath!!))

        runManager.fireRunConfigurationChanged(rcSettings)

        assertNull(nativeSettings.getLinkedProjectSettings(mainFile.canonicalPath!!))
    }
}
