/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
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

abstract class ControllerBeanNameAsPathJavaTestCase : ExplytInspectionJavaTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(ControllerBeanNameAsPathInspection::class.java)
    }

    protected fun problemsIn(fileName: String, @Language("JAVA") code: String): List<HighlightInfo> {
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

    protected fun applySingleQuickFix(fileName: String, @Language("JAVA") code: String): String {
        myFixture.configureByText(fileName, code)
        val fixes = myFixture.getAllQuickFixes()
        assertSize(1, fixes)
        myFixture.launchAction(fixes.single())
        return myFixture.editor.document.text
    }
}

class ControllerBeanNameAsPathInspectionTest : ControllerBeanNameAsPathJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testServletStackIsDetected() {
        assertEquals(WebApplicationStack.SERVLET, WebApplicationStack.of(module))
    }

    fun testRestControllerValueWithLeadingSlashWarnsThatExactRequestFails() {
        val problems = problemsIn(
            "AppController.java", """
            package demo;

            import org.springframework.web.bind.annotation.*;

            @RestController("/app")
            public class AppController {
                @GetMapping("/items")
                public String items() { return ""; }
            }
            """.trimIndent()
        )
        assertStereotype("demo.AppController", SpringWebClasses.REST_CONTROLLER)

        val description = assertSingleWarningOn(problems, "\"/app\"")
        assertTrue(description, description.contains("/app"))
        assertTrue(description, description.contains("bean name", ignoreCase = true))
        assertTrue(description, description.contains("500"))
    }

    fun testControllerValueWithoutLeadingSlashWarnsWithoutFailingRequestSentence() {
        val problems = problemsIn(
            "ApiController.java", """
            package demo;

            import org.springframework.stereotype.Controller;

            @Controller(value = "api/v1")
            public class ApiController {
            }
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
            "SearchController.java", """
            package demo;

            import org.springframework.web.bind.annotation.*;

            @RestController("searchController")
            public class SearchController {
            }
            """.trimIndent()
        )
        assertStereotype("demo.SearchController", SpringWebClasses.REST_CONTROLLER)

        assertEmpty(problems)
    }

    fun testHttpRequestHandlerBeanNameUrlIsNotReported() {
        val problems = problemsIn(
            "LegacyHandler.java", """
            package demo;

            import org.springframework.stereotype.Controller;
            import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

            @Controller("/legacy")
            public class LegacyHandler extends DefaultServletHttpRequestHandler {
            }
            """.trimIndent()
        )
        val handler = myFixture.findClass("demo.LegacyHandler")
        assertStereotype("demo.LegacyHandler", SpringWebClasses.CONTROLLER)
        assertTrue(InheritanceUtil.isInheritor(handler, HTTP_REQUEST_HANDLER))

        assertEmpty(problems)
    }

    fun testMvcControllerInterfaceBeanNameUrlIsNotReported() {
        val problems = problemsIn(
            "LegacyView.java", """
            package demo;

            import org.springframework.web.servlet.mvc.ParameterizableViewController;

            @org.springframework.stereotype.Controller("/legacy")
            public class LegacyView extends ParameterizableViewController {
            }
            """.trimIndent()
        )
        val view = myFixture.findClass("demo.LegacyView")
        assertStereotype("demo.LegacyView", SpringWebClasses.CONTROLLER)
        assertTrue(InheritanceUtil.isInheritor(view, MVC_CONTROLLER))

        assertEmpty(problems)
    }

    fun testQuickFixMovesValueToNewClassRequestMapping() {
        val result = applySingleQuickFix(
            "AppController.java", """
            package demo;

            import org.springframework.web.bind.annotation.*;

            @RestController("/a<caret>pp")
            public class AppController {
            }
            """.trimIndent()
        )

        assertTrue(result, result.contains("RequestMapping(\"/app\")"))
        assertFalse(result, result.contains("RestController("))
        assertTrue(result, result.contains("@RestController"))
        assertEquals(result, 1, "\"/app\"".toRegex(RegexOption.LITERAL).findAll(result).count())
    }

    fun testQuickFixKeepsExistingClassRequestMapping() {
        val result = applySingleQuickFix(
            "AppController.java", """
            package demo;

            import org.springframework.web.bind.annotation.*;

            @RestController("/a<caret>pp")
            @RequestMapping("/x")
            public class AppController {
            }
            """.trimIndent()
        )

        assertTrue(result, result.contains("@RequestMapping(\"/x\")"))
        assertEquals(result, 1, "RequestMapping".toRegex(RegexOption.LITERAL).findAll(result).count())
        assertFalse(result, result.contains("/app"))
        assertFalse(result, result.contains("RestController("))
        assertTrue(result, result.contains("@RestController"))
    }
}

class ControllerBeanNameAsPathReactiveInspectionTest : ControllerBeanNameAsPathJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springReactiveWeb_3_1_1)

    fun testReactiveStackWarnsWithBeanNameWordingOnly() {
        assertEquals(WebApplicationStack.REACTIVE, WebApplicationStack.of(module))

        val problems = problemsIn(
            "AppController.java", """
            package demo;

            import org.springframework.web.bind.annotation.*;

            @RestController("/app")
            public class AppController {
            }
            """.trimIndent()
        )
        assertStereotype("demo.AppController", SpringWebClasses.REST_CONTROLLER)

        val description = assertSingleWarningOn(problems, "\"/app\"")
        assertTrue(description, description.contains("/app"))
        assertTrue(description, description.contains("bean name", ignoreCase = true))
        assertFalse(description, description.contains("500"))
    }
}
