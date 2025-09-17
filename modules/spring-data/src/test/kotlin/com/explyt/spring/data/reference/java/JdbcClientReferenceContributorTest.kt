/*
 * Copyright © 2025 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.data.reference.java

import com.explyt.jpa.ql.JpqlLanguage
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiPolyVariantReference
import org.intellij.lang.annotations.Language

class JdbcClientReferenceContributorTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springJdbc_6_2_5
    )

    override fun setUp() {
        super.setUp()
        SpringToolRunConfigurationsSettingsState.getInstance().sqlLanguageId = JpqlLanguage.INSTANCE.id
    }

    override fun tearDown() {
        super.tearDown()
        SpringToolRunConfigurationsSettingsState.getInstance().sqlLanguageId = ""
    }

    fun testReferenceSimpleSql() {
        @Language("JAVA") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient;
        import org.springframework.stereotype.Component;
        import org.springframework.beans.factory.annotation.Autowired;
        
        @Component
        public class TestComponent {
            @Autowired JdbcClient jdbcClient;
            
            public void method() {
                jdbcClient.sql("select * from table where id = :id") 
                        .param("<caret>id", 1)
                        .query();
            }             
        }
            """
        myFixture.configureByText("TestComponent.java", codeFragment.trimIndent())
        val psiReference = myFixture.getReferenceAtCaretPosition() as? PsiPolyVariantReference
        assertNotNull(psiReference)
        val resolve = psiReference?.multiResolve(false)
        assertTrue(resolve?.isNotEmpty() == true)
    }

    fun testReferenceMultyLineSql() {
        @Language("JAVA") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient;
        import org.springframework.stereotype.Component;
        import org.springframework.beans.factory.annotation.Autowired;
        
        @Component
        public class TestComponent {
            @Autowired JdbcClient jdbcClient;
            
            public void method() {
                jdbcClient.sql("select * from table " +
                                        "where id = :id") 
                        .param("<caret>id", 1)
                        .query();
            }             
        }
            """
        myFixture.configureByText("TestComponent.java", codeFragment.trimIndent())
        val psiReference = myFixture.getReferenceAtCaretPosition() as? PsiPolyVariantReference
        assertNotNull(psiReference)
        val resolve = psiReference?.multiResolve(false)
        assertTrue(resolve?.isNotEmpty() == true)
    }

    fun testReferenceMultyLineSqlVariable() {
        @Language("JAVA") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient;
        import org.springframework.stereotype.Component;
        import org.springframework.beans.factory.annotation.Autowired;
        
        @Component
        public class TestComponent {
            @Autowired JdbcClient jdbcClient;
            
            public void method() {
                String sql = "select * from table " +
                                        "where id = :id";
                jdbcClient.sql(sql) 
                        .param("<caret>id", 1)
                        .query();
            }             
        }
            """
        myFixture.configureByText("TestComponent.java", codeFragment.trimIndent())
        val psiReference = myFixture.getReferenceAtCaretPosition() as? PsiPolyVariantReference
        assertNotNull(psiReference)
        val resolve = psiReference?.multiResolve(false)
        assertTrue(resolve?.isNotEmpty() == true)
    }

    fun testReferenceMultyLineSqlField() {
        @Language("JAVA") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient;
        import org.springframework.stereotype.Component;
        import org.springframework.beans.factory.annotation.Autowired;
        
        @Component
        public class TestComponent {
            @Autowired JdbcClient jdbcClient;
            private static final String sql = "select * from table " +
                                        "where id = :id";            
            public void method() {
                jdbcClient.sql(sql) 
                        .param("<caret>id", 1)
                        .query();
            }             
        }
            """
        myFixture.configureByText("TestComponent.java", codeFragment.trimIndent())
        val psiReference = myFixture.getReferenceAtCaretPosition() as? PsiPolyVariantReference
        assertNotNull(psiReference)
        val resolve = psiReference?.multiResolve(false)
        assertTrue(resolve?.isNotEmpty() == true)
    }
}