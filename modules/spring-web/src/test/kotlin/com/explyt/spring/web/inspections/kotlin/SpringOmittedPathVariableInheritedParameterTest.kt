/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.kotlin

import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.inspections.SpringOmittedPathVariableParameterInspection
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.InspectionManager
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

class SpringOmittedPathVariableInheritedParameterTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springWeb_6_0_7
    )

    fun testInheritedNamedPathVariableMissingFromTheOverrideMappingIsReportedInsideTheControllerFile() {
        myFixture.addFileToProject(
            "com/example/AppApi.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable

            interface AppApi {
                @GetMapping("/apps/{appId}")
                fun get(@PathVariable("appId") appId: String): String
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/AppController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class AppController : AppApi {
                @GetMapping("/apps/{id}")
                override fun get(appId: String): String = appId
            }
            """.trimIndent()
        )
        val interfaceParameter = myFixture.findClass("com.example.AppApi")
            .findMethodsByName("get", false).single().parameterList.parameters.single()
        val override = myFixture.findClass("com.example.AppController").findMethodsByName("get", false).single()
        assertTrue("Precondition: @PathVariable is on the interface", interfaceParameter.isMetaAnnotatedBy(SpringWebClasses.PATH_VARIABLE))
        assertFalse(
            "Precondition: the override has no @PathVariable",
            override.parameterList.parameters.single().isMetaAnnotatedBy(SpringWebClasses.PATH_VARIABLE)
        )

        val descriptors = SpringOmittedPathVariableParameterInspection()
            .checkMethod(override.toUElement() as UMethod, InspectionManager.getInstance(project), false)
            .orEmpty()

        val locations = descriptors.map { "${it.psiElement?.containingFile?.name}: ${it.psiElement?.text}" }
        assertEquals(
            "the omitted appId is reported on the override, inside AppController.kt: $locations",
            listOf("AppController.kt: appId: String", "AppController.kt: \"/apps/{id}\"").sorted(),
            locations.sorted()
        )
    }
}
