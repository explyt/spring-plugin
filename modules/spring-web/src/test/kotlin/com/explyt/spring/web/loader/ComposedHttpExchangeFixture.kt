/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.providers.EndpointIconGutterHandler
import com.explyt.spring.web.providers.HttpExchangeEndpointActionsLineMarkerProvider
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

abstract class ComposedHttpExchangeFixture(
    private val codeInsightFixture: CodeInsightTestFixture,
    private val module: Module,
    private val springMajor: Int,
) {

    data class ExchangeRow(val path: String, val methods: List<String>)

    fun assertExchangeAttributeExists(attribute: String) {
        val annotation = facade().findClass(
            SpringWebClasses.HTTP_EXCHANGE, GlobalSearchScope.allScope(codeInsightFixture.project)
        )
        assertNotNull("Precondition: @HttpExchange resolves", annotation)
        assertEquals("Precondition: @HttpExchange declares $attribute", 1, annotation!!.findMethodsByName(attribute, false).size)
    }

    abstract fun identifierOf(method: PsiMethod): PsiElement

    fun rows(clientName: String, methodName: String): List<ExchangeRow> {
        assertPreconditions()
        method(clientName, methodName)
        return clientRows(clientName).filter { it.first == methodName }.map { it.second }.sortedBy { it.path }
    }

    fun allRows(clientName: String): List<ExchangeRow> {
        assertPreconditions()
        assertTrue(
            "Precondition: $clientName declares exchange methods",
            client(clientName).methods.any { it.isMetaAnnotatedBy(SpringWebClasses.HTTP_EXCHANGE) }
        )
        return clientRows(clientName).map { it.second }.sortedBy { it.path }
    }

    fun gutterRows(clientName: String, methodName: String): List<ExchangeRow> {
        assertPreconditions()
        val markers = mutableListOf<LineMarkerInfo<*>>()
        HttpExchangeEndpointActionsLineMarkerProvider()
            .collectSlowLineMarkers(mutableListOf(identifierOf(method(clientName, methodName))), markers)
        return markers.map { marker ->
            val info = (marker.navigationHandler as EndpointIconGutterHandler).endpointInfo
            ExchangeRow(info.path, info.requestMethods)
        }
    }

    private fun clientRows(clientName: String): List<Pair<String?, ExchangeRow>> =
        SpringWebEndpointsSearcher.getInstance(codeInsightFixture.project).getAllEndpoints(module)
            .filter { it.type == EndpointType.SPRING_HTTP_EXCHANGE && it.containingClass?.name == clientName }
            .map { (it.psiElement.toUElement() as? UMethod)?.name to ExchangeRow(it.path, it.requestMethods) }

    private fun method(clientName: String, methodName: String): PsiMethod {
        val methods = client(clientName).findMethodsByName(methodName, false)
        assertEquals("Precondition: $methodName is declared once on $clientName", 1, methods.size)
        val method = methods.single()
        assertTrue(
            "Precondition: $methodName carries an @HttpExchange-family annotation",
            method.isMetaAnnotatedBy(SpringWebClasses.HTTP_EXCHANGE)
        )
        return method
    }

    private fun client(clientName: String): PsiClass {
        val client = facade().findClass("com.example.$clientName", projectScope())
        assertNotNull("Precondition: $clientName is in the fixture", client)
        return client!!
    }


    private fun assertPreconditions() {
        val allScope = GlobalSearchScope.allScope(codeInsightFixture.project)
        assertNotNull(
            "Precondition: @HttpExchange resolves",
            facade().findClass(SpringWebClasses.HTTP_EXCHANGE, allScope)
        )
        assertNotNull(
            "Precondition: @GetExchange resolves",
            facade().findClass("org.springframework.web.service.annotation.GetExchange", allScope)
        )
        for (name in COMPOSED_EXCHANGES) {
            val composed = facade().findClass("com.example.$name", projectScope())
            assertNotNull("Precondition: $name is in the fixture", composed)
            assertTrue(
                "Precondition: $name is meta-annotated with @HttpExchange",
                composed!!.hasAnnotation(SpringWebClasses.HTTP_EXCHANGE)
            )
        }
        val twoLevel = facade().findClass("com.example.ApiGetV", projectScope())
        assertNotNull("Precondition: ApiGetV is in the fixture", twoLevel)
        assertTrue("Precondition: ApiGetV is meta-annotated with @ApiGet", twoLevel!!.hasAnnotation("com.example.ApiGet"))
        assertSpringCoreMajorVersion()
    }

    private fun assertSpringCoreMajorVersion() {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        val springVersion = facade().findClass("org.springframework.core.SpringVersion", scope)
        assertNotNull("Precondition: spring-core is on the module classpath", springVersion)
        val jarName = springVersion!!.containingFile.virtualFile.path.substringBefore("!/").substringAfterLast('/')
        assertTrue(
            "Precondition: spring-core jar $jarName is version $springMajor.x",
            jarName.startsWith("spring-core-$springMajor.")
        )
    }

    private fun projectScope() = GlobalSearchScope.projectScope(codeInsightFixture.project)

    private fun facade() = JavaPsiFacade.getInstance(codeInsightFixture.project)

    private companion object {
        val COMPOSED_EXCHANGES = listOf("ApiGet", "ApiPost", "ApiAccept", "ApiConv", "ApiType")
    }
}
