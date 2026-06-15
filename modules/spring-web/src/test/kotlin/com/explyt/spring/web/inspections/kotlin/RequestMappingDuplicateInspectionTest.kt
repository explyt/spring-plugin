/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.kotlin

import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.RequestMappingDuplicateInspection
import org.intellij.lang.annotations.Language

class RequestMappingDuplicateInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWeb_6_0_7)


    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(RequestMappingDuplicateInspection::class.java)
    }

    fun testCorrectMethod() {
        @Language("kotlin") val trimIndent = """            
            import org.springframework.web.bind.annotation.*

            @RestController
            class DuplicatedEndpointController {

                @RequestMapping(path = [<error descr="Duplicated endpoint 'conflictedGet'">"/same/uri"</error>], method = [RequestMethod.GET])
                @ResponseBody
                fun conflictedRequestMapping() = "conflictedRequestMapping"               

                @GetMapping(<error descr="Duplicated endpoint 'conflictedRequestMapping'">"/same/uri"</error>)
                @ResponseBody
                fun conflictedGet() = "conflictedGet"             

                @PostMapping("/same/uri")
                @ResponseBody
                fun postWithoutConflict() = "postWithoutConflict"             
                
                @PostMapping(<error descr="Duplicated endpoint 'testWithoutSlash'">"/slash/test"</error>)
                @ResponseBody
                fun testWithSlash() = ""

                @PostMapping(<error descr="Duplicated endpoint 'testWithSlash'">"slash/test"</error>)
                @ResponseBody
                fun testWithoutSlash() = ""
                
                @PostMapping("some")
                @ResponseBody
                fun some1() = ""
                
                @GetMapping("some")
                @ResponseBody
                fun some2() = ""
            }
            """.trimIndent()
        myFixture.configureByText("DuplicatedEndpointController.kt", trimIndent)
        myFixture.testHighlighting("DuplicatedEndpointController.kt")
    }
}