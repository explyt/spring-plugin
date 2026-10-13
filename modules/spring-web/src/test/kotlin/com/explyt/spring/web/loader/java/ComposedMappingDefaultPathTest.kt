/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.HandlerMethods
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

class ComposedMappingDefaultPathTest : ComposedControllerPrefixJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testAliasDefaultPathOfComposedMappingIsServedOnPlainUse() {
        addController("@ReportMapping")

        val endpoint = reportEndpoint()

        assertEquals("/reports", endpoint.path)
        assertEquals(listOf("GET"), endpoint.requestMethods)
    }

    fun testPathSetAtUseSiteOverridesTheAliasDefault() {
        addController("@ReportMapping(path = \"/custom\")")

        val endpoint = reportEndpoint()

        assertEquals("/custom", endpoint.path)
        assertEquals(listOf("GET"), endpoint.requestMethods)
    }

    fun testAliasDefaultPrefixOfComposedControllerIsServedOnPlainUse() =
        assertItemsPaths(ALIAS_TO_PATH, "@ApiController", listOf("/api/items"))

    fun testPrefixSetAtUseSiteOverridesTheControllerAliasDefault() =
        assertItemsPaths(ALIAS_TO_PATH, "@ApiController(path = \"/v2\")", listOf("/v2/items"))

    fun testTwoPrefixesSetAtUseSiteServeTheHandlerUnderEach() =
        assertItemsPaths(ALIAS_TO_PATH, "@ApiController(path = {\"/x\", \"/y\"})", listOf("/x/items", "/y/items"))

    fun testAliasDefaultOfValueAttributeIsTheControllerPrefix() =
        assertItemsPaths(ALIAS_TO_VALUE, "@ApiController", listOf("/dval/items"))

    fun testAliasDefaultReachesThroughTwoComposedLevels() =
        assertItemsPaths(ALIAS_TO_PATH, "@ApiNested", listOf("/api/items"))

    fun testPrefixFixedOnTheMetaAnnotationIsTheControllerPrefix() =
        assertItemsPaths(FIXED_ON_META, "@ApiController", listOf("/fixed/items"))

    fun testPlainRequestMappingPrefixBesideComposedAnnotation() =
        assertItemsPaths(ALIAS_TO_PATH, "@RestController @RequestMapping(\"/p\")", listOf("/p/items"))

    fun testConventionPathDefaultIsTheControllerPrefixBeforeSpring7() =
        assertItemsPaths(CONVENTION_PATH, "@ApiController", listOf("/conv/items"))

    fun testHandlerMethodsReadTheAliasDefaultPrefixOfComposedController() {
        addItemsController(ALIAS_TO_PATH, "@ApiController")
        assertFalse("Precondition: the loader finds the controller", itemsPaths().isEmpty())

        val prefixes = HandlerMethods.requestMappingPrefixes(
            itemsController(), MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        )

        assertEquals(listOf("/api"), prefixes)
    }

    fun testDirectRequestMappingPrefixWinsOverComposedControllerPrefix() =
        assertOnlyDirectPrefixIsServed("@ApiController @RequestMapping(\"/p\")")

    fun testDirectRequestMappingPrefixDeclaredFirstWinsOverComposedControllerPrefix() =
        assertOnlyDirectPrefixIsServed("@RequestMapping(\"/p\") @ApiController")

    fun testHandlerMethodsReadOnlyTheDirectPrefixBesideComposedController() =
        assertHandlerMethodsReadOnlyTheDirectPrefix("@ApiController @RequestMapping(\"/p\")")

    fun testHandlerMethodsReadOnlyTheDirectPrefixDeclaredFirstBesideComposedController() =
        assertHandlerMethodsReadOnlyTheDirectPrefix("@RequestMapping(\"/p\") @ApiController")

    private fun assertOnlyDirectPrefixIsServed(usage: String) {
        addItemsController(ALIAS_TO_PATH, usage)
        assertControllerCarriesDirectAndComposedMapping()

        assertEquals(listOf("/p/items"), itemsPaths())
    }

    private fun assertHandlerMethodsReadOnlyTheDirectPrefix(usage: String) {
        addItemsController(ALIAS_TO_PATH, usage)
        assertControllerCarriesDirectAndComposedMapping()
        assertFalse("Precondition: the loader finds the controller", itemsPaths().isEmpty())

        val prefixes = HandlerMethods.requestMappingPrefixes(
            itemsController(), MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        )

        assertEquals(listOf("/p"), prefixes)
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

    private fun addController(usage: String) {
        myFixture.addFileToProject(
            "com/example/ReportController.java", """
            package com.example;

            import org.springframework.core.annotation.AliasFor;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RequestMethod;
            import org.springframework.web.bind.annotation.RestController;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.METHOD)
            @Retention(RetentionPolicy.RUNTIME)
            @RequestMapping(method = RequestMethod.GET)
            @interface ReportMapping {
                @AliasFor(annotation = RequestMapping.class, attribute = "path")
                String[] path() default "/reports";
            }

            @RestController
            public class ReportController {
                $usage
                public String report() {
                    return "report";
                }
            }
            """.trimIndent()
        )
    }

    private fun reportEndpoint(): EndpointElement {
        val mapping = JavaPsiFacade.getInstance(project)
            .findClass("com.example.ReportMapping", GlobalSearchScope.projectScope(project))
        assertNotNull("Precondition: the composed mapping is in the fixture", mapping)
        assertEquals(
            "Precondition: the composed mapping is meta-annotated with @RequestMapping",
            SpringWebClasses.REQUEST_MAPPING,
            mapping!!.getAnnotation(SpringWebClasses.REQUEST_MAPPING)?.resolveAnnotationType()?.qualifiedName
        )
        return SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.type == EndpointType.SPRING_MVC && it.containingClass?.name == "ReportController" }
            .single { (it.psiElement as PsiMethod).name == "report" }
    }
}

