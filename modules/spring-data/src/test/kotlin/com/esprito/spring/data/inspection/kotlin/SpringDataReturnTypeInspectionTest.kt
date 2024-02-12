package com.esprito.spring.data.inspection.kotlin

import com.esprito.spring.data.inspection.SpringDataReturnTypeInspection
import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary

class SpringDataReturnTypeInspectionTest : EspritoInspectionKotlinTestCase() {
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
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByNameAndCity():Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testCorrectExistsMethod() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun existsByName():Boolean            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testCorrectCountMethod() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun countByName(): Int            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testCorrectDeleteMethod() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun deleteByName()            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testInvalidExistsMethod() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun existsByName():<warning descr="Return type must be a boolean">Int</warning>            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testInvalidCountMethod() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun countByName():<warning descr="Return type must be a number">Boolean</warning>            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testInvalidDeleteMethod() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun deleteByName():<warning descr="Return type must be a number or void">Boolean</warning>            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }
}
