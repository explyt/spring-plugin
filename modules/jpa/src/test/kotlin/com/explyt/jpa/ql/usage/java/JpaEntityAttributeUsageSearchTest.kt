package com.explyt.jpa.ql.usage

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary

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
            "MyEntity.java", """
            import javax.persistence.*;
            import jakarta.persistence.*;
            
            @Entity
            public class MyEntity {
                @Column
                String na<caret>me;
            }
        """.trimIndent()
        )

        val usages = myFixture.findUsages(myFixture.elementAtCaret)

        assertSize(1, usages)

        assertEquals(targetElement, usages.first().reference?.element)
    }
}