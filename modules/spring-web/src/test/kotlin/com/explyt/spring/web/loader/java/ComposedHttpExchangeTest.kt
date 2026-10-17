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

class ComposedHttpExchangeTest : ComposedHttpExchangeJavaTestCase(6) {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary("org.springframework:spring-context:6.2.12"),
        TestLibrary("org.springframework:spring-web:6.2.12"),
    )

    fun testExchangesDifferingOnlyInHeadersServeNothing() {
        fixture.assertExchangeAttributeExists("headers")
        assertEquals(emptyList<ExchangeRow>(), fixture.rows(ITEMS, "headersDiffer"))
    }

    fun testEndpointActionsGutterSkipsExchangesDifferingOnlyInHeaders() =
        assertEquals(emptyList<ExchangeRow>(), fixture.gutterRows(ITEMS, "headersDiffer"))

    fun testExchangesDifferingOnlyInAcceptOrderServeNothing() =
        assertEquals(emptyList<ExchangeRow>(), fixture.rows(ITEMS, "acceptOrderDiffers"))

    fun testExchangesEqualIncludingHeadersServeOneRow() =
        assertEquals(listOf(ExchangeRow("/a", GET)), fixture.rows(ITEMS, "equalHeaders"))

    fun testBareComposedExchangeServesItsAliasDefaultUrl() =
        assertEquals(listOf(ExchangeRow("/items", GET)), fixture.rows(ITEMS, "bare"))

    fun testUrlSetAtUseSiteOverridesTheAliasDefault() =
        assertEquals(listOf(ExchangeRow("/v2", GET)), fixture.rows(ITEMS, "useSite"))

    fun testAliasDefaultReachesThroughTwoComposedLevels() =
        assertEquals(listOf(ExchangeRow("/lvl", GET)), fixture.rows(ITEMS, "twoLevel"))

    fun testComposedExchangeWithAcceptAliasServesItsAliasDefaultUrl() =
        assertEquals(listOf(ExchangeRow("/items", GET)), fixture.rows(ITEMS, "accept"))

    fun testConventionUrlOfComposedExchangeIsServedBeforeSpring7() =
        assertEquals(listOf(ExchangeRow("/conv", GET)), fixture.rows(ITEMS, "convention"))

    fun testConventionUrlSetAtUseSiteIsServedBeforeSpring7() =
        assertEquals(listOf(ExchangeRow("/convUse", GET)), fixture.rows(ITEMS, "conventionUseSite"))

    fun testGetExchangeFollowedByDifferentHttpExchangeServesNothing() =
        assertEquals(emptyList<ExchangeRow>(), fixture.rows(ITEMS, "getThenExchange"))

    fun testHttpExchangeFollowedByDifferentGetExchangeServesNothing() =
        assertEquals(emptyList<ExchangeRow>(), fixture.rows(ITEMS, "exchangeThenGet"))

    fun testTwoDifferentComposedExchangesServeNothing() =
        assertEquals(emptyList<ExchangeRow>(), fixture.rows(ITEMS, "twoComposed"))

    fun testExchangesDifferingOnlyInAcceptServeNothing() =
        assertEquals(emptyList<ExchangeRow>(), fixture.rows(ITEMS, "acceptDiffers"))

    fun testEqualExchangesServeOneRow() =
        assertEquals(listOf(ExchangeRow("/same", GET)), fixture.rows(ITEMS, "equalDuplicates"))

    fun testUnreadableUrlBesideAnEqualExchangeKeepsTheReadableRow() =
        assertEquals(listOf(ExchangeRow("/same", listOf("GET", "GET"))), fixture.rows(ITEMS, "unreadable"))

    fun testPlainExchangeBesideConflictingMethodsIsStillServed() =
        assertEquals(listOf(ExchangeRow("/plain", GET)), fixture.rows(ITEMS, "plain"))

    fun testComposedExchangeOnTheTypeIsTheClassPrefix() =
        assertEquals(listOf(ExchangeRow("/prefix/x", GET)), fixture.rows("PrefixClient", "x"))

    fun testDifferentExchangesOnTheTypeServeNoRowOfThatInterface() =
        assertEquals(emptyList<ExchangeRow>(), fixture.allRows("TypeConflictClient"))

    fun testEndpointActionsGutterReadsTheAliasDefaultUrl() =
        assertEquals(listOf(ExchangeRow("/items", GET)), fixture.gutterRows(ITEMS, "bare"))

    fun testEndpointActionsGutterSkipsConflictingExchanges() =
        assertEquals(emptyList<ExchangeRow>(), fixture.gutterRows(ITEMS, "getThenExchange"))
}