class ComposedControllerPrefixSpring7Test : ComposedControllerPrefixJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary("org.springframework:spring-webmvc:7.0.9", true))

    fun testConventionPathDefaultIsIgnoredInSpring7() =
        assertItemsPaths(CONVENTION_PATH, "@ApiController", listOf("/items"))

    fun testAliasDefaultPrefixOfComposedControllerIsServedInSpring7() =
        assertItemsPaths(ALIAS_TO_PATH, "@ApiController", listOf("/api/items"))
}

abstract class ComposedControllerPrefixJavaTestCase : ExplytJavaLightTestCase() {

    protected class Composition(val metaMapping: String, val members: String)

    protected fun assertItemsPaths(composition: Composition, usage: String, expected: List<String>) {
        addItemsController(composition, usage)

        assertEquals(expected, itemsPaths())
    }

    protected fun addItemsController(composition: Composition, usage: String) {
        myFixture.addFileToProject(
            "com/example/ApiController.java", """
            package com.example;

            import org.springframework.core.annotation.AliasFor;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            @RestController
            ${composition.metaMapping}
            public @interface ApiController {
                ${composition.members}
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ApiNested.java", """
            package com.example;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            @ApiController
            public @interface ApiNested {
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ItemsController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            $usage
            public class ItemsController {
                @GetMapping("/items")
                public String items() {
                    return "items";
                }
            }
            """.trimIndent()
        )
    }

    protected fun itemsController(): PsiClass {
        val facade = JavaPsiFacade.getInstance(project)
        assertNotNull(
            "Precondition: @RequestMapping resolves",
            facade.findClass(SpringWebClasses.REQUEST_MAPPING, GlobalSearchScope.allScope(project))
        )
        val composed = facade.findClass("com.example.ApiController", GlobalSearchScope.projectScope(project))
        assertNotNull("Precondition: the composed controller annotation is in the fixture", composed)
        assertTrue(
            "Precondition: the composed controller annotation is meta-annotated with @RequestMapping",
            composed!!.hasAnnotation(SpringWebClasses.REQUEST_MAPPING)
        )
        val controller = facade.findClass("com.example.ItemsController", GlobalSearchScope.projectScope(project))
        assertNotNull("Precondition: the controller is in the fixture", controller)
        return controller!!
    }

    protected fun itemsPaths(): List<String> {
        itemsController()
        val paths = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.type == EndpointType.SPRING_MVC && it.containingClass?.name == "ItemsController" }
            .map { it.path }
            .sorted()
        assertFalse("Precondition: the loader finds the controller", paths.isEmpty())
        return paths
    }

    protected companion object {
        val ALIAS_TO_PATH = Composition(
            "@RequestMapping",
            "@AliasFor(annotation = RequestMapping.class, attribute = \"path\") String[] path() default \"/api\";"
        )
        val ALIAS_TO_VALUE = Composition(
            "@RequestMapping",
            "@AliasFor(annotation = RequestMapping.class, attribute = \"value\") String[] value() default \"/dval\";"
        )
        val CONVENTION_PATH = Composition("@RequestMapping", "String[] path() default \"/conv\";")
        val FIXED_ON_META = Composition("@RequestMapping(\"/fixed\")", "String name() default \"\";")
    }
}
