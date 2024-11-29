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

package com.explyt.spring.core.reference.kotlin

import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.core.properties.references.ValueHintReference
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference

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

}