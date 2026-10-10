/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.kotlin

import com.explyt.spring.core.service.ComposedBeanNameFixture
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry

class ComposedBeanNameSpring7Test : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary("org.springframework:spring-context:7.0.9"),
        TestLibrary.springBootAutoConfigure_4_1_0,
    )

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        super.tearDown()
        Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
    }

    fun testComposedBeanNameAliasInSpring7() = assertComposedBeanName(
        "@get:AliasFor(annotation = Bean::class, attribute = \"name\") val beanName: Array<String> = []",
        "@MyBean(beanName = [\"x\"])", "beanName", "name", listOf("x"), listOf("x")
    )

    fun testComposedNameAttributeWithoutAliasForIsIgnoredInSpring7() = assertComposedBeanName(
        "val name: Array<String> = []",
        "@MyBean(name = [\"x\"])", "name", null, listOf("x"), listOf("foo")
    )

    fun testMirroredValueAttributeIsIgnoredInSpring7() = assertMirroredBeanName("@MyBean(\"x\")", "value")

    fun testMirroredNameAttributeIsIgnoredInSpring7() = assertMirroredBeanName("@MyBean(name = [\"x\"])", "name")

    private fun assertMirroredBeanName(usage: String, attribute: String) {
        ComposedBeanNameFixture.assertSpringCoreMajorVersion(myFixture, module, 7)
        ComposedBeanNameFixture.addKotlinApplication(
            myFixture,
            """
            @Bean
            annotation class MyBean(
                @get:AliasFor("name") val value: Array<String> = [],
                @get:AliasFor("value") val name: Array<String> = [],
            )
            """,
            usage,
        )
        ComposedBeanNameFixture.assertFactoryBeanNames(
            myFixture, module, mapOf(("beanname.MyBean" to attribute) to listOf("x")), listOf("foo")
        )
    }

    private fun assertComposedBeanName(
        attributeDeclaration: String,
        usage: String,
        attribute: String,
        aliasTarget: String?,
        declaredValues: List<String>,
        expectedNames: List<String>,
    ) {
        ComposedBeanNameFixture.assertSpringCoreMajorVersion(myFixture, module, 7)
        ComposedBeanNameFixture.addKotlin(myFixture, attributeDeclaration, usage)
        ComposedBeanNameFixture.assertComposedBeanNames(myFixture, module, attribute, aliasTarget, declaredValues, expectedNames)
    }
}
