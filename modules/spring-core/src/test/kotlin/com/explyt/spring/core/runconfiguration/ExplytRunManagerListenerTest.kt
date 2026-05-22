/*
 * Copyright © 2026 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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
