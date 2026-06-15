/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data.inspection.java

import com.explyt.spring.data.inspection.SpringJdbcClientParamInspection
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class JdbcClientParameterInspectionTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springJdbc_6_2_5
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringJdbcClientParamInspection::class.java)
    }

    fun testNamedParamSuccess() {
        @Language("JAVA") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient;
        import org.springframework.stereotype.Component;
        import org.springframework.beans.factory.annotation.Autowired;
        
        @Component
        public class TestComponent {
            @Autowired JdbcClient jdbcClient;
            
            public void method() {
                jdbcClient.sql("select * from table where name = :name and id = :id")
                        .param("name", "MyName")
                        .param("id", 1)
                        .query();
            }
                        	
        }
            """
        myFixture.configureByText("TestComponent.java", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.java")
    }

    fun testIndexParamSuccess() {
        @Language("JAVA") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient;
        import org.springframework.stereotype.Component;
        import org.springframework.beans.factory.annotation.Autowired;
        
        @Component
        public class TestComponent {
            @Autowired JdbcClient jdbcClient;
            
            public void method() {
                jdbcClient.sql("select * from table where name = ? and id = ?")
                        .param(1, "name")
                        .param(2, "id")
                        .query();
            }
                        	
        }
            """
        myFixture.configureByText("TestComponent.java", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.java")
    }

    fun testNamedParamNotFound() {
        @Language("JAVA") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient;
        import org.springframework.stereotype.Component;
        import org.springframework.beans.factory.annotation.Autowired;
        
        @Component
        public class TestComponent {
            @Autowired JdbcClient jdbcClient;
            
            public void method() {
                jdbcClient.sql("select * from table where name = :name and id = :id")
                        .param(<warning descr="Parameter with name: test1 not found in SQL query">"test1"</warning>, "MyName")
                        .param(<warning descr="Parameter with name: test2 not found in SQL query">"test2"</warning>, "MyName", 4)
                        .param("id", 1)
                        .query();
            }
                        	
        }
            """
        myFixture.configureByText("TestComponent.java", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.java")
    }

    fun testNamedParamNotFoundSubstring() {
        @Language("JAVA") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient;
        import org.springframework.stereotype.Component;
        import org.springframework.beans.factory.annotation.Autowired;
        
        @Component
        public class TestComponent {
            @Autowired JdbcClient jdbcClient;
            
            public void method() {
                jdbcClient.sql("select * from table where name = :test1" +
                                                                " and id = :id")
                        .param(<warning descr="Parameter with name: test not found in SQL query">"test"</warning>, "MyName") 
                        .param("id", 1)
                        .query();
            }
                        	
        }
            """
        myFixture.configureByText("TestComponent.java", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.java")
    }

    fun testIndexParamZero() {
        @Language("JAVA") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient;
        import org.springframework.stereotype.Component;
        import org.springframework.beans.factory.annotation.Autowired;
        
        @Component
        public class TestComponent {
            @Autowired JdbcClient jdbcClient;
            
            public void method() {
                jdbcClient.sql("select * from table where id = ?")
                    .param(<warning descr="'JdbcIndex' parameter should be start with 1">0</warning>, "id");  
            }             
        }
            """
        myFixture.configureByText("TestComponent.java", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.java")
    }

    fun testIndexParamOutOfRange() {
        @Language("JAVA") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient;
        import org.springframework.stereotype.Component;
        import org.springframework.beans.factory.annotation.Autowired;
        
        @Component
        public class TestComponent {
            @Autowired JdbcClient jdbcClient;
            
            public void method() {
                jdbcClient.sql("select * from table where id = ?")
                    .param(<warning descr="JdbcIndex parameter is out of range. All parameters count: 1">2</warning>, "id");  
            }             
        }
            """
        myFixture.configureByText("TestComponent.java", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.java")
    }

    fun testIndexParamDuplicate() {
        @Language("JAVA") val codeFragment = """
        import org.springframework.jdbc.core.simple.JdbcClient;
        import org.springframework.stereotype.Component;
        import org.springframework.beans.factory.annotation.Autowired;
        
        @Component
        public class TestComponent {
            @Autowired JdbcClient jdbcClient;
            
            public void method() {
                jdbcClient.sql("select * from table where id = ?")
                    .param(<warning>1</warning>, "id1")  
                    .param(<warning>1</warning>, "id2");
            }             
        }
            """
        myFixture.configureByText("TestComponent.java", codeFragment.trimIndent())
        myFixture.testHighlighting("TestComponent.java")
    }
}
