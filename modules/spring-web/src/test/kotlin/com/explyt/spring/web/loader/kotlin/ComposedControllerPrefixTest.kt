/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.HandlerMethods
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

class ComposedControllerPrefixTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testAliasDefaultPrefixOfComposedControllerIsServedOnPlainUse() =
        assertItemsPaths("@ApiController", listOf("/api/items"))

    fun testPrefixSetAtUseSiteOverridesTheControllerAliasDefault() =
        assertItemsPaths("@ApiController(path = [\"/v2\"])", listOf("/v2/items"))

    fun testTwoPrefixesSetAtUseSiteServeTheHandlerUnderEach() =
        assertItemsPaths("@ApiController(path = [\"/x\", \"/y\"])", listOf("/x/items", "/y/items"))

    fun testAliasDefaultOfValueAttributeIsTheControllerPrefix() =
        assertItemsPaths("@ValueController", listOf("/dval/items"))

    fun testAliasDefaultReachesThroughTwoComposedLevels() =
        assertItemsPaths("@ApiNested", listOf("/api/items"))

    fun testPrefixFixedOnTheMetaAnnotationIsTheControllerPrefix() =
        assertItemsPaths("@FixedController", listOf("/fixed/items"))

    fun testDirectRequestMappingPrefixWinsOverComposedControllerPrefix() =
        assertOnlyDirectPrefixIsServed("@ApiController @RequestMapping(\"/p\")")

    fun testDirectRequestMappingPrefixDeclaredFirstWinsOverComposedControllerPrefix() =
        assertOnlyDirectPrefixIsServed("@RequestMapping(\"/p\") @ApiController")

    fun testHandlerMethodsReadOnlyTheDirectPrefixBesideComposedController() =
        assertHandlerMethodsReadOnlyTheDirectPrefix("@ApiController @RequestMapping(\"/p\")")

    fun testHandlerMethodsReadOnlyTheDirectPrefixDeclaredFirstBesideComposedController() =
        assertHandlerMethodsReadOnlyTheDirectPrefix("@RequestMapping(\"/p\") @ApiController")

    private fun assertItemsPaths(usage: String, expected: List<String>) {
        addSources(usage)
        assertComposedAnnotationsAreMetaAnnotated()

        assertEquals(expected, itemsPaths())
    }

    private fun assertOnlyDirectPrefixIsServed(usage: String) {
        addSources(usage)
        assertComposedAnnotationsAreMetaAnnotated()
        assertControllerCarriesDirectAndComposedMapping()

        assertEquals(listOf("/p/items"), itemsPaths())
    }

    private fun assertHandlerMethodsReadOnlyTheDirectPrefix(usage: String) {
        addSources(usage)
        assertComposedAnnotationsAreMetaAnnotated()
        assertControllerCarriesDirectAndComposedMapping()
        assertFalse("Precondition: the loader finds the controller", itemsPaths().isEmpty())

        val prefixes = HandlerMethods.requestMappingPrefixes(
            itemsController(), MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        )

        assertEquals(listOf("/p"), prefixes)
    }

    private fun itemsPaths(): List<String> {
        val paths = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.type == EndpointType.SPRING_MVC && it.containingClass?.name == "ItemsController" }
            .map { it.path }
            .sorted()
        assertFalse("Precondition: the loader finds the controller", paths.isEmpty())
        return paths
    }

    private fun itemsController(): PsiClass {
        val controller = JavaPsiFacade.getInstance(project)
            .findClass("com.example.ItemsController", GlobalSearchScope.projectScope(project))
        assertNotNull("Precondition: the controller is in the fixture", controller)
        return controller!!
    }

    private fun assertControllerCarriesDirectAndComposedMapping() {
        val controller = itemsController()
        assertTrue(
            "Precondition: the controller carries a direct @RequestMapping",
            controller.hasAnnotation(SpringWebClasses.REQUEST_MAPPING)
        )
        assertTrue(
            "Precondition: the controller carries the composed @ApiController",
            controller.hasAnnotation("com.example.ApiController")
        )
    }

    private fun assertComposedAnnotationsAreMetaAnnotated() {
        val facade = JavaPsiFacade.getInstance(project)
        assertNotNull(
            "Precondition: @RequestMapping resolves",
            facade.findClass(SpringWebClasses.REQUEST_MAPPING, GlobalSearchScope.allScope(project))
        )
        for (name in listOf("ApiController", "ValueController", "FixedController")) {
            val composed = facade.findClass("com.example.$name", GlobalSearchScope.projectScope(project))
            assertNotNull("Precondition: $name is in the fixture", composed)
            assertTrue(
                "Precondition: $name is meta-annotated with @RequestMapping",
                composed!!.hasAnnotation(SpringWebClasses.REQUEST_MAPPING)
            )
        }
        assertNotNull(
            "Precondition: ApiNested is in the fixture",
            facade.findClass("com.example.ApiNested", GlobalSearchScope.projectScope(project))
        )
    }

    private fun addSources(usage: String) {
        myFixture.addFileToProject(
            "com/example/ComposedControllers.kt", """
            package com.example

            import org.springframework.core.annotation.AliasFor
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.RUNTIME)
            @RestController
            @RequestMapping
            annotation class ApiController(
                @get:AliasFor(annotation = RequestMapping::class, attribute = "path")
                val path: Array<String> = ["/api"],
            )

            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.RUNTIME)
            @RestController
            @RequestMapping
            annotation class ValueController(
                @get:AliasFor(annotation = RequestMapping::class, attribute = "value")
                val value: Array<String> = ["/dval"],
            )

            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.RUNTIME)
            @RestController
            @RequestMapping("/fixed")
            annotation class FixedController(val name: String = "")

            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.RUNTIME)
            @ApiController
            annotation class ApiNested
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ItemsController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RequestMapping

            $usage
            class ItemsController {
                @GetMapping("/items")
                fun items(): String = "items"
            }
            """.trimIndent()
        )
    }
}
