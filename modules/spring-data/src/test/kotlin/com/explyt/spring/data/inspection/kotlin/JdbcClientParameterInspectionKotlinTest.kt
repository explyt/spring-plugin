/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data.inspection.kotlin

import com.explyt.spring.data.inspection.SpringJdbcClientParamInspection
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class JdbcClientParameterInspectionKotlinTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springJdbc_6_2_5
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringJdbcClientParamInspection::class.java)
    }

    fun testNamedParamSuccess() {
        @Language("kotlin") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient
        import org.springframework.stereotype.Component
        import org.springframework.beans.factory.annotation.Autowired
        
        @Component
        class TestComponent {
            @Autowired
            lateinit var jdbcClient: JdbcClient
            
            fun method() {
                jdbcClient.sql("select * from table where name = :name and id = :id")
                    .param("name", "MyName")
                    .param("id", 1)
                    .query()
    }
        }
        """
        myFixture.configureByText("TestComponent.kt", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.kt")
    }

    fun testIndexParamSuccess() {
        @Language("kotlin") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient
        import org.springframework.stereotype.Component
        import org.springframework.beans.factory.annotation.Autowired
        
        @Component
        class TestComponent {
            @Autowired lateinit var jdbcClient: JdbcClient
            
            fun method() {
                jdbcClient.sql("select * from table where name = ? and id = ?")
                    .param(1, "name")
                    .param(2, "id")
                    .query()
            }
        }
        """
        myFixture.configureByText("TestComponent.kt", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.kt")
    }

    fun testNamedParamNotFound() {
        @Language("kotlin") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient
        import org.springframework.stereotype.Component
        import org.springframework.beans.factory.annotation.Autowired
        
        @Component
        class TestComponent {
            @Autowired
            lateinit var jdbcClient: JdbcClient
            
            fun method() {
                jdbcClient.sql("select * from table where name = :name and id = :id")
                    .param(<warning descr="Parameter with name: name1 not found in SQL query">"name1"</warning>, "MyName")
                    .query()
    }
        }
        """
        myFixture.configureByText("TestComponent.kt", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.kt")
    }

    fun testNamedParamNotFoundSubstring() {
        @Language("kotlin") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient
        import org.springframework.stereotype.Component
        import org.springframework.beans.factory.annotation.Autowired
        
        @Component
        class TestComponent {
            @Autowired
            lateinit var jdbcClient: JdbcClient
            
            fun method() {
                jdbcClient.sql("select * from table where name = :test1" +
                                                                " and id = :id")
                        .param(<warning descr="Parameter with name: test not found in SQL query">"test"</warning>, "MyName") 
                        .param("id", 1)
                        .query()
    }
        }
        """
        myFixture.configureByText("TestComponent.kt", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.kt")
    }

    fun testIndexParamZero() {
        @Language("kotlin") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient
        import org.springframework.stereotype.Component
        import org.springframework.beans.factory.annotation.Autowired
        
        @Component
        class TestComponent {
            @Autowired
            lateinit var jdbcClient: JdbcClient
            
            fun method() {
                jdbcClient.sql("select * from table where name = ?")
                    .param(<warning descr="'JdbcIndex' parameter should be start with 1">0</warning>, "MyName")
                    .query()
    }
        }
        """
        myFixture.configureByText("TestComponent.kt", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.kt")
    }

    fun testIndexParamOutOfRange() {
        @Language("kotlin") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient
        import org.springframework.stereotype.Component
        import org.springframework.beans.factory.annotation.Autowired
        
        @Component
        class TestComponent {
            @Autowired
            lateinit var jdbcClient: JdbcClient
            
            fun method() {
                jdbcClient.sql("select * from table where name = ?")
                    .param(<warning descr="JdbcIndex parameter is out of range. All parameters count: 1">2</warning>, "MyName")
                    .query()
    }
        }
        """
        myFixture.configureByText("TestComponent.kt", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.kt")
    }

    fun testIndexParamDuplicate() {
        @Language("kotlin") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient
        import org.springframework.stereotype.Component
        import org.springframework.beans.factory.annotation.Autowired
        
        @Component
        class TestComponent {
            @Autowired
            lateinit var jdbcClient: JdbcClient
            
            fun method() {
                jdbcClient.sql("select * from table where name = ?")
                    .param(<warning>1</warning>, "id1")
                    .param(<warning>1</warning>, "id1")
                    .query()
    }
        }
        """
        myFixture.configureByText("TestComponent.kt", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.kt")
    }
}
