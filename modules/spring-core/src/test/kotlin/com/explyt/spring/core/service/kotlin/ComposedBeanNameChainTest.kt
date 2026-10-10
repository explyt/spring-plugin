/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.ComposedBeanNameFixture
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry

class ComposedBeanNameChainTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        super.tearDown()
        Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
    }

    fun testImplicitAliasesForTheSameBeanAttributeNameTheBean() = assertBeanNames(
        """
        @Bean
        annotation class MyBean(
            @get:AliasFor(annotation = Bean::class, attribute = "name") val beanName: Array<String> = [],
            @get:AliasFor(annotation = Bean::class, attribute = "name") val id: Array<String> = [],
        )
        """,
        "@MyBean(id = [\"x\"])",
        mapOf(("beanname.MyBean" to "id") to listOf("x")),
        listOf("x"),
    )

    fun testAliasThroughIntermediateComposedAnnotation() = assertBeanNames(
        """
        @Bean
        annotation class OtherBean(
            @get:AliasFor(annotation = Bean::class, attribute = "name") val id: Array<String> = [],
        )
        @OtherBean
        annotation class MyBean(
            @get:AliasFor(annotation = OtherBean::class, attribute = "id") val beanName: Array<String> = [],
        )
        """,
        "@MyBean(beanName = [\"x\"])",
        mapOf(("beanname.MyBean" to "beanName") to listOf("x")),
        listOf("x"),
    )

    fun testIntermediateComposedAnnotationWithoutAliasFallsBackToMethodName() = assertBeanNames(
        """
        @Bean
        annotation class OtherBean(
            @get:AliasFor(annotation = Bean::class, attribute = "name") val id: Array<String> = [],
        )
        @OtherBean
        annotation class MyBean(val beanName: Array<String> = [])
        """,
        "@MyBean(beanName = [\"x\"])",
        mapOf(("beanname.MyBean" to "beanName") to listOf("x")),
        listOf("foo"),
    )

    fun testMetaBeanNameOnComposedAnnotationWithoutAttributes() = assertBeanNames(
        """
        @Bean("meta")
        annotation class MyBean
        """,
        "@MyBean",
        mapOf(("beanname.MyBean" to "value") to emptyList()),
        listOf("meta"),
    )

    fun testDirectBeanDeclaredBeforeComposedBeanWins() = assertBeanNames(
        """
        @Bean
        annotation class MyBean(
            @get:AliasFor(annotation = Bean::class, attribute = "name") val beanName: Array<String> = [],
        )
        """,
        "@Bean(\"direct\") @MyBean(beanName = [\"composed\"])",
        mapOf(
            (SpringCoreClasses.BEAN to "value") to listOf("direct"),
            ("beanname.MyBean" to "beanName") to listOf("composed"),
        ),
        listOf("direct"),
    )

    fun testComposedAliasWithConstantValue() = assertBeanNames(
        """
        @Bean
        annotation class MyBean(
            @get:AliasFor(annotation = Bean::class, attribute = "name") val beanName: Array<String> = [],
        )
        const val NAME = "x"
        """,
        "@MyBean(beanName = [NAME])",
        mapOf(("beanname.MyBean" to "beanName") to listOf("x")),
        listOf("x"),
    )

    private fun assertBeanNames(
        declarations: String,
        usage: String,
        annotationValues: Map<Pair<String, String>, List<String>>,
        expectedNames: List<String>,
    ) {
        ComposedBeanNameFixture.assertSpringCoreMajorVersion(myFixture, module, 6)
        ComposedBeanNameFixture.addKotlinApplication(myFixture, declarations, usage)
        ComposedBeanNameFixture.assertFactoryBeanNames(myFixture, module, annotationValues, expectedNames)
    }
}
