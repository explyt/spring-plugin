package com.esprito.jpa.ql.langinjection.kotlin

import com.esprito.jpa.ql.JpqlLanguage
import com.esprito.spring.test.ExplytJavaLightTestCase
import com.esprito.spring.test.TestLibrary
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
            "Test.kt",
            """
import jakarta.persistence.*
import javax.persistence.*

class CustomDepartmentRepositoryImpl : CustomDepartmentRepository {
    private val entityManager: EntityManager? = null

    fun loadDepartmentsWithoutEmployees(): List<Department> {
        return entityManager.createQuery(
            "SELECT DISTINCT d FROM " + "Department d<caret> WHERE d.employees IS EMPTY",
            Department::class.java
        )
            .getResultList()
    }
}
            """.trimIndent()
        )

        val injectionTestFixture = InjectionTestFixture(myFixture)

        injectionTestFixture.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }
}