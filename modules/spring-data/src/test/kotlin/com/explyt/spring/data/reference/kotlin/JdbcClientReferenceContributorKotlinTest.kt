/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data.reference.kotlin

import com.explyt.jpa.ql.JpqlLanguage
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiPolyVariantReference
import org.intellij.lang.annotations.Language

class JdbcClientReferenceContributorKotlinTest : ExplytKotlinLightTestCase() {
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
        @Language("kotlin") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient
        import org.springframework.stereotype.Component
        import org.springframework.beans.factory.annotation.Autowired
        
        @Component
        class TestComponent {
            @Autowired lateinit var jdbcClient: JdbcClient
            
            fun method() {
                jdbcClient.sql("select * from table where id = :id")
                    .param("<caret>id", 1)
                    .query()
            }
        }
        """
        myFixture.configureByText("TestComponent.kt", codeFragment.trimIndent())
        val psiReference = myFixture.getReferenceAtCaretPosition() as? PsiPolyVariantReference
        assertNotNull(psiReference)
        val resolve = psiReference?.multiResolve(false)
        assertTrue(resolve?.isNotEmpty() == true)
    }

    fun testReferenceMultyLineSql() {
        @Language("kotlin") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient
        import org.springframework.stereotype.Component
        import org.springframework.beans.factory.annotation.Autowired
        
        @Component
        class TestComponent {
            @Autowired lateinit var jdbcClient: JdbcClient
            
            fun method() {
                jdbcClient.sql("select * from table" +
                            " where id = :id")
                    .param("<caret>id", 1)
                    .query()
            }
        }
        """
        myFixture.configureByText("TestComponent.kt", codeFragment.trimIndent())
        val psiReference = myFixture.getReferenceAtCaretPosition() as? PsiPolyVariantReference
        assertNotNull(psiReference)
        val resolve = psiReference?.multiResolve(false)
        assertTrue(resolve?.isNotEmpty() == true)
    }

    fun testReferenceMultyLineSqlVariable() {
        @Language("kotlin") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient
        import org.springframework.stereotype.Component
        import org.springframework.beans.factory.annotation.Autowired
        
        @Component
        class TestComponent {
            @Autowired lateinit var jdbcClient: JdbcClient
            
            fun method() {
                val sql = "select * from table" +
                        " where id = :id"
                jdbcClient.sql(sql)
                    .param("<caret>id", 1)
                    .query()
            }
        }
        """
        myFixture.configureByText("TestComponent.kt", codeFragment.trimIndent())
        val psiReference = myFixture.getReferenceAtCaretPosition() as? PsiPolyVariantReference
        assertNotNull(psiReference)
        val resolve = psiReference?.multiResolve(false)
        assertTrue(resolve?.isNotEmpty() == true)
    }

    fun testReferenceMultyLineSqlField() {
        @Language("kotlin") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient
        import org.springframework.stereotype.Component
        import org.springframework.beans.factory.annotation.Autowired
        
        @Component
        class TestComponent {
            @Autowired lateinit var jdbcClient: JdbcClient
            private val sql = "select * from table" +
                        " where id = :id"
            fun method() {                
                jdbcClient.sql(sql)
                    .param("<caret>id", 1)
                    .query()
            }
        }
        """
        myFixture.configureByText("TestComponent.kt", codeFragment.trimIndent())
        val psiReference = myFixture.getReferenceAtCaretPosition() as? PsiPolyVariantReference
        assertNotNull(psiReference)
        val resolve = psiReference?.multiResolve(false)
        assertTrue(resolve?.isNotEmpty() == true)
    }
}
