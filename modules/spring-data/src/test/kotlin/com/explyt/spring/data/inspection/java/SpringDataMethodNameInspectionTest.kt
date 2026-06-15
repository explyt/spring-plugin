/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data.inspection.java

import com.explyt.spring.data.inspection.SpringDataMethodNameInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringDataMethodNameInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7, TestLibrary.springDataJpa_3_1_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringDataMethodNameInspection::class.java)
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

    fun testUnknownPropertyInMethod() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	Entity findBy<warning descr="Unknown property 'name1'">Name1</warning>();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testMissingPropertyInMethod() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	Entity <warning descr="Empty property 'findByNameOr<MISSING_PROPERTY>And'"><warning descr="Empty property 'findByNameOrAnd<MISSING_PROPERTY>'">findByNameOrAnd</warning></warning>();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testCorrectOrderBy() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	Entity findByNameAndCityOrderById();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testUnknownPropertyOrderBy() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	Entity findByNameAndCityOrderBy<warning descr="Unknown property 'prop'">Prop</warning>();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testMissingPropertyOrderBy() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	Entity <warning descr="Empty property 'findByNameAndCityOrderBy<MISSING_PROPERTY>Asc'">findByNameAndCityOrderByAsc</warning>();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testOverride() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.jpa.repository.JpaRepository<Entity, Integer> {
            	@Override
                java.util.List<Entity> findAll();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testDefaultMethod() {
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	default Entity findByNoName() {
                    return null;            
                }            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }
}
