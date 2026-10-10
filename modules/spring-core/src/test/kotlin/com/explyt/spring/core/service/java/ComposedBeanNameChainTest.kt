/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.ComposedBeanNameFixture
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry

class ComposedBeanNameChainTest : ExplytJavaLightTestCase() {
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
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor(annotation = Bean.class, attribute = "name") String[] beanName() default {};
            @AliasFor(annotation = Bean.class, attribute = "name") String[] id() default {};
        }
        """,
        "@MyBean(id = \"x\")",
        mapOf(("beanname.MyBean" to "id") to listOf("x")),
        listOf("x"),
    )

    fun testAliasThroughIntermediateComposedAnnotation() = assertBeanNames(
        """
        @Bean
        @Retention(RetentionPolicy.RUNTIME)
        @interface OtherBean {
            @AliasFor(annotation = Bean.class, attribute = "name") String[] id() default {};
        }
        @OtherBean
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor(annotation = OtherBean.class, attribute = "id") String[] beanName() default {};
        }
        """,
        "@MyBean(beanName = \"x\")",
        mapOf(("beanname.MyBean" to "beanName") to listOf("x")),
        listOf("x"),
    )

    fun testIntermediateComposedAnnotationWithoutAliasFallsBackToMethodName() = assertBeanNames(
        """
        @Bean
        @Retention(RetentionPolicy.RUNTIME)
        @interface OtherBean {
            @AliasFor(annotation = Bean.class, attribute = "name") String[] id() default {};
        }
        @OtherBean
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            String[] beanName() default {};
        }
        """,
        "@MyBean(beanName = \"x\")",
        mapOf(("beanname.MyBean" to "beanName") to listOf("x")),
        listOf("foo"),
    )

    fun testMetaBeanNameOnComposedAnnotationWithoutAttributes() = assertBeanNames(
        """
        @Bean("meta")
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {}
        """,
        "@MyBean",
        mapOf(("beanname.MyBean" to "value") to emptyList()),
        listOf("meta"),
    )

    fun testDirectBeanDeclaredBeforeComposedBeanWins() = assertBeanNames(
        """
        @Bean
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor(annotation = Bean.class, attribute = "name") String[] beanName() default {};
        }
        """,
        "@Bean(\"direct\") @MyBean(beanName = \"composed\")",
        mapOf(
            (SpringCoreClasses.BEAN to "value") to listOf("direct"),
            ("beanname.MyBean" to "beanName") to listOf("composed"),
        ),
        listOf("direct"),
    )

    fun testComposedAliasWithConstantValue() = assertBeanNames(
        """
        @Bean
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor(annotation = Bean.class, attribute = "name") String[] beanName() default {};
        }
        interface Names {
            String NAME = "x";
        }
        """,
        "@MyBean(beanName = Names.NAME)",
        mapOf(("beanname.MyBean" to "beanName") to listOf("x")),
        listOf("x"),
    )

    fun testMirroredValueAttributeFollowsConventionNameInSpring6() = assertBeanNames(
        """
        @Bean
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor("name") String[] value() default {};
            @AliasFor("value") String[] name() default {};
        }
        """,
        "@MyBean(\"x\")",
        mapOf(("beanname.MyBean" to "value") to listOf("x")),
        listOf("x"),
    )

    fun testExplicitAliasWinsOverConventionNameInSpring6() = assertBeanNames(
        """
        @Bean
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor(annotation = Bean.class, attribute = "name") String[] beanName() default {};
            String[] name() default {};
        }
        """,
        "@MyBean(beanName = \"x\", name = \"y\")",
        mapOf(("beanname.MyBean" to "beanName") to listOf("x"), ("beanname.MyBean" to "name") to listOf("y")),
        listOf("x"),
    )

    fun testConventionNameIsIgnoredWhenAnExplicitAliasExistsInSpring6() = assertBeanNames(
        """
        @Bean
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor(annotation = Bean.class, attribute = "name") String[] beanName() default {};
            String[] name() default {};
        }
        """,
        "@MyBean(name = \"y\")",
        mapOf(("beanname.MyBean" to "name") to listOf("y")),
        listOf("foo"),
    )

    fun testMirroredNameAttributeFollowsConventionNameInSpring6() = assertBeanNames(
        """
        @Bean
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor("name") String[] value() default {};
            @AliasFor("value") String[] name() default {};
        }
        """,
        "@MyBean(name = \"x\")",
        mapOf(("beanname.MyBean" to "name") to listOf("x")),
        listOf("x"),
    )

    fun testEmptyExplicitAliasHidesMetaBeanName() = assertBeanNames(
        """
        @Bean("fixed")
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor(annotation = Bean.class, attribute = "name") String[] beanName() default {};
        }
        """,
        "@MyBean",
        mapOf(("beanname.MyBean" to "beanName") to emptyList()),
        listOf("foo"),
    )

    fun testExplicitAliasOverridesMetaBeanName() = assertBeanNames(
        """
        @Bean("fixed")
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor(annotation = Bean.class, attribute = "name") String[] beanName() default {};
        }
        """,
        "@MyBean(beanName = \"x\")",
        mapOf(("beanname.MyBean" to "beanName") to listOf("x")),
        listOf("x"),
    )

    fun testConventionNameIsIgnoredWhenAnExplicitValueAliasExistsInSpring6() = assertBeanNames(
        """
        @Bean
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor(annotation = Bean.class, attribute = "value") String[] beanName() default {};
            String[] name() default {};
        }
        """,
        "@MyBean(name = \"y\")",
        mapOf(("beanname.MyBean" to "name") to listOf("y")),
        listOf("foo"),
    )

    fun testExplicitValueAliasWinsOverConventionNameInSpring6() = assertBeanNames(
        """
        @Bean
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor(annotation = Bean.class, attribute = "value") String[] beanName() default {};
            String[] name() default {};
        }
        """,
        "@MyBean(beanName = \"x\", name = \"y\")",
        mapOf(("beanname.MyBean" to "beanName") to listOf("x"), ("beanname.MyBean" to "name") to listOf("y")),
        listOf("x"),
    )

    private fun assertBeanNames(
        declarations: String,
        usage: String,
        annotationValues: Map<Pair<String, String>, List<String>>,
        expectedNames: List<String>,
    ) {
        ComposedBeanNameFixture.assertSpringCoreMajorVersion(myFixture, module, 6)
        ComposedBeanNameFixture.addJavaApplication(myFixture, declarations, usage)
        ComposedBeanNameFixture.assertFactoryBeanNames(myFixture, module, annotationValues, expectedNames)
    }
}
