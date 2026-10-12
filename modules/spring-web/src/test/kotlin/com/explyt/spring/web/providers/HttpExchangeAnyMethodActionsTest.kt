/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiMethod

class HttpExchangeAnyMethodActionsTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
    )

    fun testVerblessExchangeHasEndpointActions() {
        addClient()
        assertEquals(
            "Precondition: the model lists the verbless exchange as a route that answers every method",
            listOf("/api/x" to emptyList<String>()),
            modelEndpointsOf("any")
        )

        val handler = actionsHandlerOf("any")

        assertEquals("/api/x", handler?.endpointInfo?.path)
        assertEquals(emptyList<String>(), handler?.endpointInfo?.requestMethods)
        assertTrue(handler!!.offersOpenApiDescription)
    }

    fun testGetExchangeHasEndpointActions() {
        addClient()

        val handler = actionsHandlerOf("y") ?: error("No endpoint actions marker on ItemClient.y")

        assertEquals("/api/y", handler.endpointInfo.path)
        assertEquals(listOf("GET"), handler.endpointInfo.requestMethods)
    }

    private fun modelEndpointsOf(methodName: String): List<Pair<String, List<String>>> =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { (it.psiElement as? PsiMethod)?.name == methodName }
            .map { it.path to it.requestMethods }

    private fun actionsHandlerOf(methodName: String): EndpointIconGutterHandler? {
        val method = myFixture.findClass("com.example.ItemClient").findMethodsByName(methodName, false).single()
        val markers = mutableListOf<LineMarkerInfo<*>>()
        HttpExchangeEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(method.nameIdentifier!!), markers)
        return markers.singleOrNull()?.navigationHandler as? EndpointIconGutterHandler
    }

    private fun addClient() {
        myFixture.addFileToProject(
            "com/example/ItemClient.java", """
            package com.example;

            import org.springframework.web.service.annotation.HttpExchange;

            @HttpExchange("/api")
            public interface ItemClient {
                @HttpExchange("/x")
                String any();

                @HttpExchange(value = "/y", method = "GET")
                String y();
            }
            """.trimIndent()
        )
    }
}
