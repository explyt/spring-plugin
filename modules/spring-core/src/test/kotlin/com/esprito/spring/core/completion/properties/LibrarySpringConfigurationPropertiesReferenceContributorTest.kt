package com.esprito.spring.core.completion.properties

import com.esprito.spring.core.properties.ConfigKeyPsiElement
import com.esprito.spring.core.properties.ConfigurationPropertyKeyReference
import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.psi.PsiMember

class LibrarySpringConfigurationPropertiesReferenceContributorTest : EspritoJavaLightTestCase() {

    override fun getTestDataPath(): String = "testdata/completion/properties"

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7)

    fun testSimpleSetter() = doTest {
        initSource = "spring.datasource.ur<caret>l"
        referenceFqn = "org.springframework.boot.autoconfigure.jdbc.DataSourceProperties#setUrl"
    }

    fun testRelaxedBinding() = doTest {
        initSource = "spring.datasource.jndi-nam<caret>e=abc"
        referenceFqn = "org.springframework.boot.autoconfigure.jdbc.DataSourceProperties#setJndiName"
    }

    fun testNestedClass() = doTest {
        initSource = "spring.datasource.xa.data-source-class-na<caret>me=name"
        referenceFqn = "org.springframework.boot.autoconfigure.jdbc.DataSourceProperties.Xa#setDataSourceClassName"
    }

    fun testProjectSimpleSetter() {
        myFixture.copyFileToProject("ExternalSettings.java")
        myFixture.copyFileToProject("TestConfig.java")
        doTest {
            initSource = "mail.host-na<caret>me=hostName"
            referenceFqn = "TestConfig#setHostName"
        }
    }

    fun testProjectNestedClass() {
        myFixture.copyFileToProject("ExternalSettings.java")
        myFixture.copyFileToProject("TestConfig.java")
        doTest {
            initSource = "mail.nested-settings.another-nested-settings.pr<caret>operty2=value"
            referenceFqn = "TestConfig.AnotherNestedSettings#property2"
        }
    }

    private fun doTest(init: TestModel.() -> Unit) {
        val model = TestModel()
        model.init()

        myFixture.configureByText("application.properties", model.initSource)
        val ref = file.findReferenceAt(myFixture.caretOffset) as? ConfigurationPropertyKeyReference
        assertNotNull(ref)

        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val resolvedElement = (resolveResult.element as? ConfigKeyPsiElement)?.parent as? PsiMember
        assertNotNull(resolvedElement)
        val memberName = resolvedElement!!.name
        val classFqn = resolvedElement.containingClass?.qualifiedName
        assertEquals(model.referenceFqn, "${classFqn}#${memberName}")
    }

    private class TestModel {
        lateinit var initSource: String
        lateinit var referenceFqn: String
    }
}