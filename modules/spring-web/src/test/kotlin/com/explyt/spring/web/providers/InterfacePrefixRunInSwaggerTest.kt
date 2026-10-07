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

    fun testControllerRunInSwaggerPrefixesThePathDeclaredOnTheInterface() {
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

        val nameIdentifier = (controller.navigationElement as KtClass).nameIdentifier!!
        val action = ControllerRunLineMarkerProvider().getInfo(nameIdentifier)?.actions
            ?.filterIsInstance<RunInSwaggerAction>()?.single()
            ?: error("No Run in Swagger action on AppController")

        assertEquals(listOf("/api/apps"), action.endpoints().map { it.path })
    }
}
