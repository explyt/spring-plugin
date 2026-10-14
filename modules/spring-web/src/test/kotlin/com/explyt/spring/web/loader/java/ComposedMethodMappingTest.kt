/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.ComposedMethodMappingFixture
import com.explyt.spring.web.loader.ComposedMethodMappingFixture.MappingShape
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class ComposedMethodMappingTest : ComposedMethodMappingJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testUnreadableMappingPathProducesNoEndpointRow() {
        myFixture.addFileToProject(
            "com/example/UnreadableController.java", """
            package com.example;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("/api")
            public class UnreadableController {
                @GetMapping(PathHolder.UNRESOLVED)
                public String unreadable() { return ""; }

                @GetMapping("/known")
                public String known() { return ""; }
            }
            """.trimIndent()
        )
        val unreadableFixture = object : ComposedMethodMappingFixture(myFixture, module, "UnreadableController") {
            override fun identifierOf(method: PsiMethod): PsiElement = method.nameIdentifier!!
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

class ComposedMethodMappingSpring7Test : ComposedMethodMappingJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary("org.springframework:spring-webmvc:7.0.9", true))

    fun testConventionPathOfComposedMappingIsIgnoredInSpring7() =
        assertEquals(listOf("/api" to listOf("GET")), fixture.endpointRows("convention"))

    fun testBareComposedMappingServesItsAliasDefaultPathInSpring7() =
        assertEquals(listOf("/api/items" to listOf("GET")), fixture.endpointRows("bare"))
}

abstract class ComposedMethodMappingJavaTestCase : ExplytJavaLightTestCase() {

    protected val fixture by lazy {
        object : ComposedMethodMappingFixture(myFixture, module) {
            override fun identifierOf(method: PsiMethod): PsiElement = method.nameIdentifier!!
        }
    }

    override fun setUp() {
        super.setUp()
        addSources()
    }

    private fun addSources() {
        myFixture.addFileToProject(
            "com/example/ComposedMappings.java", """
            package com.example;

            import org.springframework.core.annotation.AliasFor;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RequestMethod;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
            @RequestMapping(method = RequestMethod.GET)
            @interface ApiGet {
                @AliasFor(annotation = RequestMapping.class, attribute = "path")
                String[] path() default "/items";
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.METHOD)
            @RequestMapping(method = RequestMethod.GET)
            @interface ApiConv {
                String[] path() default "/conv";
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.METHOD)
            @ApiGet(path = "/lvl")
            @interface ApiGetV {
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ItemsController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("/api")
            public class ItemsController {
                @ApiGet
                public String bare() { return ""; }

                @ApiGet(path = "/v2")
                public String useSite() { return ""; }

                @GetMapping(path = "/a", produces = "text/plain")
                @PostMapping(path = "/b", consumes = "application/json")
                public String getThenPost() { return ""; }

                @PostMapping(path = "/b", consumes = "application/json")
                @GetMapping(path = "/a", produces = "text/plain")
                public String postThenGet() { return ""; }

                @RequestMapping("/d")
                @ApiGet
                public String directThenComposed() { return ""; }

                @ApiGet
                @RequestMapping("/d")
                public String composedThenDirect() { return ""; }

                @ApiGetV
                public String twoLevel() { return ""; }

                @GetMapping
                public String directNoPath() { return ""; }

                @GetMapping("/items")
                public String normalLiteralPath() { return ""; }

                @ApiConv
                public String convention() { return ""; }
            }
            """.trimIndent()
        )
    }
}
