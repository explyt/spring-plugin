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
