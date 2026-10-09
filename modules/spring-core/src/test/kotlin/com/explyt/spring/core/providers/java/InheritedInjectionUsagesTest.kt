/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil

class InheritedInjectionUsagesTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    fun testAnnotatedAbstractBaseFieldIsListedOnce() {
        assertUsages(
            """
            @Component abstract class Base { @Autowired Foo foo; }
            @Component class Impl extends Base {}
            @Component class Foo {}
            """.trimIndent(), "foo", 1
        )
    }

    fun testUnannotatedAbstractBaseFieldIsListed() {
        assertUsages(
            """
            abstract class Base { @Autowired Foo foo; }
            @Component class Impl extends Base {}
            @Component class Foo {}
            """.trimIndent(), "foo", 1
        )
    }

    fun testAutowiredConstructorInAbstractBaseIsNotInjectionPoint() {
        val targets = targets(
            """
            abstract class Base { @Autowired public Base(Foo foo) {} }
            @Component class Impl extends Base { @Autowired Foo other; Impl() { super(null); } }
            @Component class Foo {}
            """.trimIndent()
        )
        assertEquals("Impl's own field must be listed: $targets", 1, targets.count { it == "other" })
        assertEquals("Base constructor parameter must not be listed: $targets", 0, targets.count { it == "foo" })
    }

    fun testAutowiredSetterInAbstractBaseIsListed() {
        assertUsages(
            """
            abstract class Base { @Autowired void setFoo(Foo foo) {} }
            @Component class Impl extends Base {}
            @Component class Foo {}
            """.trimIndent(), "foo", 1
        )
    }

    fun testBeanMethodParameterInUnannotatedAbstractConfigurationIsListedOnce() {
        assertUsages(
            """
            abstract class BaseConfig {
                @org.springframework.context.annotation.Bean
                public Bar bar(Foo foo) { return new Bar(); }
            }
            @org.springframework.context.annotation.Configuration class AppConfig extends BaseConfig {}
            class Bar {}
            @Component class Foo {}
            """.trimIndent(), "foo", 1, listOf("AppConfig")
        )
    }

    fun testTwoConcreteSubclassesDoNotDuplicateBaseInjection() {
        assertUsages(
            """
            abstract class Base { @Autowired Foo foo; }
            @Component class First extends Base {}
            @Component class Second extends Base {}
            @Component class Foo {}
            """.trimIndent(), "foo", 1, listOf("First", "Second")
        )
    }

    fun testUnrelatedNonBeanInjectionIsNotListed() {
        val targets = targets(
            """
            class Unrelated { @Autowired Foo unrelated; }
            @Component class User { @Autowired Foo used; }
            @Component class Foo {}
            """.trimIndent()
        )
        assertTrue("The bean's injection point must be listed: $targets", targets.contains("used"))
        assertFalse(targets.any { it.contains("unrelated") })
    }

    private fun assertUsages(source: String, target: String, count: Int, owners: List<String> = listOf("Impl")) {
        val targets = targets(source)
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        owners.forEach { owner ->
            assertTrue("candidates.$owner must be an active bean", beans.any { it.psiClass.qualifiedName == "candidates.$owner" })
        }
        assertEquals(count, targets.count { it == target })
    }

    private fun targets(source: String): List<String> {
        myFixture.configureByText(
            "Candidates.java",
            "package candidates;\nimport org.springframework.stereotype.Component;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" + source
        )
        myFixture.doHighlighting()
        return SpringGutterTestUtil.getGutterTargetString(
            SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        ).flatten()
    }
}
