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

package com.explyt.spring.data.inspection.java

import com.explyt.spring.data.inspection.SpringDataReturnTypeInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringDataReturnTypeInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7, TestLibrary.springDataJpa_3_1_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringDataReturnTypeInspection::class.java)
        myFixture.addClass(
            """
            public class Entity {
            		public Integer id;
            		public String name;
                    public String city;
            }
        """.trimIndent()
        )
    }

    fun testCorrectMethod() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	Entity findByNameAndCity();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testCorrectExistsMethod() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	boolean existsByName();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testCorrectCountMethod() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	int countByName();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testCorrectDeleteMethod() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	void deleteByName();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testInvalidExistsMethod() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	<warning descr="Return type must be a boolean">void</warning> existsByName();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testInvalidCountMethod() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	<warning descr="Return type must be a number">void</warning> countByName();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testInvalidDeleteMethod() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	<warning descr="Return type must be a number or void">Entity</warning> deleteByName();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }
}
