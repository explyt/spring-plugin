/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.service.ComposedBeanNameFixture
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry

class ComposedBeanNameSpring7Test : ExplytJavaLightTestCase() {
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
        "@AliasFor(annotation = Bean.class, attribute = \"name\") String[] beanName() default {};",
        "@MyBean(beanName = \"x\")", "beanName", "name", listOf("x"), listOf("x")
    )

    fun testComposedNameAttributeWithoutAliasForIsIgnoredInSpring7() = assertComposedBeanName(
        "String[] name() default {};",
        "@MyBean(name = \"x\")", "name", null, listOf("x"), listOf("foo")
    )

    private fun assertComposedBeanName(
        attributeDeclaration: String,
        usage: String,
        attribute: String,
        aliasTarget: String?,
        declaredValues: List<String>,
        expectedNames: List<String>,
    ) {
        ComposedBeanNameFixture.assertSpringCoreMajorVersion(myFixture, module, 7)
        ComposedBeanNameFixture.addJava(myFixture, attributeDeclaration, usage)
        ComposedBeanNameFixture.assertComposedBeanNames(myFixture, module, attribute, aliasTarget, declaredValues, expectedNames)
    }
}
