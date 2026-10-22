/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.ComposedHttpExchangeFixture
import com.explyt.spring.web.loader.ComposedHttpExchangeFixture.ExchangeRow
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class ComposedHttpExchangeBefore614Test : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary("org.springframework:spring-context:6.1.2"),
        TestLibrary("org.springframework:spring-web:6.1.2"),
    )

    private val fixture by lazy {
        object : ComposedHttpExchangeFixture(myFixture, module, 6) {
            override fun identifierOf(method: PsiMethod): PsiElement = method.nameIdentifier!!
        }
    }

    override fun setUp() {
        super.setUp()
        val scope = com.intellij.psi.search.GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        val versionClass = com.intellij.psi.JavaPsiFacade.getInstance(project)
            .findClass("org.springframework.core.SpringVersion", scope)
        assertNotNull("Precondition: spring-core resolves", versionClass)
        val jarName = versionClass!!.containingFile.virtualFile.path.substringBefore("!/").substringAfterLast('/')
        assertEquals("Precondition: Spring 6.1.2 is below the 6.1.4 conflict gate", "spring-core-6.1.2.jar", jarName)
        addSources()
        assertDifferentMetaDepths()
    }

    fun testDirectExchangeDeclaredAfterGetExchangeWins() =
        assertEquals(listOf(ExchangeRow("/b", listOf("POST"))), fixture.rows("ItemClient", "getThenExchange"))

    fun testDirectExchangeDeclaredBeforeGetExchangeWins() =
        assertEquals(listOf(ExchangeRow("/b", listOf("POST"))), fixture.rows("ItemClient", "exchangeThenGet"))

    fun testFirstDeclaredOfEquallyNearComposedExchangesWins() =
        assertEquals(listOf(ExchangeRow("/one", listOf("GET"))), fixture.rows("ItemClient", "twoComposed"))

    fun testDirectTypeExchangeSuppliesThePrefix() =
        assertEquals(
            listOf(ExchangeRow("/one/x", listOf("GET")), ExchangeRow("/one/y", listOf("GET"))),
            fixture.allRows("TypeConflictClient")
        )

    fun testGutterUsesDirectExchangeDeclaredAfterGetExchange() =
        assertEquals(listOf(ExchangeRow("/b", listOf("POST"))), fixture.gutterRows("ItemClient", "getThenExchange"))

    fun testGutterUsesDirectExchangeDeclaredBeforeGetExchange() =
        assertEquals(listOf(ExchangeRow("/b", listOf("POST"))), fixture.gutterRows("ItemClient", "exchangeThenGet"))

    fun testFirstDeclaredDeeperComposedExchangeWins() =
        assertEquals(listOf(ExchangeRow("/lvl", listOf("GET"))), fixture.rows("ItemClient", "deeperFirst"))

    fun testGutterUsesFirstDeclaredDeeperComposedExchange() =
        assertEquals(listOf(ExchangeRow("/lvl", listOf("GET"))), fixture.gutterRows("ItemClient", "deeperFirst"))

    fun testFirstDeclaredDeeperComposedTypeSuppliesThePrefix() =
        assertEquals(
            listOf(ExchangeRow("/deep/x", listOf("GET")), ExchangeRow("/deep/y", listOf("GET"))),
            fixture.allRows("DeeperFirstTypeClient")
        )

    private fun assertDifferentMetaDepths() {
        val httpExchange = "org.springframework.web.service.annotation.HttpExchange"
        val apiGetV = myFixture.findClass("com.example.ApiGetV")
        assertFalse("Precondition: ApiGetV does not directly carry HttpExchange", apiGetV.hasAnnotation(httpExchange))
        assertTrue("Precondition: ApiGetV carries ApiGet", apiGetV.hasAnnotation("com.example.ApiGet"))
        assertTrue("Precondition: ApiGet directly carries HttpExchange", myFixture.findClass("com.example.ApiGet").hasAnnotation(httpExchange))
        assertTrue("Precondition: ApiPost directly carries HttpExchange", myFixture.findClass("com.example.ApiPost").hasAnnotation(httpExchange))
        val apiTypeV = myFixture.findClass("com.example.ApiTypeV")
        assertFalse("Precondition: ApiTypeV does not directly carry HttpExchange", apiTypeV.hasAnnotation(httpExchange))
        assertTrue("Precondition: ApiTypeV carries ApiType", apiTypeV.hasAnnotation("com.example.ApiType"))
        assertTrue("Precondition: ApiType directly carries HttpExchange", myFixture.findClass("com.example.ApiType").hasAnnotation(httpExchange))
        assertTrue("Precondition: ApiOtherType directly carries HttpExchange", myFixture.findClass("com.example.ApiOtherType").hasAnnotation(httpExchange))
    }

    private fun addSources() {
        myFixture.addFileToProject(
            "com/example/DeeperFirstTypeClient.java", """
            package com.example;
            import org.springframework.web.service.annotation.GetExchange;

            @ApiTypeV
            @ApiOtherType
            public interface DeeperFirstTypeClient {
                @GetExchange("/x")
                String x();

                @GetExchange("/y")
                String y();
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ComposedExchanges.java", """
            package com.example;

            import org.springframework.core.annotation.AliasFor;
            import org.springframework.web.service.annotation.HttpExchange;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
            @HttpExchange(method = "GET")
            @interface ApiGet {
                @AliasFor(annotation = HttpExchange.class, attribute = "url")
                String url() default "/items";
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.METHOD)
            @HttpExchange(method = "POST")
            @interface ApiPost {
                @AliasFor(annotation = HttpExchange.class, attribute = "url")
                String url() default "/posts";
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.METHOD)
            @ApiGet(url = "/lvl")
            @interface ApiGetV {
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.METHOD)
            @HttpExchange(method = "GET")
            @interface ApiAccept {
                @AliasFor(annotation = HttpExchange.class, attribute = "url")
                String url() default "/items";

                @AliasFor(annotation = HttpExchange.class, attribute = "accept")
                String[] mediaType() default "text/plain";
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.METHOD)
            @HttpExchange(url = "/meta", method = "GET")
            @interface ApiConv {
                String url() default "/conv";
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
            @HttpExchange
            @interface ApiType {
                @AliasFor(annotation = HttpExchange.class, attribute = "url")
                String url() default "/prefix";
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.TYPE)
            @ApiType(url = "/deep")
            @interface ApiTypeV {
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.TYPE)
            @HttpExchange(url = "/shallow")
            @interface ApiOtherType {
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ItemClient.java", """
            package com.example;

            import org.springframework.web.service.annotation.GetExchange;
            import org.springframework.web.service.annotation.HttpExchange;

            public interface ItemClient {
                @ApiGetV
                @ApiPost
                String deeperFirst();

                @ApiGet
                String bare();

                @ApiGet(url = "/v2")
                String useSite();

                @ApiGetV
                String twoLevel();

                @ApiAccept
                String accept();

                @ApiConv
                String convention();

                @ApiConv(url = "/convUse")
                String conventionUseSite();

                @GetExchange("/a")
                @HttpExchange(url = "/b", method = "POST")
                String getThenExchange();

                @HttpExchange(url = "/b", method = "POST")
                @GetExchange("/a")
                String exchangeThenGet();

                @ApiGet(url = "/one")
                @ApiPost
                String twoComposed();

                @ApiAccept
                @GetExchange("/items")
                String acceptDiffers();

                @GetExchange("/same")
                @HttpExchange(url = "/same", method = "GET")
                String equalDuplicates();

                @GetExchange(PathHolder.UNRESOLVED)
                @HttpExchange(url = "/same", method = "GET")
                String unreadable();

                @GetExchange("/plain")
                String plain();
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/PrefixClient.java", """
            package com.example;

            import org.springframework.web.service.annotation.GetExchange;

            @ApiType
            public interface PrefixClient {
                @GetExchange("/x")
                String x();
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/TypeConflictClient.java", """
            package com.example;

            import org.springframework.web.service.annotation.GetExchange;
            import org.springframework.web.service.annotation.HttpExchange;

            @HttpExchange("/one")
            @ApiType
            public interface TypeConflictClient {
                @GetExchange("/x")
                String x();

                @GetExchange("/y")
                String y();
            }
            """.trimIndent()
        )
    }

}
