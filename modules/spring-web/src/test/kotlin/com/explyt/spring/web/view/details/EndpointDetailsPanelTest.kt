/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.view.details.ContractRowLines.lines
import com.explyt.spring.web.view.details.ContractRowLines.rowOf
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.Disposer
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.PlatformTestUtil

class EndpointDetailsPanelTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
    )

    private lateinit var panel: EndpointDetailsPanel

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/DemoApplication.java", """
            package com.example;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class DemoApplication {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/PingController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            public class PingController {
                @GetMapping("/ping")
                public String ping(@RequestParam String name) { return name; }
            }
            """.trimIndent()
        )
        panel = EndpointDetailsPanel(project, testRootDisposable)
    }

    override fun tearDown() {
        try {
            Disposer.dispose(panel)
        } finally {
            super.tearDown()
        }
    }

    fun testSelectingAnEndpointShowsItsContractAfterTheBackgroundComputation() {
        assertEquals(SpringWebBundle.message("explyt.web.endpoints.details.empty"), panel.contractTree.emptyText.text)

        panel.show(rowOf(project, "/ping", "GET"))

        PlatformTestUtil.waitWithEventsDispatching(
            "The contract must be rendered",
            { panel.contractTree.rowCount > 0 },
            20
        )
        assertEquals(
            listOf("GET /ping", "Parameters", "  name  QUERY, java.lang.String, required", "Response  java.lang.String"),
            lines(panel.contractTree)
        )
    }

    fun testShowingARefreshedRowOfTheShownEndpointRecomputesItsContract() {
        panel.show(rowOf(project, "/ping", "GET"))
        PlatformTestUtil.waitWithEventsDispatching("The contract must be rendered", { panel.contractTree.rowCount > 0 }, 20)
        val controller = JavaPsiFacade.getInstance(project).findClass("com.example.PingController", GlobalSearchScope.projectScope(project))!!
        val documents = PsiDocumentManager.getInstance(project)
        WriteCommandAction.runWriteCommandAction(project) {
            val document = documents.getDocument(controller.containingFile)!!
            document.insertString(document.text.indexOf("@RequestParam String name"), "@RequestParam int page, ")
            documents.commitDocument(document)
        }

        panel.show(rowOf(project, "/ping", "GET"))

        PlatformTestUtil.waitWithEventsDispatching(
            "The refreshed contract must be rendered",
            { lines(panel.contractTree).any { it.startsWith("  page") } },
            20
        )
    }

    fun testClearingTheSelectionEmptiesThePane() {
        panel.show(rowOf(project, "/ping", "GET"))
        PlatformTestUtil.waitWithEventsDispatching("The contract must be rendered", { panel.contractTree.rowCount > 0 }, 20)

        panel.show(null)

        assertEquals(0, panel.contractTree.rowCount)
        assertEquals(SpringWebBundle.message("explyt.web.endpoints.details.empty"), panel.contractTree.emptyText.text)
    }
}
