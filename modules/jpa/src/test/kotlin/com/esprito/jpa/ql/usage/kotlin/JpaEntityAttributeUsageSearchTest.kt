package com.esprito.jpa.ql.usage.kotlin

import com.esprito.spring.test.ExplytJavaLightTestCase
import com.esprito.spring.test.TestLibrary

abstract class JpaEntityAttributeUsageSearchTest : ExplytJavaLightTestCase() {
    class Jakarta : JpaEntityAttributeUsageSearchTest() {
        override val libraries = arrayOf(
            TestLibrary.jakarta_persistence_3_1_0
        )
    }

    class Javax : JpaEntityAttributeUsageSearchTest() {
        override val libraries = arrayOf(
            TestLibrary.javax_persistence_2_2
        )
    }

    fun testFindUsage() {
        myFixture.configureByText("query.jpql", "SELECT e.na<caret>me FROM MyEntity e")
        val targetElement = myFixture.elementAtCaret

        myFixture.configureByText(
            "MyEntity.kt", """
            import jakarta.persistence.*
            import javax.persistence.*
            
            @Entity
            class MyEntity {
                @Column
                var na<caret>me: String? = null
            }
        """.trimIndent()
        )

        val usages = myFixture.findUsages(myFixture.elementAtCaret)

        assertSize(1, usages)

        assertEquals(targetElement, usages.first().reference?.element)
    }
}