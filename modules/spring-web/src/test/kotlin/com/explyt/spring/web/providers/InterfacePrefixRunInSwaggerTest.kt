/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.SpringWebClasses
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import org.jetbrains.kotlin.psi.KtClass

class InterfacePrefixRunInSwaggerTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testEndpointRunInSwaggerUsesTheInterfacePrefix() {
        val method = addController().findMethodsByName("apps", false).single()
        val identifier = (method.navigationElement as org.jetbrains.kotlin.psi.KtNamedFunction).nameIdentifier!!
        val endpoints = EndpointRunLineMarkerProvider().getInfo(identifier)?.actions
            ?.filterIsInstance<RunInSwaggerAction>()?.single()?.endpoints().orEmpty()
        assertEquals(listOf("/api/apps"), endpoints.map { it.path })
    }

    fun testEndpointActionsUseTheInterfacePrefix() {
        val method = addController().findMethodsByName("apps", false).single()
        val identifier = (method.navigationElement as org.jetbrains.kotlin.psi.KtNamedFunction).nameIdentifier!!
        val markers = mutableListOf<com.intellij.codeInsight.daemon.LineMarkerInfo<*>>()
        ControllerEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(identifier), markers)
        val paths = markers.mapNotNull { (it.navigationHandler as? EndpointIconGutterHandler)?.endpointInfo?.path }
        assertEquals(listOf("/api/apps"), paths)
    }

    fun testControllerRunInSwaggerPrefixesThePathDeclaredOnTheInterface() {
        val controller = addController()
        val nameIdentifier = (controller.navigationElement as KtClass).nameIdentifier!!
        val action = ControllerRunLineMarkerProvider().getInfo(nameIdentifier)?.actions
            ?.filterIsInstance<RunInSwaggerAction>()?.single()
            ?: error("No Run in Swagger action on AppController")
        assertEquals(listOf("/api/apps"), action.endpoints().map { it.path })
    }

    private fun addController(): com.intellij.psi.PsiClass {
        myFixture.addFileToProject(
            "com/example/AppApi.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RequestMapping

            @RequestMapping("/api")
            interface AppApi {
                @GetMapping("/apps")
                fun apps(): String
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/AppController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.RestController

            @RestController
            class AppController : AppApi {
                override fun apps(): String = "apps"
            }
            """.trimIndent()
        )
        val controller = myFixture.findClass("com.example.AppController")
        val api = myFixture.findClass("com.example.AppApi")
        assertTrue("Precondition: the prefix is on the interface", api.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING))
        assertFalse("Precondition: the controller declares no prefix", controller.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING))

        assertTrue("Precondition: the interface declares the method mapping", (api.findMethodsByName("apps", false).single().navigationElement as org.jetbrains.kotlin.psi.KtNamedFunction).annotationEntries.isNotEmpty())
        assertTrue("Precondition: the controller method declares no annotations", (controller.findMethodsByName("apps", false).single().navigationElement as org.jetbrains.kotlin.psi.KtNamedFunction).annotationEntries.isEmpty())
        return controller
    }
}
