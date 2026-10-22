/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.explyt.spring.web.providers.CoRouterEndpointActionsLineMarkerProvider
import com.explyt.spring.web.providers.EndpointIconGutterHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class CoRouterAnyMethodOpenApiActionTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.kotlin_1_9_22
    )

    fun testPathRouteThatAnswersEveryMethodOffersAnOpenApiDescription() {
        addRouter()

        val handler = gutterHandlerOf("path")
        assertEquals("/x", handler.endpointInfo.path)
        assertEquals("Precondition: path(\"/x\", handler) declares no method", emptyList<String>(), handler.endpointInfo.requestMethods)

        assertTrue(
            "A route that answers every method must offer Generate OpenApi Doc",
            handler.offersOpenApiDescription
        )
        assertTrue(AddEndpointToOpenApiIntention(handler.endpointInfo).isAvailable(project, null, null))
    }

    fun testGetRouteOffersAnOpenApiDescription() {
        addRouter()

        val handler = gutterHandlerOf("GET")
        assertEquals("/y", handler.endpointInfo.path)
        assertEquals(listOf("GET"), handler.endpointInfo.requestMethods)

        assertTrue(handler.offersOpenApiDescription)
        assertTrue(AddEndpointToOpenApiIntention(handler.endpointInfo).isAvailable(project, null, null))
    }

    private fun addRouter() {
        myFixture.addFileToProject(
            "com/example/ItemHandler.kt", """
            package com.example

            import org.springframework.stereotype.Service
            import org.springframework.web.reactive.function.server.ServerRequest
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.buildAndAwait

            @Service
            class ItemHandler {
                suspend fun handle(request: ServerRequest): ServerResponse = ServerResponse.ok().buildAndAwait()
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ItemRoutes.kt", """
            package com.example

            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.RouterFunction
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.coRouter

            @Configuration
            class ItemRoutes {
                @Bean
                fun routes(handler: ItemHandler): RouterFunction<ServerResponse> = coRouter {
                    path("/x", handler::handle)
                    GET("/y", handler::handle)
                }
            }
            """.trimIndent()
        )
    }

    private fun gutterHandlerOf(routeName: String): EndpointIconGutterHandler {
        val file = myFixture.findClass("com.example.ItemRoutes").containingFile.navigationElement.containingFile
        val call = PsiTreeUtil.findChildrenOfType(file, KtCallExpression::class.java)
            .single { it.calleeExpression?.text == routeName }
        val identifier = (call.calleeExpression as KtNameReferenceExpression).getReferencedNameElement()
        val markers = mutableListOf<LineMarkerInfo<*>>()
        CoRouterEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(identifier), markers)
        return markers.singleOrNull()?.navigationHandler as? EndpointIconGutterHandler
            ?: error("No endpoint actions marker on $routeName, got $markers")
    }
}
