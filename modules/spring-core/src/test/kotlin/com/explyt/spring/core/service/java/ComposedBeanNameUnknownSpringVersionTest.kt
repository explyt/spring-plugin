/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.util.BeanAnnotationNames
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope

class ComposedBeanNameUnknownSpringVersionTest : ExplytJavaLightTestCase() {

    fun testExplicitAliasNamesTheBeanWhenSpringVersionIsUnknown() = assertBeanNames(
        "@AliasFor(annotation = Bean.class, attribute = \"name\") String[] beanName() default {};",
        "@MyBean(beanName = \"x\")",
        setOf("x"),
    )

    fun testConventionNameIsIgnoredWhenSpringVersionIsUnknown() = assertBeanNames(
        "String[] name() default {};",
        "@MyBean(name = \"x\")",
        null,
    )

    private fun assertBeanNames(attributeDeclaration: String, usage: String, expectedNames: Set<String>?) {
        myFixture.addFileToProject(
            "org/springframework/context/annotation/Bean.java",
            """
            package org.springframework.context.annotation;
            public @interface Bean {
                String[] value() default {};
                String[] name() default {};
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "org/springframework/core/annotation/AliasFor.java",
            """
            package org.springframework.core.annotation;
            public @interface AliasFor {
                String value() default "";
                String attribute() default "";
                Class<? extends java.lang.annotation.Annotation> annotation() default java.lang.annotation.Annotation.class;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "beanname/App.java",
            """
            package beanname;
            import org.springframework.context.annotation.Bean;
            import org.springframework.core.annotation.AliasFor;
            @Bean
            @interface MyBean {
                $attributeDeclaration
            }
            public class App {
                $usage
                public Object foo() { return new Object(); }
            }
            """.trimIndent()
        )
        assertNull("spring-core version is unknown", SpringBootUtil.getSpringCoreMajorVersion(module))
        val scope = GlobalSearchScope.projectScope(project)
        val composed = JavaPsiFacade.getInstance(project).findClass("beanname.MyBean", scope)!!
        assertEquals(SpringCoreClasses.BEAN, composed.getAnnotation(SpringCoreClasses.BEAN)?.resolveAnnotationType()?.qualifiedName)
        val factory = JavaPsiFacade.getInstance(project).findClass("beanname.App", scope)!!
            .findMethodsByName("foo", false).single()
        assertNotNull(factory.getAnnotation("beanname.MyBean"))

        assertEquals(expectedNames, BeanAnnotationNames.of(factory))
    }
}
