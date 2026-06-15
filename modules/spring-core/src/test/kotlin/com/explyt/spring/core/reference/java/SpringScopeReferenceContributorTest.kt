/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.reference.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElement

class SpringScopeReferenceContributorTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testScopeVariantsValue() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.SCOPE}("<caret>")
            public class SpringBean {}
            """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition()

        assertEquals(
            setOf("singleton", "prototype"),
            ref?.variants?.map { (it as LookupElement).lookupString }?.toSet(),
        )
    }

    fun testScopeVariantsScopeName() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.SCOPE}(scopeName="<caret>")
            public class SpringBean {}
            """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition()

        assertEquals(
            setOf("singleton", "prototype"),
            ref?.variants?.map { (it as LookupElement).lookupString }?.toSet(),
        )
    }

    fun testScopeVariantsCustomScope() {
        myFixture.addClass(
            """
            import org.springframework.beans.BeansException;
            import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
            import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class TenantBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
                public static final String SCOPE_NAME="scopeNameConstant";
            
                @Override
                public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
                    TenantScope tenantScope = new TenantScope();
                    factory.registerScope("tenant", tenantScope);
                    factory.registerScope(SCOPE_NAME, tenantScope);
                }
            }
        """.trimIndent()
        )
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.SCOPE}("<caret>")
            public class SpringBean {}
            """.trimIndent()
        )
        val ref = myFixture.getReferenceAtCaretPosition()

        assertEquals(
            setOf("singleton", "prototype", "tenant", "scopeNameConstant"),
            ref?.variants?.map { (it as LookupElement).lookupString }?.toSet(),
        )
    }

}