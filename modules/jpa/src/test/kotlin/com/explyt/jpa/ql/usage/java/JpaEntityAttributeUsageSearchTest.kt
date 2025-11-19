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
        myFixture.configureByText("query.ejpql", "SELECT e.na<caret>me FROM MyEntity e")
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