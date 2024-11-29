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

package com.explyt.jpa.ql.langinjection.java

import com.explyt.jpa.ql.JpqlLanguage
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.testFramework.fixtures.InjectionTestFixture

abstract class JpqlEntityManagerLanguageInjectorTest : ExplytJavaLightTestCase() {
    class Jakarta : JpqlEntityManagerLanguageInjectorTest() {
        override val libraries = arrayOf(
            TestLibrary.jakarta_persistence_3_1_0
        )
    }

    class Javax : JpqlEntityManagerLanguageInjectorTest() {
        override val libraries = arrayOf(
            TestLibrary.javax_persistence_2_2
        )
    }

    fun testInjection() {
        val vf = myFixture.configureByText(
            "Test.java",
            """
import jakarta.persistence.*;
import javax.persistence.*;

import java.util.List;

public class CustomDepartmentRepositoryImpl implements CustomDepartmentRepository {
    private EntityManager entityManager;

    @Override
    public List<Department> loadDepartmentsWithoutEmployees() {
        return entityManager.createQuery(
                        "SELECT DISTINCT d FROM " + "Department d<caret> WHERE d.employees IS EMPTY",
                        Department.class
                )
                .getResultList();
    }
}
            """.trimIndent()
        )

        val injectionTestFixture = InjectionTestFixture(myFixture)

        injectionTestFixture.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }
}