/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.ComposedMethodMappingFixture
import com.explyt.spring.web.loader.ComposedMethodMappingFixture.MappingShape
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtNamedFunction

class ComposedMethodMappingTest : ComposedMethodMappingKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testUnreadableMappingPathProducesNoEndpointRow() {
        myFixture.addFileToProject(
            "com/example/UnreadableController.kt", """
            package com.example
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            @RequestMapping("/api")
            class UnreadableController {
                @GetMapping(PathHolder.UNRESOLVED)
                fun unreadable(): String = ""

                @GetMapping("/known")
                fun known(): String = ""
            }
            """.trimIndent()
        )
        val unreadableFixture = object : ComposedMethodMappingFixture(myFixture, module, "UnreadableController") {
            override fun identifierOf(method: PsiMethod): PsiElement = (method.navigationElement as KtNamedFunction).nameIdentifier!!
        }
        unreadableFixture.assertMappingPathCannotBeEvaluated("unreadable")
        assertEquals(listOf("/api/known" to listOf("GET")), unreadableFixture.endpointRows("known"))
        assertEquals(emptyList<Pair<String, List<String>>>(), unreadableFixture.endpointRows("unreadable"))
    }

    fun testBareDirectMappingServesTheClassPrefix() =
        assertEquals(listOf("/api" to listOf("GET")), fixture.endpointRows("directNoPath"))

    fun testLiteralDirectMappingServesTheClassPrefixAndPath() =
        assertEquals(listOf("/api/items" to listOf("GET")), fixture.endpointRows("normalLiteralPath"))

    fun testBareComposedMappingServesItsAliasDefaultPath() =
        assertEquals(listOf("/api/items" to listOf("GET")), fixture.endpointRows("bare"))

    fun testPathSetAtUseSiteOverridesTheAliasDefault() =
        assertEquals(listOf("/api/v2" to listOf("GET")), fixture.endpointRows("useSite"))

    fun testOnlyTheFirstOfTwoComposedMappingsIsServed() =
        assertEquals(listOf("/api/a" to listOf("GET")), fixture.endpointRows("getThenPost"))

    fun testOnlyTheFirstOfTwoComposedMappingsIsServedInReversedOrder() =
        assertEquals(listOf("/api/b" to listOf("POST")), fixture.endpointRows("postThenGet"))

    fun testDirectRequestMappingBeatsComposedMappingDeclaredAfterIt() =
        assertEquals(listOf("/api/d" to emptyList<String>()), fixture.endpointRows("directThenComposed"))

    fun testDirectRequestMappingBeatsComposedMappingDeclaredBeforeIt() =
        assertEquals(listOf("/api/d" to emptyList<String>()), fixture.endpointRows("composedThenDirect"))

    fun testAliasDefaultReachesThroughTwoComposedLevels() =
        assertEquals(listOf("/api/lvl" to listOf("GET")), fixture.endpointRows("twoLevel"))

    fun testConventionPathOfComposedMappingIsServedBeforeSpring7() =
        assertEquals(listOf("/api/conv" to listOf("GET")), fixture.endpointRows("convention"))

    fun testEndpointActionsGutterReadsTheAliasDefaultPath() =
        assertEquals(MappingShape("/api/items", listOf("GET")), fixture.gutterShape("bare"))

    fun testEndpointActionsGutterReadsThePathSetAtUseSite() =
        assertEquals(MappingShape("/api/v2", listOf("GET")), fixture.gutterShape("useSite"))

    fun testEndpointActionsGutterReadsOnlyTheFirstComposedMapping() =
        assertEquals(GET_A, fixture.gutterShape("getThenPost"))

    fun testEndpointActionsGutterReadsOnlyTheFirstComposedMappingInReversedOrder() =
        assertEquals(POST_B, fixture.gutterShape("postThenGet"))

    fun testEndpointActionsGutterLetsTheDirectRequestMappingDeclaredFirstWin() =
        assertEquals(MappingShape("/api/d", emptyList()), fixture.gutterShape("directThenComposed"))

    fun testEndpointActionsGutterLetsTheDirectRequestMappingDeclaredLastWin() =
        assertEquals(MappingShape("/api/d", emptyList()), fixture.gutterShape("composedThenDirect"))

    fun testRunInSwaggerReadsTheAliasDefaultPath() =
        assertEquals(MappingShape("/api/items", listOf("GET")), fixture.runInSwaggerShape("bare"))

    fun testRunInSwaggerReadsOnlyTheFirstComposedMappingInReversedOrder() =
        assertEquals(POST_B, fixture.runInSwaggerShape("postThenGet"))

    fun testOpenApiEndpointInfoReadsTheAliasDefaultPath() =
        assertEquals(MappingShape("/api/items", listOf("GET")), fixture.openApiShape("bare"))

    fun testOpenApiEndpointInfoReadsOnlyTheFirstComposedMapping() =
        assertEquals(GET_A, fixture.openApiShape("getThenPost"))

    fun testOpenApiEndpointInfoReadsOnlyTheFirstComposedMappingInReversedOrder() =
        assertEquals(POST_B, fixture.openApiShape("postThenGet"))

    private companion object {
        val GET_A = MappingShape("/api/a", listOf("GET"), produces = listOf("text/plain"))
        val POST_B = MappingShape("/api/b", listOf("POST"), consumes = listOf("application/json"))
    }
}

