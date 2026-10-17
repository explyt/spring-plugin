/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.ComposedHttpExchangeFixture
import com.explyt.spring.web.loader.ComposedHttpExchangeFixture.ExchangeRow
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtNamedFunction

class ComposedHttpExchangeTest : ComposedHttpExchangeKotlinTestCase(6) {

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

class ComposedHttpExchangeSpring7Test : ComposedHttpExchangeKotlinTestCase(7) {

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

abstract class ComposedHttpExchangeKotlinTestCase(private val springMajor: Int) : ExplytKotlinLightTestCase() {

    protected val fixture by lazy {
        object : ComposedHttpExchangeFixture(myFixture, module, springMajor) {
            override fun identifierOf(method: PsiMethod): PsiElement =
                (method.navigationElement as KtNamedFunction).nameIdentifier!!
        }
    }

    override fun setUp() {
        super.setUp()
        addSources()
    }

    private fun addSources() {
        if (springMajor == 7) {
            myFixture.addFileToProject(
                "com/example/VersionClient.kt", """
                package com.example
                import org.springframework.web.service.annotation.GetExchange
                import org.springframework.web.service.annotation.HttpExchange

                interface VersionClient {
                    @GetExchange(url = "/a", version = "1")
                    @HttpExchange(url = "/a", method = "GET", version = "2")
                    fun versionDiffers(): String
                }
                """.trimIndent()
            )
        }
        myFixture.addFileToProject(
            "com/example/ComposedExchanges.kt", """
            package com.example

            import org.springframework.core.annotation.AliasFor
            import org.springframework.web.service.annotation.HttpExchange

            @Retention(AnnotationRetention.RUNTIME)
            @Target(AnnotationTarget.FUNCTION)
            @HttpExchange(url = "/a", method = "GET", headers = ["X=1"])
            annotation class ApiHeaders

            @Retention(AnnotationRetention.RUNTIME)
            @Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
            @HttpExchange(method = "GET")
            annotation class ApiGet(
                @get:AliasFor(annotation = HttpExchange::class, attribute = "url")
                val url: String = "/items",
            )

            @Retention(AnnotationRetention.RUNTIME)
            @Target(AnnotationTarget.FUNCTION)
            @HttpExchange(method = "POST")
            annotation class ApiPost(
                @get:AliasFor(annotation = HttpExchange::class, attribute = "url")
                val url: String = "/posts",
            )

            @Retention(AnnotationRetention.RUNTIME)
            @Target(AnnotationTarget.FUNCTION)
            @ApiGet(url = "/lvl")
            annotation class ApiGetV

            @Retention(AnnotationRetention.RUNTIME)
            @Target(AnnotationTarget.FUNCTION)
            @HttpExchange(method = "GET")
            annotation class ApiAccept(
                @get:AliasFor(annotation = HttpExchange::class, attribute = "url")
                val url: String = "/items",
                @get:AliasFor(annotation = HttpExchange::class, attribute = "accept")
                val mediaType: Array<String> = ["text/plain"],
            )

            @Retention(AnnotationRetention.RUNTIME)
            @Target(AnnotationTarget.FUNCTION)
            @HttpExchange(url = "/meta", method = "GET")
            annotation class ApiConv(val url: String = "/conv")

            @Retention(AnnotationRetention.RUNTIME)
            @Target(AnnotationTarget.CLASS)
            @HttpExchange
            annotation class ApiType(
                @get:AliasFor(annotation = HttpExchange::class, attribute = "url")
                val url: String = "/prefix",
            )
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ItemClient.kt", """
            package com.example

            import org.springframework.web.service.annotation.GetExchange
            import org.springframework.web.service.annotation.HttpExchange

            interface ItemClient {
                @ApiHeaders
                @HttpExchange(url = "/a", method = "GET")
                fun headersDiffer(): String

                @GetExchange(url = "/a", accept = ["text/plain", "application/json"])
                @HttpExchange(url = "/a", method = "GET", accept = ["application/json", "text/plain"])
                fun acceptOrderDiffers(): String

                @ApiHeaders
                @HttpExchange(url = "/a", method = "GET", headers = ["X=1"])
                fun equalHeaders(): String

                @ApiGet
                fun bare(): String

                @ApiGet(url = "/v2")
                fun useSite(): String

                @ApiGetV
                fun twoLevel(): String

                @ApiAccept
                fun accept(): String

                @ApiConv
                fun convention(): String

                @ApiConv(url = "/convUse")
                fun conventionUseSite(): String

                @GetExchange("/a")
                @HttpExchange(url = "/b", method = "POST")
                fun getThenExchange(): String

                @HttpExchange(url = "/b", method = "POST")
                @GetExchange("/a")
                fun exchangeThenGet(): String

                @ApiGet(url = "/one")
                @ApiPost
                fun twoComposed(): String

                @ApiAccept
                @GetExchange("/items")
                fun acceptDiffers(): String

                @GetExchange("/same")
                @HttpExchange(url = "/same", method = "GET")
                fun equalDuplicates(): String

                @GetExchange(PathHolder.UNRESOLVED)
                @HttpExchange(url = "/same", method = "GET")
                fun unreadable(): String

                @GetExchange("/plain")
                fun plain(): String
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/PrefixClient.kt", """
            package com.example

            import org.springframework.web.service.annotation.GetExchange

            @ApiType
            interface PrefixClient {
                @GetExchange("/x")
                fun x(): String
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/TypeConflictClient.kt", """
            package com.example

            import org.springframework.web.service.annotation.GetExchange
            import org.springframework.web.service.annotation.HttpExchange

            @HttpExchange("/one")
            @ApiType
            interface TypeConflictClient {
                @GetExchange("/x")
                fun x(): String

                @GetExchange("/y")
                fun y(): String
            }
            """.trimIndent()
        )
    }

    protected companion object {
        const val ITEMS = "ItemClient"
        val GET = listOf("GET")
    }
}
