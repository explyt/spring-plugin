/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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