class ComposedHttpExchangeSpring7Test : ComposedHttpExchangeJavaTestCase(7) {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary("org.springframework:spring-context:7.0.9"),
        TestLibrary("org.springframework:spring-web:7.0.9"),
    )

    fun testExchangesDifferingOnlyInVersionServeNothing() {
        fixture.assertExchangeAttributeExists("version")
        assertEquals(emptyList<ExchangeRow>(), fixture.rows("VersionClient", "versionDiffers"))
    }

    fun testBareComposedExchangeServesItsAliasDefaultUrlInSpring7() =
        assertEquals(listOf(ExchangeRow("/items", GET)), fixture.rows(ITEMS, "bare"))

    fun testConventionUrlOfComposedExchangeIsIgnoredInSpring7() =
        assertEquals(listOf(ExchangeRow("/meta", GET)), fixture.rows(ITEMS, "convention"))

    fun testConventionUrlSetAtUseSiteIsIgnoredInSpring7() =
        assertEquals(listOf(ExchangeRow("/meta", GET)), fixture.rows(ITEMS, "conventionUseSite"))

    fun testGetExchangeFollowedByDifferentHttpExchangeServesNothingInSpring7() =
        assertEquals(emptyList<ExchangeRow>(), fixture.rows(ITEMS, "getThenExchange"))
}

abstract class ComposedHttpExchangeJavaTestCase(private val springMajor: Int) : ExplytJavaLightTestCase() {

    protected val fixture by lazy {
        object : ComposedHttpExchangeFixture(myFixture, module, springMajor) {
            override fun identifierOf(method: PsiMethod): PsiElement = method.nameIdentifier!!
        }
    }

    override fun setUp() {
        super.setUp()
        addSources()
    }

    private fun addSources() {
        if (springMajor == 7) {
            myFixture.addFileToProject(
                "com/example/VersionClient.java", """
                package com.example;
                import org.springframework.web.service.annotation.GetExchange;
                import org.springframework.web.service.annotation.HttpExchange;

                public interface VersionClient {
                    @GetExchange(url = "/a", version = "1")
                    @HttpExchange(url = "/a", method = "GET", version = "2")
                    String versionDiffers();
                }
                """.trimIndent()
            )
        }
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
            @Target(ElementType.METHOD)
            @HttpExchange(url = "/a", method = "GET", headers = "X=1")
            @interface ApiHeaders {
            }

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
            @Target(ElementType.TYPE)
            @HttpExchange
            @interface ApiType {
                @AliasFor(annotation = HttpExchange.class, attribute = "url")
                String url() default "/prefix";
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ItemClient.java", """
            package com.example;

            import org.springframework.web.service.annotation.GetExchange;
            import org.springframework.web.service.annotation.HttpExchange;

            public interface ItemClient {
                @ApiHeaders
                @HttpExchange(url = "/a", method = "GET")
                String headersDiffer();

                @GetExchange(url = "/a", accept = {"text/plain", "application/json"})
                @HttpExchange(url = "/a", method = "GET", accept = {"application/json", "text/plain"})
                String acceptOrderDiffers();

                @ApiHeaders
                @HttpExchange(url = "/a", method = "GET", headers = "X=1")
                String equalHeaders();

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

    protected companion object {
        const val ITEMS = "ItemClient"
        val GET = listOf("GET")
    }
}
