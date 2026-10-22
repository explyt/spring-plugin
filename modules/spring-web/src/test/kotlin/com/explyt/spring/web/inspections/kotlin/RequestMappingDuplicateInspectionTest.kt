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

    fun testComposedMappingWithAliasDefaultPathDuplicatesAGetMapping() {
        addComposedMapping()
        @Language("kotlin") val controller = """
            import com.example.ApiGet
            import org.springframework.web.bind.annotation.*

            @RestController
            class ComposedDuplicateController {

                @<error descr="Duplicated endpoint 'listed'">ApiGet</error>
                fun bare() = "bare"

                @GetMapping(<error descr="Duplicated endpoint 'bare'">"/items"</error>)
                fun listed() = "listed"
            }
            """.trimIndent()
        myFixture.configureByText("ComposedDuplicateController.kt", controller)
        myFixture.testHighlighting("ComposedDuplicateController.kt")
    }

    fun testComposedMappingWithPathSetAtUseSiteDoesNotDuplicateTheAliasDefault() {
        addComposedMapping()
        @Language("kotlin") val controller = """
            import com.example.ApiGet
            import org.springframework.web.bind.annotation.*

            @RestController
            class ComposedDistinctController {

                @ApiGet(path = ["/v2"])
                fun useSite() = "useSite"

                @GetMapping("/items")
                fun listed() = "listed"

                @PostMapping(<error descr="Duplicated endpoint 'secondPost'">"/same"</error>)
                fun firstPost() = "firstPost"

                @PostMapping(<error descr="Duplicated endpoint 'firstPost'">"/same"</error>)
                fun secondPost() = "secondPost"
            }
            """.trimIndent()
        myFixture.configureByText("ComposedDistinctController.kt", controller)
        myFixture.testHighlighting("ComposedDistinctController.kt")
    }

    private fun addComposedMapping() {
        myFixture.addFileToProject(
            "com/example/ApiGet.kt", """
            package com.example

            import org.springframework.core.annotation.AliasFor
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RequestMethod

            @Retention(AnnotationRetention.RUNTIME)
            @Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
            @RequestMapping(method = [RequestMethod.GET])
            annotation class ApiGet(
                @get:AliasFor(annotation = RequestMapping::class, attribute = "path")
                val path: Array<String> = ["/items"],
            )
            """.trimIndent()
        )
        val composed = myFixture.findClass("com.example.ApiGet")
        assertTrue(
            "Precondition: @ApiGet is meta-annotated with @RequestMapping",
            composed.hasAnnotation("org.springframework.web.bind.annotation.RequestMapping")
        )
    }
}
