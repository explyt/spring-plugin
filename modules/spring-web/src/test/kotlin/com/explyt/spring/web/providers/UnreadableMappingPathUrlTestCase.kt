/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.HandlerMethods
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType

abstract class UnreadableMappingPathUrlTestCase : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    protected abstract val unreadableMethods: List<String>

    protected abstract val readableUrls: Map<String, String>

    protected abstract fun addController()

    override fun setUp() {
        super.setUp()
        addController()
    }

    fun testUnreadablePathIsDeclaredButNotRead() {
        unreadableMethods.forEach(::assertDeclaredButUnreadable)
    }

    fun testEndpointActionsOfferNoUrlForUnreadablePath() {
        unreadableMethods.forEach(::assertDeclaredButUnreadable)
        assertEquals(
            unreadableMethods.associateWith { emptyList<String>() },
            unreadableMethods.associateWith(::endpointActionsUrls)
        )
    }

    fun testRunInSwaggerOffersNoUrlForUnreadablePath() {
        unreadableMethods.forEach(::assertDeclaredButUnreadable)
        assertEquals(
            unreadableMethods.associateWith { emptyList<String>() },
            unreadableMethods.associateWith(::runInSwaggerUrls)
        )
    }

    fun testEndpointInfoIsAbsentForUnreadablePath() {
        unreadableMethods.forEach(::assertDeclaredButUnreadable)
        assertEquals(
            unreadableMethods.associateWith { null },
            unreadableMethods.associateWith { SpringWebUtil.getEndpointInfo(uMethod(it), "/api")?.path }
        )
    }

    fun testControllerRunInSwaggerListsOnlyReadablePaths() {
        unreadableMethods.forEach(::assertDeclaredButUnreadable)
        val action = ControllerRunLineMarkerProvider().getInfo(classIdentifier())?.actions
            ?.filterIsInstance<RunInSwaggerAction>()?.single()
            ?: error("No Run in Swagger action on ApiController")

        assertEquals(readableUrls.values.toList(), action.endpoints().map { it.path })
    }

    fun testEndpointActionsKeepReadableUrls() {
        assertEquals(readableUrls, readableUrls.keys.associateWith { endpointActionsUrls(it).single() })
    }

    fun testRunInSwaggerKeepsReadableUrls() {
        assertEquals(readableUrls, readableUrls.keys.associateWith { runInSwaggerUrls(it).single() })
    }

    fun testEndpointInfoKeepsReadableUrls() {
        assertEquals(
            readableUrls,
            readableUrls.keys.associateWith { SpringWebUtil.getEndpointInfo(uMethod(it), "/api")?.path }
        )
    }

    private fun assertDeclaredButUnreadable(methodName: String) {
        val mapping = HandlerMethods.methodMapping(
            method(methodName), MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        )
        assertTrue("Precondition: $methodName declares a path", mapping.pathsDeclared)
        assertEquals("Precondition: the path of $methodName is not readable", emptyList<String>(), mapping.paths.map { it.path })
    }

    private fun endpointActionsUrls(methodName: String): List<String> {
        val markers = mutableListOf<LineMarkerInfo<*>>()
        ControllerEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(identifier(methodName)), markers)
        return markers.mapNotNull { (it.navigationHandler as? EndpointIconGutterHandler)?.endpointInfo?.path }
    }

    private fun runInSwaggerUrls(methodName: String): List<String> =
        EndpointRunLineMarkerProvider().getInfo(identifier(methodName))?.actions
            ?.filterIsInstance<RunInSwaggerAction>()?.flatMap { action -> action.endpoints().map { it.path } }
            .orEmpty()

    private fun controller(): PsiClass = myFixture.findClass(CONTROLLER)

    private fun method(name: String): PsiMethod = controller().findMethodsByName(name, false).single()

    private fun uMethod(name: String): UMethod = method(name).toUElementOfType<UMethod>()!!

    private fun identifier(name: String): PsiElement = method(name).let {
        (it.navigationElement as? KtNamedFunction)?.nameIdentifier ?: it.nameIdentifier!!
    }

    private fun classIdentifier(): PsiElement = controller().let {
        (it.navigationElement as? KtClass)?.nameIdentifier ?: it.nameIdentifier!!
    }

    private companion object {
        const val CONTROLLER = "com.example.ApiController"
    }
}
