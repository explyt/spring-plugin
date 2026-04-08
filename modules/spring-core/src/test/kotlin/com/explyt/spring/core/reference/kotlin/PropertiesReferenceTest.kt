/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.reference.kotlin

import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.core.properties.references.ValueHintReference
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference

class PropertiesReferenceTest : ExplytKotlinLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + "reference/properties"

    override val libraries: Array<TestLibrary> =
        arrayOf(
            TestLibrary.springBoot_3_1_1,
            TestLibrary.springContext_6_0_7,
            TestLibrary.resilience4j_2_2_0
        )

    fun testRefSpringBeanReference() {
        myFixture.copyFileToProject("FooBeanComponent.kt")
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.properties",
            "main.foo-bean-component=fooBean<caret>Component"
        )
        val ref = file.findReferenceAt(myFixture.caretOffset) as? ValueHintReference

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val nameClass = (resolveResult.element as? PsiClass)?.name
        assertEquals(nameClass, "FooBeanComponent")
    }

    fun testRefInnerClass() {
        myFixture.copyFileToProject("LssConfigurationProperties.kt")
        myFixture.configureByText(
            "application.properties",
            "lss.lss-plan-<caret>configuration"
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val nameClass = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(nameClass, "setLssPlanConfiguration")
    }

    fun testRefValueResource() {
        myFixture.copyFileToProject("MainFooProperties.kt")
        myFixture.configureByText(
            "application-default.properties",
            "main.foo-bean-component=fooBeanComponent"
        )
        myFixture.configureByText(
            "application.properties",
            "main.local.code-resource=classpath:application-de<caret>fault.properties"
        )

        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? FileReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val name = (multiResolve[0].element as? PropertiesFileImpl)?.name
        assertEquals(name, "application-default.properties")
    }

    fun testHandleAsValues() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.copyFileToProject("WeekEnum.kt")
        myFixture.configureByText(
            "application.properties",
            """
main.enum-value-additional=TUE<caret>SDAY
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? ValueHintReference)
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? PsiEnumConstant)?.name
        assertEquals(name, "TUESDAY")
    }

}