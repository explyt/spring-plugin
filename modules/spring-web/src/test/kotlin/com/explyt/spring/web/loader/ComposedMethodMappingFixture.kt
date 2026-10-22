/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.providers.ControllerEndpointActionsLineMarkerProvider
import com.explyt.spring.web.providers.EndpointIconGutterHandler
import com.explyt.spring.web.providers.EndpointRunLineMarkerProvider
import com.explyt.spring.web.providers.RunInSwaggerAction
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.HandlerMethods
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElement

abstract class ComposedMethodMappingFixture(
    private val codeInsightFixture: CodeInsightTestFixture,
    private val module: Module,
    private val controllerName: String = "ItemsController",
) {

    data class MappingShape(
        val path: String,
        val methods: List<String>,
        val produces: List<String> = emptyList(),
        val consumes: List<String> = emptyList(),
    )

    abstract fun identifierOf(method: PsiMethod): PsiElement

    fun endpointRows(methodName: String): List<Pair<String, List<String>>> {
        val rows = controllerEndpoints()
        assertTrue(
            "Precondition: $methodName is a handler of the fixture",
            handlerNames().contains(methodName)
        )
        return rows.filter { it.first == methodName }.map { it.second to it.third }.sortedBy { it.first }
    }

    fun assertMappingPathCannotBeEvaluated(methodName: String) {
        val method = handler(methodName).toUElement() as UMethod
        val mappings = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        val annotation = mappings.nearestAnnotation(method as UAnnotated)
        assertNotNull("Precondition: $methodName declares a mapping", annotation)
        val values = mappings.getAnnotationMemberValues(annotation!!, setOf("path", "value"))
        assertEquals("Precondition: $methodName declares one path value", 1, values.size)
        assertNull("Precondition: the declared path cannot be evaluated", values.single().evaluateString())
    }

    fun gutterShape(methodName: String): MappingShape {
        assertComposedMappingsAreMetaAnnotated()
        val markers = mutableListOf<LineMarkerInfo<*>>()
        ControllerEndpointActionsLineMarkerProvider()
            .collectSlowLineMarkers(mutableListOf(identifierOf(handler(methodName))), markers)
        assertEquals("Precondition: one Endpoint Actions marker on $methodName", 1, markers.size)
        return (markers.single().navigationHandler as EndpointIconGutterHandler).endpointInfo.shape()
    }

    fun runInSwaggerShape(methodName: String): MappingShape {
        assertComposedMappingsAreMetaAnnotated()
        val actions = EndpointRunLineMarkerProvider().getInfo(identifierOf(handler(methodName)))?.actions
            ?.filterIsInstance<RunInSwaggerAction>().orEmpty()
        assertEquals("Precondition: one Run in Swagger action on $methodName", 1, actions.size)
        return actions.single().endpoints().single().shape()
    }

    fun openApiShape(methodName: String): MappingShape {
        assertComposedMappingsAreMetaAnnotated()
        val controller = controller()
        val prefix = HandlerMethods.requestMappingPrefixes(
            controller, MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        ).single()
        val uMethod = handler(methodName).toUElement() as UMethod
        val endpoint = SpringWebUtil.getEndpointInfo(uMethod, prefix)
        assertNotNull("Precondition: $methodName is described for OpenAPI", endpoint)
        return endpoint!!.shape()
    }

    private fun EndpointInfo.shape() =
        MappingShape(path, requestMethods, produces.toList(), consumes.toList())

    private fun controllerEndpoints(): List<Triple<String?, String, List<String>>> {
        assertComposedMappingsAreMetaAnnotated()
        val rows = SpringWebEndpointsSearcher.getInstance(codeInsightFixture.project).getAllEndpoints(module)
            .filter { it.type == EndpointType.SPRING_MVC && it.containingClass?.name == controllerName }
            .map { Triple((it.psiElement.toUElement() as? UMethod)?.name, it.path, it.requestMethods) }
        assertFalse("Precondition: the loader finds the controller", rows.isEmpty())
        return rows
    }

    private fun handlerNames(): Set<String> = controller().methods.mapTo(mutableSetOf()) { it.name }

    private fun handler(methodName: String): PsiMethod {
        val methods = controller().findMethodsByName(methodName, false)
        assertEquals("Precondition: $methodName is a handler of the fixture", 1, methods.size)
        return methods.single()
    }

    private fun controller(): PsiClass {
        val controller = facade().findClass("com.example.$controllerName", GlobalSearchScope.projectScope(codeInsightFixture.project))
        assertNotNull("Precondition: the controller is in the fixture", controller)
        return controller!!
    }

    private fun assertComposedMappingsAreMetaAnnotated() {
        val projectScope = GlobalSearchScope.projectScope(codeInsightFixture.project)
        assertNotNull(
            "Precondition: @RequestMapping resolves",
            facade().findClass(SpringWebClasses.REQUEST_MAPPING, GlobalSearchScope.allScope(codeInsightFixture.project))
        )
        for (name in listOf("ApiGet", "ApiConv")) {
            val composed = facade().findClass("com.example.$name", projectScope)
            assertNotNull("Precondition: $name is in the fixture", composed)
            assertTrue(
                "Precondition: $name is meta-annotated with @RequestMapping",
                composed!!.hasAnnotation(SpringWebClasses.REQUEST_MAPPING)
            )
        }
        val twoLevel = facade().findClass("com.example.ApiGetV", projectScope)
        assertNotNull("Precondition: ApiGetV is in the fixture", twoLevel)
        assertTrue("Precondition: ApiGetV is meta-annotated with @ApiGet", twoLevel!!.hasAnnotation("com.example.ApiGet"))
    }

    private fun facade() = JavaPsiFacade.getInstance(codeInsightFixture.project)
}
