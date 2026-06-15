/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
