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

package com.explyt.spring.core.completion.properties.kotlin

import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiMember
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference

class LibrarySpringConfigurationPropertiesReferenceContributorTest : ExplytKotlinLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "completion/properties"

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
        myFixture.copyFileToProject("ExternalSettings.kt")
        myFixture.copyFileToProject("TestConfig.kt")
        doTest {
            initSource = "mail.host-na<caret>me=hostName"
            referenceFqn = "TestConfig#setHostName"
        }
    }

    fun testProjectNestedClass() {
        myFixture.copyFileToProject("ExternalSettings.kt")
        myFixture.copyFileToProject("TestConfig.kt")
        doTest {
            initSource = "mail.nested-settings.another-nested-settings.pr<caret>operty2=value"
            referenceFqn = "TestConfig.AnotherNestedSettings#property2"
        }
    }

    private fun doTest(init: TestModel.() -> Unit) {
        val model = TestModel()
        model.init()

        myFixture.configureByText("application.properties", model.initSource)
        val ref = file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference
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