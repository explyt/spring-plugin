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

class ComposedHttpExchangeBefore614Test : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary("org.springframework:spring-context:6.1.2"),
        TestLibrary("org.springframework:spring-web:6.1.2"),
    )

    private val fixture by lazy {
        object : ComposedHttpExchangeFixture(myFixture, module, 6) {
            override fun identifierOf(method: PsiMethod): PsiElement = (method.navigationElement as KtNamedFunction).nameIdentifier!!
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
            "com/example/DeeperFirstTypeClient.kt", """
            package com.example
            import org.springframework.web.service.annotation.GetExchange

            @ApiTypeV
            @ApiOtherType
            interface DeeperFirstTypeClient {
                @GetExchange("/x")
                fun x(): String

                @GetExchange("/y")
                fun y(): String
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ComposedExchanges.kt", """
            package com.example

            import org.springframework.core.annotation.AliasFor
            import org.springframework.web.service.annotation.HttpExchange

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
            @Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
            @HttpExchange
            annotation class ApiType(
                @get:AliasFor(annotation = HttpExchange::class, attribute = "url")
                val url: String = "/prefix",
            )

            @Retention(AnnotationRetention.RUNTIME)
            @Target(AnnotationTarget.CLASS)
            @ApiType(url = "/deep")
            annotation class ApiTypeV

            @Retention(AnnotationRetention.RUNTIME)
            @Target(AnnotationTarget.CLASS)
            @HttpExchange(url = "/shallow")
            annotation class ApiOtherType
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ItemClient.kt", """
            package com.example

            import org.springframework.web.service.annotation.GetExchange
            import org.springframework.web.service.annotation.HttpExchange

            interface ItemClient {
                @ApiGetV
                @ApiPost
                fun deeperFirst(): String

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

}
