/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry
import java.util.Locale

/**
 * Kotlin twin of [DefaultBeanNameJavaTest]: a Kotlin nested class is static and compiles to the binary name
 * `Outer$Inner`, so Spring names it exactly as the Java static nested class.
 */
class DefaultBeanNameKotlinTest : ExplytKotlinLightTestCase() {
    override val libraries = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false, testRootDisposable)
        myFixture.addFileToProject(
            "com/explyt/demo/App.kt",
            "package com.explyt.demo\n@org.springframework.boot.autoconfigure.SpringBootApplication class App"
        )
    }

    fun testTwoLeadingCapitalsKeepTheClassName() {
        addComponent("ABTestController")

        assertEquals("ABTestController", beanNameOf("com.explyt.demo.ABTestController"))
    }

    fun testAcronymPrefixKeepsTheClassName() {
        addComponent("URLService")

        assertEquals("URLService", beanNameOf("com.explyt.demo.URLService"))
    }

    fun testNestedComponentIsNamedAfterItsOuterQualifiedShortName() {
        myFixture.addFileToProject(
            "com/explyt/demo/Outer.kt",
            """
            package com.explyt.demo
            class Outer {
                @org.springframework.stereotype.Component
                class Inner
            }
            """.trimIndent()
        )

        assertEquals("outer.Inner", beanNameOf("com.explyt.demo.Outer.Inner"))
    }

    fun testLowercasingIgnoresTheDefaultLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"))
            addComponent("ItemService")

            assertEquals("itemService", beanNameOf("com.explyt.demo.ItemService"))
        } finally {
            Locale.setDefault(original)
        }
    }

    fun testOrdinaryClassNameIsDecapitalized() {
        addComponent("Foo")

        assertEquals("foo", beanNameOf("com.explyt.demo.Foo"))
    }

    fun testSingleCharacterClassNameIsLowercased() {
        addComponent("A")

        assertEquals("a", beanNameOf("com.explyt.demo.A"))
    }

    fun testExplicitComponentNameWins() {
        myFixture.addFileToProject(
            "com/explyt/demo/Named.kt",
            "package com.explyt.demo\n@org.springframework.stereotype.Component(\"custom\") class Named"
        )

        assertEquals("custom", beanNameOf("com.explyt.demo.Named"))
    }

    fun testBeanFunctionKeepsItsName() {
        myFixture.addFileToProject(
            "com/explyt/demo/GadgetConfig.kt",
            """
            package com.explyt.demo
            class Gadget
            @org.springframework.context.annotation.Configuration
            class GadgetConfig {
                @org.springframework.context.annotation.Bean
                fun URLGadget(): Gadget = Gadget()
            }
            """.trimIndent()
        )

        val names = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .filter { it.psiClass.qualifiedName == "com.explyt.demo.Gadget" }
            .map { it.name }
        assertEquals(listOf("URLGadget"), names)
    }

    private fun addComponent(className: String) {
        myFixture.addFileToProject(
            "com/explyt/demo/$className.kt",
            "package com.explyt.demo\n@org.springframework.stereotype.Component class $className"
        )
    }

    private fun beanNameOf(qualifiedName: String): String {
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .filter { it.psiClass.qualifiedName == qualifiedName && !it.isMember() }
        assertEquals(
            "Precondition: $qualifiedName is one active component bean, got ${beans.map { it.name }}",
            1,
            beans.size
        )
        return beans.single().name
    }
}
