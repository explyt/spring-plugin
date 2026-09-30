/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.builder.openapi.yaml.OpenApiYamlPathHttpTypeBuilder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypes
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

/**
 * A Kotlin `suspend` handler read through the signature it declares.
 *
 * The compiled method carries the coroutine continuation as one more, last parameter and returns `Object`. Spring MVC
 * supplies the continuation and answers with the declared type, so reading the compiled shape documented a
 * parameter no request carries and a response of type `object`.
 */
class HandlerSignatureTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.kotlin_1_9_22,
    )

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/ShortLinkController.kt", """
            package com.example

            import org.springframework.http.ResponseEntity
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.PostMapping
            import org.springframework.web.bind.annotation.RestController

            class ShortLink(val code: String, val target: String)

            @RestController
            class ShortLinkController {
                @GetMapping("/api/short-links/{code}")
                suspend fun find(@PathVariable code: String): ResponseEntity<ShortLink> = TODO()

                @PostMapping("/api/short-links/{code}/touch")
                suspend fun touch(@PathVariable code: String) {}

                @GetMapping("/api/short-links/{code}/plain")
                fun plain(@PathVariable code: String): ResponseEntity<ShortLink> = TODO()
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/JavaShortLinkController.java", """
            package com.example;

            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class JavaShortLinkController {
                @GetMapping("/api/java/short-links/{code}")
                public ResponseEntity<ShortLink> find(@PathVariable String code) { return null; }
            }
            """.trimIndent()
        )
    }

    fun testSuspendHandlerHasNoContinuationParameter() {
        val find = method("com.example.ShortLinkController", "find")
        assertEquals(
            "Precondition: the compiled method carries the continuation as its last parameter",
            listOf("code", "\$completion"), find.parameterList.parameters.map { it.name }
        )
        assertEquals("Precondition: the compiled method returns Object", "java.lang.Object", find.returnType?.canonicalText)

        assertEquals(listOf("code"), HandlerSignature.requestParameters(find).map { it.name })
        assertEquals(
            "org.springframework.http.ResponseEntity<com.example.ShortLink>",
            HandlerSignature.declaredReturnType(find)?.canonicalText
        )
    }

    fun testSuspendHandlerReturningUnitAnswersVoid() {
        val touch = method("com.example.ShortLinkController", "touch")

        assertEquals(listOf("code"), HandlerSignature.requestParameters(touch).map { it.name })
        assertEquals(PsiTypes.voidType(), HandlerSignature.declaredReturnType(touch))
    }

    fun testNonSuspendKotlinAndJavaHandlersAreReadAsCompiled() {
        for (handler in listOf(
            method("com.example.ShortLinkController", "plain"),
            method("com.example.JavaShortLinkController", "find"),
        )) {
            assertEquals(handler.parameterList.parameters.asList(), HandlerSignature.requestParameters(handler))
            assertEquals(handler.returnType, HandlerSignature.declaredReturnType(handler))
        }
    }

    /** "Add endpoint to OpenAPI" documents the declared response type instead of `object`. */
    fun testOpenApiResponseOfASuspendHandlerIsTheDeclaredType() {
        val endpoint = SpringWebUtil.getEndpointInfo(method("com.example.ShortLinkController", "find").toUElement() as UMethod)
            ?: error("find is not an endpoint")

        assertEquals("com.example.ShortLink", endpoint.returnTypeFqn)
        val yaml = StringBuilder().also { OpenApiYamlPathHttpTypeBuilder(endpoint, "GET", builder = it).build() }.toString()
        assertTrue("The response schema refers to ShortLink, got:\n$yaml", yaml.contains("#/components/schemas/ShortLink'"))
    }

    private fun method(className: String, name: String): PsiMethod {
        val controller = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
            ?: error("$className is not in the fixture")
        return controller.findMethodsByName(name, false).single()
    }
}
