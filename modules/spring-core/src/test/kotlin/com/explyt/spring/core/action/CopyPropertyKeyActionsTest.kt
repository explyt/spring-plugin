/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.action

import com.explyt.spring.test.ExplytBaseLightTestCase
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.testFramework.TestActionEvent
import java.awt.datatransfer.DataFlavor

class CopyPropertyKeyActionsTest : ExplytBaseLightTestCase() {

    fun testCopyPathFromProperties() {
        myFixture.configureByText("application.properties", "spring.datasource.ur<caret>l=jdbc:h2:mem:test")
        assertEquals("spring.datasource.url", performCopy(CopyPropertyPathAction()))
    }

    fun testCopyEnvFromProperties() {
        myFixture.configureByText("application.properties", "spring.main.log-startup-in<caret>fo=true")
        assertEquals("SPRING_MAIN_LOGSTARTUPINFO", performCopy(CopyPropertyAsEnvVariableAction()))
    }

    fun testCopyPathFromYaml() {
        myFixture.configureByText(
            "application.yaml",
            """
                spring:
                  datasource:
                    ur<caret>l: jdbc:h2:mem:test
            """.trimIndent()
        )
        assertEquals("spring.datasource.url", performCopy(CopyPropertyPathAction()))
    }

    fun testCopyEnvFromYaml() {
        myFixture.configureByText(
            "application.yaml",
            """
                my:
                  main-project:
                    person:
                      first-na<caret>me: John
            """.trimIndent()
        )
        assertEquals("MY_MAINPROJECT_PERSON_FIRSTNAME", performCopy(CopyPropertyAsEnvVariableAction()))
    }

    fun testActionDisabledInNonPropertyFile() {
        myFixture.configureByText("Sample.java", "class Sam<caret>ple {}")
        val presentation = updatePresentation(CopyPropertyPathAction())
        assertFalse(presentation.isEnabledAndVisible)
    }

    private fun performCopy(action: AnAction): String? {
        val event = TestActionEvent.createTestEvent(action, dataContext())
        action.update(event)
        assertTrue("Action is expected to be enabled and visible", event.presentation.isEnabledAndVisible)
        action.actionPerformed(event)
        return CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor)
    }

    private fun updatePresentation(action: AnAction) =
        TestActionEvent.createTestEvent(action, dataContext()).also { action.update(it) }.presentation

    private fun dataContext() = SimpleDataContext.builder()
        .add(CommonDataKeys.PROJECT, project)
        .add(CommonDataKeys.EDITOR, myFixture.editor)
        .add(CommonDataKeys.PSI_FILE, myFixture.file)
        .add(CommonDataKeys.VIRTUAL_FILE, myFixture.file.virtualFile)
        .build()
}
