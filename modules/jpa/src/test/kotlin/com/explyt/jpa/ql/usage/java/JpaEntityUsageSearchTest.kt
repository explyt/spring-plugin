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

package com.explyt.jpa.ql.usage

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary

abstract class JpaEntityUsageSearchTest : ExplytJavaLightTestCase() {
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
            "MyEntity.java", """
            import javax.persistence.*;
            import jakarta.persistence.*;
            
            @Entity
            public class MyE<caret>ntity {
            }
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
            "MyEntity.java", """
            import javax.persistence.*;
            import jakarta.persistence.*;
            
            @Entity(name = "NameOverride")
            public class MyE<caret>ntity {
            }
        """.trimIndent()
        )

        val usages = myFixture.findUsages(myFixture.elementAtCaret)

        assertSize(1, usages)

        assertEquals(targetElement, usages.first().reference?.element)
    }
}