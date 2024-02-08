package com.esprito.jpa.ql.usage.kotlin

import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary

abstract class JpaEntityUsageSearchTest : EspritoJavaLightTestCase() {
    class Jakarta : JpaEntityUsageSearchTest() {
        override val libraries = arrayOf(
            TestLibrary.jakarta_persistence_3_1_0
        )
    }

    class Javax : JpaEntityUsageSearchTest() {
        override val libraries = arrayOf(
            TestLibrary.javax_persistence_2_2
        )
    }

    fun testFindUsageByImplicitName() {
        myFixture.configureByText("query.jpql", "SELECT e FROM MyEnt<caret>ity e")
        val targetElement = myFixture.elementAtCaret

        myFixture.configureByText(
            "MyEntity.kt", """
            import javax.persistence.*
            import jakarta.persistence.*
            
            @Entity
            class MyE<caret>ntity
        """.trimIndent()
        )

        val usages = myFixture.findUsages(myFixture.elementAtCaret)

        assertSize(1, usages)

        assertEquals(targetElement, usages.first().reference?.element)
    }

    fun testFindUsageByExplicitName() {
        myFixture.configureByText("query.jpql", "SELECT e FROM Name<caret>Override e")
        val targetElement = myFixture.elementAtCaret

        myFixture.configureByText(
            "MyEntity.kt", """
            import javax.persistence.*
            import jakarta.persistence.*
            
            @Entity(name = "NameOverride")
            class MyE<caret>ntity
        """.trimIndent()
        )

        val usages = myFixture.findUsages(myFixture.elementAtCaret)

        assertSize(1, usages)

        assertEquals(targetElement, usages.first().reference?.element)
    }
}