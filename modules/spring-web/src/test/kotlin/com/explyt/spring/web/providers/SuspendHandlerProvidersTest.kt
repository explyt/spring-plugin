/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Every gutter that builds a request description from a Kotlin `suspend` function reads the signature it declares.
 *
 * The compiled method returns `Object` and carries the coroutine continuation as its last parameter, so a provider
 * reading the compiled shape documents the response as `object`. A JAX-RS resource takes a parameter without a
 * binding annotation for the request body; a Kotlin parameter always carries a synthesized nullability annotation, so
 * no Kotlin body was found at all, and once it is, the continuation must not be mistaken for one.
 *
 * Retrofit and JAX-RS are not on the test classpath, so their annotations are declared as project sources: the
 * providers only look them up by name.
 */
class SuspendHandlerProvidersTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.kotlin_1_9_22,
    )

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/ShortLink.kt", "package com.example\n\nclass ShortLink(val code: String, val target: String)\n"
        )
    }

    /** "Run in Swagger" on a Spring MVC handler. */
    fun testRunInSwaggerDocumentsTheDeclaredResponse() {
        myFixture.addFileToProject(
            "com/example/ShortLinkController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class ShortLinkController {
                @GetMapping("/api/short-links/{code}")
                suspend fun find(@PathVariable code: String): ShortLink = TODO()
            }
            """.trimIndent()
        )

        val endpoint = runInSwaggerEndpoint(EndpointRunLineMarkerProvider(), "com.example.ShortLinkController", "find")

        assertEquals("com.example.ShortLink", endpoint.returnTypeFqn)
    }

    fun testHttpExchangeActionsDocumentTheDeclaredResponse() {
        myFixture.addFileToProject(
            "com/example/ShortLinkClient.kt", """
            package com.example

            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.service.annotation.HttpExchange

            interface ShortLinkClient {
                @HttpExchange(value = "/api/remote/short-links/{code}", method = "GET")
                suspend fun find(@PathVariable code: String): ShortLink
            }
            """.trimIndent()
        )

        val endpoint = actionsEndpoint(HttpExchangeEndpointActionsLineMarkerProvider(), "com.example.ShortLinkClient", "find")

        assertEquals("com.example.ShortLink", endpoint.returnTypeFqn)
    }

    fun testRetrofitRunMarkerDocumentsTheDeclaredResponse() {
        addRetrofitGet()

        val endpoint = runInSwaggerEndpoint(RetrofitRunLineMarkerProvider(), "com.example.RetrofitShortLinks", "find")

        assertEquals("com.example.ShortLink", endpoint.returnTypeFqn)
    }

    fun testRetrofitActionsDocumentTheDeclaredResponse() {
        addRetrofitGet()

        val endpoint = actionsEndpoint(RetrofitEndpointActionsLineMarkerProvider(), "com.example.RetrofitShortLinks", "find")

        assertEquals("com.example.ShortLink", endpoint.returnTypeFqn)
    }

    fun testJaxRsResourceDocumentsTheDeclaredResponseAndBody() {
        addJaxRsResource()

        val endpoint = runInSwaggerEndpoint(JaxRsRunLineMarkerProvider(), "com.example.ShortLinkResource", "create")

        assertEquals("com.example.ShortLink", endpoint.returnTypeFqn)
        assertEquals("The body is the declared parameter", "com.example.ShortLink", endpoint.requestBodyInfo?.typeFqn)
    }

    /** A request with no body parameter has no body: the continuation is not one. */
    fun testJaxRsContinuationIsNotTakenForTheRequestBody() {
        addJaxRsResource()

        val endpoint = runInSwaggerEndpoint(JaxRsRunLineMarkerProvider(), "com.example.ShortLinkResource", "touch")

        assertNull(
            "The continuation must not be documented as the request body, got ${endpoint.requestBodyInfo?.typeFqn}",
            endpoint.requestBodyInfo
        )
        assertEquals("java.lang.Void", endpoint.returnTypeFqn)
    }

    private fun runInSwaggerEndpoint(provider: RunLineMarkerContributor, className: String, methodName: String): EndpointInfo {
        val info = provider.getInfo(nameIdentifierOf(className, methodName))
            ?: error("No run marker on $className.$methodName")
        val action = info.actions?.filterIsInstance<RunInSwaggerAction>()?.single()
            ?: error("No Run in Swagger action on $className.$methodName")
        return action.endpoints().single()
    }

    private fun actionsEndpoint(provider: LineMarkerProviderDescriptor, className: String, methodName: String): EndpointInfo {
        val markers = mutableListOf<LineMarkerInfo<*>>()
        provider.collectSlowLineMarkers(mutableListOf(nameIdentifierOf(className, methodName)), markers)
        val handler = markers.singleOrNull()?.navigationHandler as? EndpointIconGutterHandler
            ?: error("No endpoint actions marker on $className.$methodName, got $markers")
        return handler.endpointInfo
    }

    private fun nameIdentifierOf(className: String, methodName: String): PsiElement {
        val method = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
            ?.findMethodsByName(methodName, false)?.single()
            ?: error("$className.$methodName is not in the fixture")
        assertEquals(
            "Precondition: the compiled method carries the continuation as its last parameter",
            "\$completion", method.parameterList.parameters.last().name
        )
        assertEquals("Precondition: the compiled method returns Object", "java.lang.Object", method.returnType?.canonicalText)
        return (method.navigationElement as KtNamedFunction).nameIdentifier!!
    }

    private fun addRetrofitGet() {
        myFixture.addFileToProject(
            "retrofit2/http/GET.java", """
            package retrofit2.http;

            import java.lang.annotation.*;

            @Target(ElementType.METHOD)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface GET { String value() default ""; }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/RetrofitShortLinks.kt", """
            package com.example

            import retrofit2.http.GET

            interface RetrofitShortLinks {
                @GET("api/short-links/latest")
                suspend fun find(): ShortLink
            }
            """.trimIndent()
        )
    }

    private fun addJaxRsResource() {
        jaxRsAnnotation(
            "HttpMethod",
            "@Target(ElementType.ANNOTATION_TYPE) @Retention(RetentionPolicy.RUNTIME) public @interface HttpMethod { String value(); }"
        )
        jaxRsAnnotation(
            "Path",
            "@Target({ElementType.TYPE, ElementType.METHOD}) @Retention(RetentionPolicy.RUNTIME) public @interface Path { String value(); }"
        )
        jaxRsAnnotation(
            "POST",
            "@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME) @HttpMethod(\"POST\") public @interface POST {}"
        )
        jaxRsAnnotation(
            "PathParam",
            "@Target(ElementType.PARAMETER) @Retention(RetentionPolicy.RUNTIME) public @interface PathParam { String value(); }"
        )
        myFixture.addFileToProject(
            "com/example/ShortLinkResource.kt", """
            package com.example

            import jakarta.ws.rs.POST
            import jakarta.ws.rs.Path
            import jakarta.ws.rs.PathParam

            @Path("/api/jaxrs/short-links")
            class ShortLinkResource {
                @POST
                @Path("/create")
                suspend fun create(link: ShortLink): ShortLink = link

                @POST
                @Path("/{code}/touch")
                suspend fun touch(@PathParam("code") code: String) {}
            }
            """.trimIndent()
        )
    }

    private fun jaxRsAnnotation(name: String, declaration: String) {
        myFixture.addFileToProject(
            "jakarta/ws/rs/$name.java",
            "package jakarta.ws.rs;\n\nimport java.lang.annotation.*;\n\n$declaration\n"
        )
    }
}
