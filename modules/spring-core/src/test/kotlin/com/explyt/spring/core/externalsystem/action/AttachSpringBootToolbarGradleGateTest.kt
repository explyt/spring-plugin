/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.testFramework.TestActionEvent
import javax.swing.Icon

/**
 * A Gradle configuration carries no main class to peek at, so the toolbar enables it by a task being present. A task
 * alone is true of `clean` in any Gradle project; the project must also depend on Spring Boot, read from the library
 * model rather than from PSI so the update stays cheap.
 */
class AttachSpringBootToolbarGradleGateTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBoot_3_1_1)

    fun testGradleTaskIsOfferedInASpringBootProject() {
        assertTrue(updatePresentation(project, gradleConfiguration(project, "bootRun")).isEnabledAndVisible)
    }
}

class AttachSpringBootToolbarGradleGateWithoutBootTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testGradleTaskIsNotOfferedWithoutSpringBoot() {
        assertFalse(updatePresentation(project, gradleConfiguration(project, "clean")).isVisible)
    }
}

private fun updatePresentation(project: Project, configuration: ExternalSystemRunConfiguration): Presentation {
    val manager = RunManager.getInstance(project) as RunManagerImpl
    val settings = RunnerAndConfigurationSettingsImpl(manager, configuration)
    manager.addConfiguration(settings)
    manager.selectedConfiguration = settings
    try {
        val action = AttachSpringBootToolbarProjectAction()
        val event = TestActionEvent.createTestEvent(
            action,
            SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
        )
        action.update(event)
        return event.presentation
    } finally {
        manager.selectedConfiguration = null
        manager.removeConfiguration(settings)
    }
}

private fun gradleConfiguration(project: Project, task: String): ExternalSystemRunConfiguration =
    ExternalSystemRunConfiguration(ProjectSystemId("GRADLE"), project, GradleStubFactory, task).apply {
        settings.taskNames = mutableListOf(task)
    }

private object GradleStubType : ConfigurationType {
    override fun getDisplayName() = "Stub"
    override fun getConfigurationTypeDescription() = "Stub"
    override fun getIcon(): Icon? = null
    override fun getId() = "StubGradleToolbar"
    override fun getConfigurationFactories() = arrayOf(GradleStubFactory)
}

private object GradleStubFactory : ConfigurationFactory(GradleStubType) {
    override fun getId() = "StubGradleToolbar"
    override fun createTemplateConfiguration(project: Project) =
        ExternalSystemRunConfiguration(ProjectSystemId("GRADLE"), project, this, "Stub")
}
