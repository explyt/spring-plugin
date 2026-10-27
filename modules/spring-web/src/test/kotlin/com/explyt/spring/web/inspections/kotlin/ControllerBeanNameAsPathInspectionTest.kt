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
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.toUElementOfType
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
        assertEquals(
            "$classFqn must carry Spring's $stereotypeFqn",
            stereotypeFqn,
            myFixture.findClass(classFqn).getAnnotation(stereotypeFqn)?.resolveAnnotationType()?.qualifiedName
        )
    }

    protected fun applySingleQuickFix(fileName: String, @Language("kotlin") code: String): String {
        myFixture.configureByText(fileName, code)
        assertStereotype(
            "demo.${fileName.substringBeforeLast('.')}",
            if (code.contains("@RestController")) SpringWebClasses.REST_CONTROLLER else SpringWebClasses.CONTROLLER
        )
        val fixes = myFixture.getAllQuickFixes()
        assertSize(1, fixes)
        if (!code.contains("@RequestMapping")) {
            assertEquals("Move '/app' to @RequestMapping", fixes.single().text)
        }
        myFixture.launchAction(fixes.single())
        return myFixture.editor.document.text
    }
}

class ControllerBeanNameAsPathInspectionTest : ControllerBeanNameAsPathKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testServletStackIsDetected() {
        assertEquals(WebApplicationStack.SERVLET, WebApplicationStack.of(module))
    }

    fun testInheritedSuperclassRequestMappingOffersOnlyRemoveFix() {
        val problems = problemsIn(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.*

            @RequestMapping("/api")
            abstract class BaseController

            @RestController("/app")
            class AppController : BaseController()
            """.trimIndent()
        )
        assertStereotype("demo.BaseController", SpringWebClasses.REQUEST_MAPPING)
        assertTrue(InheritanceUtil.isInheritor(myFixture.findClass("demo.AppController"), "demo.BaseController"))
        assertOnlyRemoveFixPreservesMapping(problems, "demo.BaseController")
    }

    fun testImplementedInterfaceRequestMappingOffersOnlyRemoveFix() {
        val problems = problemsIn(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.*

            @RequestMapping("/api")
            interface Api

            @RestController("/app")
            class AppController : Api
            """.trimIndent()
        )
        assertStereotype("demo.Api", SpringWebClasses.REQUEST_MAPPING)
        assertTrue(myFixture.findClass("demo.Api").isInterface)
        assertTrue(InheritanceUtil.isInheritor(myFixture.findClass("demo.AppController"), "demo.Api"))
        assertOnlyRemoveFixPreservesMapping(problems, "demo.Api")
    }

    fun testComposedRequestMappingOffersOnlyRemoveFix() {
        myFixture.addFileToProject(
            "demo/ApiMapping.kt", """
            package demo

            import org.springframework.web.bind.annotation.*

            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.RUNTIME)
            @RequestMapping("/api")
            annotation class ApiMapping
            """.trimIndent()
        )
        val problems = problemsIn(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.*

            @demo.ApiMapping
            @RestController("/app")
            class AppController
            """.trimIndent()
        )
        assertStereotype("demo.ApiMapping", SpringWebClasses.REQUEST_MAPPING)
        assertEquals("demo.ApiMapping", myFixture.findClass("demo.AppController").toUElementOfType<UClass>()!!
            .uAnnotations.single { it.sourcePsi?.text?.contains("ApiMapping") == true }.qualifiedName)
        assertOnlyRemoveFixPreservesMapping(problems, "demo.ApiMapping")
        assertEquals("demo.ApiMapping", myFixture.findClass("demo.AppController").toUElementOfType<UClass>()!!
            .uAnnotations.single { it.sourcePsi?.text?.contains("ApiMapping") == true }.qualifiedName)
    }

    private fun assertOnlyRemoveFixPreservesMapping(problems: List<HighlightInfo>, mappingOwner: String) {
        assertEquals(WebApplicationStack.SERVLET, WebApplicationStack.of(module))
        assertStereotype("demo.AppController", SpringWebClasses.REST_CONTROLLER)
        assertSingleWarningOn(problems, "\"/app\"")
        assertNull(myFixture.findClass("demo.AppController").getAnnotation(SpringWebClasses.REQUEST_MAPPING))
        assertEquals(
            "\"/api\"", myFixture.findClass(mappingOwner)
                .getAnnotation(SpringWebClasses.REQUEST_MAPPING)?.findDeclaredAttributeValue("value")?.text?.trim('{', '}')
        )
        val fixes = myFixture.getAllQuickFixes()
        assertSize(1, fixes)
        val fix = fixes.single()
        assertEquals("Remove bean name '/app'", fix.text)
        myFixture.launchAction(fix)
        val controller = myFixture.findClass("demo.AppController")
        assertNull(controller.getAnnotation(SpringWebClasses.REQUEST_MAPPING))
        assertStereotype("demo.AppController", SpringWebClasses.REST_CONTROLLER)
        assertNull(controller.getAnnotation(SpringWebClasses.REST_CONTROLLER)?.findDeclaredAttributeValue("value"))
        assertFalse(myFixture.editor.document.text.contains("RestController("))
        assertEquals(
            "\"/api\"", myFixture.findClass(mappingOwner)
                .getAnnotation(SpringWebClasses.REQUEST_MAPPING)?.findDeclaredAttributeValue("value")?.text?.trim('{', '}')
        )
    }

    fun testNamedValueArgumentQuickFixMovesOnlyTheValue() {
        val problems = problemsIn(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.*

            @RestController(value = "/app")
            class AppController
            """.trimIndent()
        )
        assertEquals(WebApplicationStack.SERVLET, WebApplicationStack.of(module))
        assertStereotype("demo.AppController", SpringWebClasses.REST_CONTROLLER)
        assertSingleWarningOn(problems, "\"/app\"")
        assertNull(myFixture.findClass("demo.AppController").getAnnotation(SpringWebClasses.REQUEST_MAPPING))
        val fixes = myFixture.getAllQuickFixes()
        assertSize(1, fixes)
        assertEquals("Move '/app' to @RequestMapping", fixes.single().text)
        myFixture.launchAction(fixes.single())
        val result = myFixture.editor.document.text
        assertTrue(result, result.contains("@RequestMapping(\"/app\")"))
        assertFalse(result, result.contains("RestController("))
        assertStereotype("demo.AppController", SpringWebClasses.REST_CONTROLLER)
        assertNull(
            myFixture.findClass("demo.AppController").getAnnotation(SpringWebClasses.REST_CONTROLLER)
                ?.findDeclaredAttributeValue("value")
        )
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

    fun testConstantBeanNameIsReportedAndQuickFixKeepsExpression() {
        val problems = problemsIn(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.RestController

            object Paths {
                const val APP = "/app"
            }

            @RestController(Paths.APP)
            class AppController
            """.trimIndent()
        )
        assertStereotype("demo.AppController", SpringWebClasses.REST_CONTROLLER)
        assertSingleWarningOn(problems, "Paths.APP")
        val annotation = myFixture.findClass("demo.AppController").toUElementOfType<UClass>()!!
            .uAnnotations.single { it.qualifiedName == SpringWebClasses.REST_CONTROLLER }
        val expression = annotation.findDeclaredAttributeValue("value") as UQualifiedReferenceExpression
        val constant = (expression.selector as UReferenceExpression).resolve() as PsiField
        assertEquals("APP", constant.name)
        assertTrue(constant.hasModifierProperty(PsiModifier.STATIC))
        assertTrue(constant.hasModifierProperty(PsiModifier.FINAL))
        assertEquals("/app", constant.computeConstantValue())
        assertTrue((constant.navigationElement as KtProperty).hasModifier(KtTokens.CONST_KEYWORD))

        val result = applySingleQuickFix(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.RestController

            object Paths {
                const val APP = "/app"
            }

            @RestController(Paths.APP)
            class AppController
            """.trimIndent()
        )
        assertTrue(result, result.contains("@RequestMapping(Paths.APP)"))
    }

    fun testEmptyBeanNameIsNotReported() {
        val problems = problemsIn(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.RestController

            @RestController("")
            class AppController
            """.trimIndent()
        )
        assertStereotype("demo.AppController", SpringWebClasses.REST_CONTROLLER)
        assertEmpty(problems)
    }

    fun testControllerBeanNameIsReportedAndQuickFixKeepsController() {
        val code = """
            package demo

            import org.springframework.stereotype.Controller

            @Controller("/app")
            class ApiController
            """.trimIndent()
        val problems = problemsIn("ApiController.kt", code)
        assertEquals(WebApplicationStack.SERVLET, WebApplicationStack.of(module))
        assertStereotype("demo.ApiController", SpringWebClasses.CONTROLLER)
        val description = assertSingleWarningOn(problems, "\"/app\"")
        assertTrue(description, description.contains("500"))

        val result = applySingleQuickFix("ApiController.kt", code)
        assertTrue(result, result.contains("@RequestMapping(\"/app\")"))
        assertTrue(result, result.contains("@Controller"))
        assertFalse(result, result.contains("Controller(\"/app\")"))
    }

    fun testComposedControllerAnnotationIsNotReported() {
        val problems = problemsIn(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.RestController

            @RestController
            annotation class MyController(val value: String = "")

            @MyController("/app")
            class AppController
            """.trimIndent()
        )
        assertNotNull(myFixture.findClass("demo.MyController").getAnnotation(SpringWebClasses.REST_CONTROLLER))
        assertEmpty(problems)
    }

    fun testDirectHttpRequestHandlerIsExcluded() {
        val problems = problemsIn(
            "LegacyHandler.kt", """
            package demo

            import org.springframework.stereotype.Controller
            import org.springframework.web.HttpRequestHandler

            @Controller("/files/**")
            class LegacyHandler : HttpRequestHandler {
                override fun handleRequest(
                    request: jakarta.servlet.http.HttpServletRequest,
                    response: jakarta.servlet.http.HttpServletResponse
                ) = Unit
            }
            """.trimIndent()
        )
        val handler = myFixture.findClass("demo.LegacyHandler")
        assertStereotype("demo.LegacyHandler", SpringWebClasses.CONTROLLER)
        assertTrue(InheritanceUtil.isInheritor(handler, HTTP_REQUEST_HANDLER))
        val handleRequest = handler.findMethodsByName("handleRequest", false).single()
        assertEquals(HTTP_REQUEST_HANDLER, handleRequest.findSuperMethods().single().containingClass?.qualifiedName)
        assertEquals(
            "jakarta.servlet.http.HttpServletRequest",
            handleRequest.parameterList.parameters[0].type.canonicalText
        )
        assertEquals(
            "jakarta.servlet.http.HttpServletResponse",
            handleRequest.parameterList.parameters[1].type.canonicalText
        )
        assertEmpty(problems)
    }

    fun testExistingNamedPathMappingIsKept() {
        val result = applySingleQuickFix(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.*

            @RestController("/app")
            @RequestMapping(path = ["/x"])
            class AppController
            """.trimIndent()
        )
        assertTrue(result, result.contains("@RequestMapping(path = [\"/x\"])"))
        assertFalse(result, result.contains("/app"))
    }

    fun testQuickFixPreviewMatchesLaunchedResult() {
        myFixture.configureByText(
            "AppController.kt", """
            package demo

            import org.springframework.web.bind.annotation.RestController

            @RestController("/app")
            class AppController
            """.trimIndent()
        )
        assertStereotype("demo.AppController", SpringWebClasses.REST_CONTROLLER)
        val fix = myFixture.getAllQuickFixes().single()
        myFixture.checkPreviewAndLaunchAction(fix)
        assertTrue(myFixture.editor.document.text.contains("@RequestMapping(\"/app\")"))
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
