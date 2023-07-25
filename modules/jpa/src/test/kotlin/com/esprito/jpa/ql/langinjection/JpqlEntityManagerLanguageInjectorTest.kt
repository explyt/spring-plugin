package com.esprito.jpa.ql.langinjection

import com.esprito.jpa.ql.JpqlLanguage
import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.testFramework.fixtures.InjectionTestFixture

abstract class JpqlEntityManagerLanguageInjectorTest : EspritoJavaLightTestCase() {
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