class ComposedMethodMappingSpring7Test : ComposedMethodMappingKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary("org.springframework:spring-webmvc:7.0.9", true))

    fun testConventionPathOfComposedMappingIsIgnoredInSpring7() =
        assertEquals(listOf("/api" to listOf("GET")), fixture.endpointRows("convention"))

    fun testBareComposedMappingServesItsAliasDefaultPathInSpring7() =
        assertEquals(listOf("/api/items" to listOf("GET")), fixture.endpointRows("bare"))
}

abstract class ComposedMethodMappingKotlinTestCase : ExplytKotlinLightTestCase() {

    protected val fixture by lazy {
        object : ComposedMethodMappingFixture(myFixture, module) {
            override fun identifierOf(method: PsiMethod): PsiElement =
                (method.navigationElement as KtNamedFunction).nameIdentifier!!
        }
    }

    override fun setUp() {
        super.setUp()
        addSources()
    }

    private fun addSources() {
        myFixture.addFileToProject(
            "com/example/ComposedMappings.kt", """
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

            @Retention(AnnotationRetention.RUNTIME)
            @Target(AnnotationTarget.FUNCTION)
            @RequestMapping(method = [RequestMethod.GET])
            annotation class ApiConv(val path: Array<String> = ["/conv"])

            @Retention(AnnotationRetention.RUNTIME)
            @Target(AnnotationTarget.FUNCTION)
            @ApiGet(path = ["/lvl"])
            annotation class ApiGetV
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ItemsController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PostMapping
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            @RequestMapping("/api")
            class ItemsController {
                @ApiGet
                fun bare(): String = ""

                @ApiGet(path = ["/v2"])
                fun useSite(): String = ""

                @GetMapping(path = ["/a"], produces = ["text/plain"])
                @PostMapping(path = ["/b"], consumes = ["application/json"])
                fun getThenPost(): String = ""

                @PostMapping(path = ["/b"], consumes = ["application/json"])
                @GetMapping(path = ["/a"], produces = ["text/plain"])
                fun postThenGet(): String = ""

                @RequestMapping("/d")
                @ApiGet
                fun directThenComposed(): String = ""

                @ApiGet
                @RequestMapping("/d")
                fun composedThenDirect(): String = ""

                @ApiGetV
                fun twoLevel(): String = ""

                @GetMapping
                fun directNoPath(): String = ""

                @GetMapping("/items")
                fun normalLiteralPath(): String = ""

                @ApiConv
                fun convention(): String = ""
            }
            """.trimIndent()
        )
    }
}
