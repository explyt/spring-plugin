/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.kotlin

import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.inspections.ControllerBeanNameAsPathInspection
import com.explyt.spring.web.util.WebApplicationStack
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.util.InheritanceUtil
import org.intellij.lang.annotations.Language

private const val HTTP_REQUEST_HANDLER = "org.springframework.web.HttpRequestHandler"
private const val MVC_CONTROLLER = "org.springframework.web.servlet.mvc.Controller"

abstract class ControllerBeanNameAsPathKotlinTestCase : ExplytInspectionKotlinTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(ControllerBeanNameAsPathInspection::class.java)
    }

    protected fun problemsIn(fileName: String, @Language("kotlin") code: String): List<HighlightInfo> {
        myFixture.configureByText(fileName, code)
        val toolId = ControllerBeanNameAsPathInspection().shortName
        return myFixture.doHighlighting().filter { it.inspectionToolId == toolId }
    }

    protected fun assertSingleWarningOn(problems: List<HighlightInfo>, literal: String): String {
        assertSize(1, problems)
        val problem = problems.single()
        assertEquals(HighlightSeverity.WARNING, problem.severity)
        assertEquals(literal, problem.text)
        return problem.description
    }

    protected fun assertStereotype(classFqn: String, stereotypeFqn: String) {
        assertNotNull(
            "$classFqn must carry Spring's $stereotypeFqn",
            myFixture.findClass(classFqn).getAnnotation(stereotypeFqn)
        )
    }

    protected fun applySingleQuickFix(fileName: String, @Language("kotlin") code: String): String {
        myFixture.configureByText(fileName, code)
        val fixes = myFixture.getAllQuickFixes()
        assertSize(1, fixes)
        myFixture.launchAction(fixes.single())
        return myFixture.editor.document.text
    }
}

class ControllerBeanNameAsPathInspectionTest : ControllerBeanNameAsPathKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testServletStackIsDetected() {
        assertEquals(WebApplicationStack.SERVLET, WebApplicationStack.of(module))
    }

    fun testRestControllerValueWithLeadingSlashWarnsThatExactRequestFails() {
        val problems = problemsIn(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.*

            @RestController("/app")
            class AppController {
                @GetMapping("/items")
                fun items(): String = ""
            }
            """.trimIndent()
        )
        assertStereotype("demo.AppController", SpringWebClasses.REST_CONTROLLER)

        val description = assertSingleWarningOn(problems, "\"/app\"")
        assertTrue(description, description.contains("/app"))
        assertTrue(description, description.contains("bean name", ignoreCase = true))
        assertTrue(description, description.contains("500"))
    }

    fun testNamedValueArgumentIsReported() {
        val problems = problemsIn(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.*

            @RestController(value = "/app")
            class AppController
            """.trimIndent()
        )
        assertStereotype("demo.AppController", SpringWebClasses.REST_CONTROLLER)

        val description = assertSingleWarningOn(problems, "\"/app\"")
        assertTrue(description, description.contains("500"))
    }

    fun testControllerValueWithoutLeadingSlashWarnsWithoutFailingRequestSentence() {
        val problems = problemsIn(
            "ApiController.kt", """
            package demo

            import org.springframework.stereotype.Controller

            @Controller(value = "api/v1")
            class ApiController
            """.trimIndent()
        )
        assertStereotype("demo.ApiController", SpringWebClasses.CONTROLLER)

        val description = assertSingleWarningOn(problems, "\"api/v1\"")
        assertTrue(description, description.contains("api/v1"))
        assertTrue(description, description.contains("bean name", ignoreCase = true))
        assertFalse(description, description.contains("500"))
    }

    fun testPlainBeanNameIsNotReported() {
        val problems = problemsIn(
            "SearchController.kt", """
            package demo

            import org.springframework.web.bind.annotation.*

            @RestController("searchController")
            class SearchController
            """.trimIndent()
        )
        assertStereotype("demo.SearchController", SpringWebClasses.REST_CONTROLLER)

        assertEmpty(problems)
    }

    fun testHttpRequestHandlerBeanNameUrlIsNotReported() {
        val problems = problemsIn(
            "LegacyHandler.kt", """
            package demo

            import org.springframework.stereotype.Controller
            import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler

            @Controller("/legacy")
            class LegacyHandler : DefaultServletHttpRequestHandler()
            """.trimIndent()
        )
        val handler = myFixture.findClass("demo.LegacyHandler")
        assertStereotype("demo.LegacyHandler", SpringWebClasses.CONTROLLER)
        assertTrue(InheritanceUtil.isInheritor(handler, HTTP_REQUEST_HANDLER))

        assertEmpty(problems)
    }

    fun testMvcControllerInterfaceBeanNameUrlIsNotReported() {
        val problems = problemsIn(
            "LegacyView.kt", """
            package demo

            import org.springframework.web.servlet.mvc.ParameterizableViewController

            @org.springframework.stereotype.Controller("/legacy")
            class LegacyView : ParameterizableViewController()
            """.trimIndent()
        )
        val view = myFixture.findClass("demo.LegacyView")
        assertStereotype("demo.LegacyView", SpringWebClasses.CONTROLLER)
        assertTrue(InheritanceUtil.isInheritor(view, MVC_CONTROLLER))

        assertEmpty(problems)
    }

    fun testQuickFixMovesValueToNewClassRequestMapping() {
        val result = applySingleQuickFix(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.*

            @RestController("/a<caret>pp")
            class AppController
            """.trimIndent()
        )

        assertTrue(result, result.contains("RequestMapping(\"/app\")"))
        assertFalse(result, result.contains("RestController("))
        assertTrue(result, result.contains("@RestController"))
        assertEquals(result, 1, "\"/app\"".toRegex(RegexOption.LITERAL).findAll(result).count())
    }

    fun testQuickFixKeepsExistingClassRequestMapping() {
        val result = applySingleQuickFix(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.*

            @RestController("/a<caret>pp")
            @RequestMapping("/x")
            class AppController
            """.trimIndent()
        )

        assertTrue(result, result.contains("@RequestMapping(\"/x\")"))
        assertEquals(result, 1, "RequestMapping".toRegex(RegexOption.LITERAL).findAll(result).count())
        assertFalse(result, result.contains("/app"))
        assertFalse(result, result.contains("RestController("))
        assertTrue(result, result.contains("@RestController"))
    }
}

class ControllerBeanNameAsPathReactiveInspectionTest : ControllerBeanNameAsPathKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springReactiveWeb_3_1_1)

    fun testReactiveStackWarnsWithBeanNameWordingOnly() {
        assertEquals(WebApplicationStack.REACTIVE, WebApplicationStack.of(module))

        val problems = problemsIn(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.*

            @RestController("/app")
            class AppController
            """.trimIndent()
        )
        assertStereotype("demo.AppController", SpringWebClasses.REST_CONTROLLER)

        val description = assertSingleWarningOn(problems, "\"/app\"")
        assertTrue(description, description.contains("/app"))
        assertTrue(description, description.contains("bean name", ignoreCase = true))
        assertFalse(description, description.contains("500"))
    }
}
