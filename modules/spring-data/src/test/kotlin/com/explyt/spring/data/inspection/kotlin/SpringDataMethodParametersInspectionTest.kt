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

import com.explyt.spring.data.inspection.SpringDataMethodParametersInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary

class SpringDataMethodParametersInspectionTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7, TestLibrary.springDataJpa_3_1_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringDataMethodParametersInspection::class.java)
        myFixture.addClass(
            """
            public static class TestMono<T> { 
                public T data;
            }
            """.trimMargin()
        )
        myFixture.addClass(
            """
            public class Address {             
                public int zipCode;
            }
              """.trimIndent()
        )
        myFixture.addClass(
            """
            public class Entity {
            		public Integer id;
            		public String name;
                    public String city;
                    public Address address;
                    public List<String> phones;
            }
            """.trimIndent()
        )
    }

    fun testCorrectMethod() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByIdIn(a:kotlin.collections.List<Int>):Entity            	                
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testCorrectEmbeddedField() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByAddressZipCode(a: Int):Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testIncorrectEmbeddedField() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByAddressZipCode(<warning>a:String</warning>):Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testCorrectMonoParameter() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByName(a:TestMono<String>):Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testIncorrectMonoParameter() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByName(<warning>a:TestMono<Int></warning>):Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testCorrectBetweenParameter() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByIdBetween(i1:Int, i2:Int):Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testIncorrectSecondParameterBetween() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByIdBetween(i1:Int, <warning>i2:String</warning>):Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testIncorrectBetweenMalformed() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun <warning>findByIdBetween</warning>(i1:Int):Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testCorrectCollectionFieldParameter() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByPhones(a:String):Entity            	                
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testIncorrectCollectionFieldParameterType() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByPhones(<warning descr="Expected parameter types: String">a:Int</warning>):Entity            	                
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testIncorrectCollectionFieldParameter() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByPhones(<warning>a:kotlin.collections.List<String></warning>):Entity            	                
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testRedundantMethodParameters() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun <warning>findByPhonesAndId</warning>(a:String, id:Int, o:Int):Entity            	                
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }
}
