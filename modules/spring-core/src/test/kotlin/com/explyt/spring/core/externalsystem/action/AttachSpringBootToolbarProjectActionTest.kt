/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.runconfiguration.SpringBootConfigurationFactory
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.DumbModeTestUtils
import com.intellij.testFramework.TestActionEvent

class AttachSpringBootToolbarProjectActionTest : ExplytKotlinLightTestCase() {

    fun testUpdateEnablesJavaApplicationConfigurationWithoutResolvingPsi() {
        val configuration = ApplicationConfiguration(
            "Java application",
            project,
            ApplicationConfigurationType.getInstance()
        ).apply {
            mainClassName = "missing.Application"
        }
        assertUpdateVisible(configuration)
    }

    fun testUpdateEnablesSpringBootConfigurationWithoutResolvingPsi() {
        val configuration = SpringBootConfigurationFactory.createTemplateConfiguration(project).apply {
            mainClassName = "missing.Application"
        }
        assertUpdateVisible(configuration)
    }

    fun testUpdateHidesConfigurationWithoutMainClass() {
        val configuration = SpringBootConfigurationFactory.createTemplateConfiguration(project)
        assertFalse(updatePresentation(configuration).isVisible)
    }

    /**
     * Attaching resolves the main class through indexes, so during indexing the button must stay in place and only
     * become inert: a main-toolbar button that disappears on every indexing pass shifts the whole toolbar.
     */
    fun testUpdateKeepsButtonVisibleButDisabledInDumbMode() {
        val configuration = SpringBootConfigurationFactory.createTemplateConfiguration(project).apply {
            mainClassName = "missing.Application"
        }
        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            val presentation = updatePresentation(configuration)
            assertTrue(presentation.isVisible)
            assertFalse(presentation.isEnabled)
        }
    }

    private fun assertUpdateVisible(configuration: RunConfiguration) {
        assertTrue(updatePresentation(configuration).isEnabledAndVisible)
    }

    private fun updatePresentation(configuration: RunConfiguration) = run {
        val manager = RunManager.getInstance(project) as RunManagerImpl
        val settings = RunnerAndConfigurationSettingsImpl(manager, configuration)
        manager.addConfiguration(settings)
        manager.selectedConfiguration = settings
        try {
            TestActionEvent.createTestEvent(
                AttachSpringBootToolbarProjectAction(),
                SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
            ).also { AttachSpringBootToolbarProjectAction().update(it) }.presentation
        } finally {
            manager.selectedConfiguration = null
            manager.removeConfiguration(settings)
        }
    }
}
