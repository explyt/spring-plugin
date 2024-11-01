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

package com.explyt.spring.data.inspection.kotlin

import com.explyt.spring.data.inspection.SpringDataMethodNameInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary

class SpringDataMethodNameInspectionTest : ExplytInspectionKotlinTestCase() {
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
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByNameAndCity():Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testUnknownPropertyInMethod() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findBy<warning descr="Unknown property 'name1'">Name1</warning>():Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testMissingPropertyInMethod() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun <warning descr="Empty property 'findByNameOr<MISSING_PROPERTY>And'"><warning descr="Empty property 'findByNameOrAnd<MISSING_PROPERTY>'">findByNameOrAnd</warning></warning>():Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testCorrectOrderBy() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByNameAndCityOrderById():Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testUnknownPropertyOrderBy() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByNameAndCityOrderBy<warning descr="Unknown property 'prop'">Prop</warning>():Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testMissingPropertyOrderBy() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun <warning descr="Empty property 'findByNameAndCityOrderBy<MISSING_PROPERTY>Asc'">findByNameAndCityOrderByAsc</warning>():Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testOverride() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findAll():Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testDefaultMethod() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findAll():Entity? { return null }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }
}
