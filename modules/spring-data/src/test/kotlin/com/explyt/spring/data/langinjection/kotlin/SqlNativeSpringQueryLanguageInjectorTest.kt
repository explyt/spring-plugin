/*
 * Copyright © 2024 Explyt Ltd
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

package com.explyt.spring.data.langinjection.kotlin

import com.explyt.jpa.ql.JpqlLanguage
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.testFramework.fixtures.InjectionTestFixture
import org.intellij.lang.annotations.Language

class SqlNativeSpringQueryLanguageInjectorTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springDataJpa_3_4_0, TestLibrary.jakarta_persistence_3_1_0
    )

    override fun setUp() {
        super.setUp()
        SpringToolRunConfigurationsSettingsState.getInstance().sqlLanguageId = JpqlLanguage.INSTANCE.id
    }

    override fun tearDown() {
        super.tearDown()
        SpringToolRunConfigurationsSettingsState.getInstance().sqlLanguageId = ""
    }

    fun testNativeQueryInjection() {
        @Language("kotlin") val code = """  
            import org.springframework.data.jpa.repository.Query
            import org.springframework.data.repository.Repository

            interface TestRepository : Repository<String, Integer> {
            	@Query(value = "select a from users as uv where uv.a > 0 group by u <caret> having max(s)", nativeQuery = true)	       
	            fun findAll(): List<String>           	
            }
            """

        myFixture.configureByText("TestRepository.kt", code.trimIndent())
        val injectionTestFixture = InjectionTestFixture(myFixture)
        injectionTestFixture.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }

    fun testJdbcTemplateQueryInjection() {
        @Language("kotlin") val code = """  
            import org.springframework.jdbc.core.JdbcTemplate
            import org.springframework.stereotype.Component
            
            @Component
            class TestService(val jdbcTemplate: JdbcTemplate) {
                fun testQuery() {
                    jdbcTemplate.query("select uv12.a as p21 from users<caret> u") {(rs, rowNum) -> "" }
                }
            }
            """

        myFixture.configureByText("TestService.kt", code.trimIndent())
        val injectionTestFixture = InjectionTestFixture(myFixture)
        injectionTestFixture.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }

    fun testNamedParameterJdbcTemplateQueryInjection() {
        @Language("kotlin") val code = """  
            import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource
            import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
            import org.springframework.stereotype.Component
            
            class Employee(var firstName: String)

            @Component
            class TestService(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
                fun testQuery() {
                    val employee = Employee("James")
                    val namedParameters = BeanPropertySqlParameterSource(employee)
                    (namedParameterJdbcTemplate as NamedParameterJdbcTemplate).query(
                        "SELECT COUNT(*) FROM EMPLOYEE<caret> WHERE FIRST_NAME = :firstName"
                        , namedParameters) { rs -> "" }
                }
            }
            """

        myFixture.configureByText("TestService.kt", code.trimIndent())
        val injectionTestFixture = InjectionTestFixture(myFixture)
        injectionTestFixture.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }

    fun testJdbcClientQueryInjection() {
        @Language("kotlin") val code = """  
            import org.springframework.jdbc.core.RowCountCallbackHandler
            import org.springframework.jdbc.core.simple.JdbcClient
            import org.springframework.stereotype.Component
            
            @Component
            class TestService(val jdbcClient: JdbcClient) {
                fun testQuery() {
                    val countCallbackHandler = RowCountCallbackHandler()
                    jdbcClient.sql("SELECT COUNT(*) FROM EMPLOYEE<caret> WHERE FIRST_NAME = ?")
                        .param("James")
                        .query(countCallbackHandler)
                    countCallbackHandler.getRowCount()
                }
            }
            """

        myFixture.configureByText("TestService.kt", code.trimIndent())
        val injectionTestFixture = InjectionTestFixture(myFixture)
        injectionTestFixture.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }

    fun testJdbcTemplateExecuteInjection() {
        @Language("kotlin") val code = """  
            import org.springframework.jdbc.core.JdbcTemplate
            import org.springframework.stereotype.Component
            
            @Component
            class TestService(val jdbcTemplate: JdbcTemplate) {                
                fun testQuery() {
                    jdbcTemplate.execute("select uv12.a as p21 <caret>from users u join employe e where u.p21 > 0 and e.id=0 ")
                }
            }
            """

        myFixture.configureByText("TestService.kt", code.trimIndent())
        val injectionTestFixture = InjectionTestFixture(myFixture)
        injectionTestFixture.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }

    fun testJdbcTemplateBatchUpdateInjection() {
        @Language("kotlin") val code = """  
            import org.springframework.jdbc.core.JdbcTemplate
            import org.springframework.stereotype.Component
            
            @Component 
            class TestService(val jdbcTemplate: JdbcTemplate) {
                fun testQuery() {
                    jdbcTemplate.batchUpdate("select * from t%s", "select uv12.a as p21 from users u%s")
                }
            }
            """

        myFixture.configureByText("TestService.kt", code.trimIndent().format("<caret>", ""))
        val injectionTestFixture1 = InjectionTestFixture(myFixture)
        injectionTestFixture1.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)

        myFixture.configureByText("TestService.kt", code.trimIndent().format("", "<caret>"))
        val injectionTestFixture2 = InjectionTestFixture(myFixture)
        injectionTestFixture2.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }

    fun testVarargInjection() {
        @Language("kotlin") val code = """  
            import org.springframework.jdbc.core.JdbcTemplate
            import org.springframework.stereotype.Component
            import java.nio.file.Path
            
            @Component 
            class TestService(val jdbcTemplate: JdbcTemplate) {
                fun testQuery() {
                    val path = Path.of("first", "second", "select * from <caret>ts") 
                }
            }
            """

        myFixture.configureByText("TestService.kt", code.trimIndent())
        val injectionTestFixture1 = InjectionTestFixture(myFixture)
        injectionTestFixture1.assertInjectedLangAtCaret(null)
    }

    fun testJdbcTemplateUpdateInjection() {
        @Language("kotlin") val code = """  
            import org.springframework.jdbc.core.JdbcTemplate
            import org.springframework.stereotype.Component
            
            @Component
            class TestService(val jdbcTemplate: JdbcTemplate) {
                fun testQuery() {
                    jdbcTemplate.update("update ttt set id=1<caret>")
                }
            }
            """

        myFixture.configureByText("TestService.kt", code.trimIndent())
        val injectionTestFixture1 = InjectionTestFixture(myFixture)
        injectionTestFixture1.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }

    fun testJdbcTemplateUpdateInjectionMultiLine() {
        @Language("kotlin") val code = """  
            import org.springframework.jdbc.core.JdbcTemplate
            import org.springframework.stereotype.Component
            
            @Component 
            class TestService(val jdbcTemplate: JdbcTemplate) {
                fun testQuery() {
                    jdbcTemplate.update("update ttt %s" +
                        "set id=1%s")
                }
            }
            """

        myFixture.configureByText("TestService.kt", code.trimIndent().format("<caret>", ""))
        val injectionTestFixture1 = InjectionTestFixture(myFixture)
        injectionTestFixture1.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)

        myFixture.configureByText("TestService.kt", code.trimIndent().format("", "<caret>"))
        val injectionTestFixture2 = InjectionTestFixture(myFixture)
        injectionTestFixture2.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }

    fun testNamedNativeQueryInjection() {
        @Language("kotlin") val code = """   
            
            @jakarta.persistence.Entity
            @jakarta.persistence.NamedNativeQuery(name = "query", query = "SELECT d FROM " + "<caret>Department d")
            class TestEntity 
            """


        myFixture.configureByText("TestEntity.kt", code.trimIndent())
        val injectionTestFixture = InjectionTestFixture(myFixture)
        injectionTestFixture.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }

    fun testNativeNoInjection() {
        @Language("kotlin") val code = """   
            import org.springframework.stereotype.Component
            
            @Component
            class TestService {
                fun testQuery() {
                    System.out.printf("update %s ttt set id=1<caret> %n", "1")
                }
            }
            """

        myFixture.configureByText("TestService.kt", code.trimIndent())
        val injectionTestFixture1 = InjectionTestFixture(myFixture)
        injectionTestFixture1.assertInjectedLangAtCaret(null)
    }
}