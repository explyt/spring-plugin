package com.esprito.jpa.ql.langinjection.java

import com.esprito.jpa.ql.JpqlLanguage
import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.testFramework.fixtures.InjectionTestFixture

abstract class JpqNamedQueryLanguageInjectorTest : EspritoJavaLightTestCase() {
    class Jakarta : JpqNamedQueryLanguageInjectorTest() {
        override val libraries = arrayOf(
            TestLibrary.jakarta_persistence_3_1_0
        )
    }

    class Javax : JpqNamedQueryLanguageInjectorTest() {
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
@Entity
@NamedQuery(name = "Department.loadAll", query = "SELECT d FROM " + "<caret>Department d")
public class Department {
}
            """.trimIndent()
        )

        val injectionTestFixture = InjectionTestFixture(myFixture)

        injectionTestFixture.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }
}

