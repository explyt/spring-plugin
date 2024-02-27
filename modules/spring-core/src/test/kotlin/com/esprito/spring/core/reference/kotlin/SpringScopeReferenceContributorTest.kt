package com.esprito.spring.core.reference.kotlin

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElement

class SpringScopeReferenceContributorTest : EspritoKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testScopeVariantsValue() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.SCOPE}("<caret>")
            class SpringBean {}
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
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.SCOPE}(scopeName="<caret>")
            class SpringBean {}
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
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.SCOPE}("<caret>")
            class SpringBean {}
            """.trimIndent()
        )

        val ref = myFixture.getReferenceAtCaretPosition()

        assertEquals(
            setOf("singleton", "prototype", "tenant", "scopeNameConstant"),
            ref?.variants?.map { (it as LookupElement).lookupString }?.toSet(),
        )
    }

}