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

package com.explyt.spring.core.properties.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.lang.properties.IProperty
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.ResolveResult
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.junit.Assert
import java.io.File

private const val TEST_DATA_PATH = "properties"

@TestMetadata(TEST_DATA_PATH)
class ConfigurationPropertyReferenceContributorTest : ExplytJavaLightTestCase() {

    companion object {
        const val PLACE_TO_INSERT = "<PLACE_TO_INSERT>"
    }

    override fun getTestDataPath(): String = super.getTestDataPath() + "properties"

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7)

    fun testVariants() {
        preparation("spring.datasource.<caret>")

        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        assertEquals(
            setOf(
                "spring.datasource.driver-class-name",
                "spring.datasource.password",
                "spring.datasource.url",
                "spring.datasource.username"
            ), lookupElementStrings!!.toSet()
        )
    }

    fun testResolve() {
        preparation("spring.datasource.ur<caret>l")

        val ref = file.findReferenceAt(myFixture.caretOffset) as? PsiPolyVariantReference
        assertNotNull(ref)

        val multiResolve = ref!!.multiResolve(true)
        assertEquals(2, multiResolve.size)
        multiResolve.expect("spring.datasource.url", true)
        multiResolve.expect("spring.datasource.url", false)
    }

    private fun Array<ResolveResult>.expect(propertyKey: String, isProperty: Boolean) {
        Assert.assertTrue("Property '${propertyKey}' not found", any {
            val element = it.element ?: return@any false
            if (isProperty) {
                (element as? IProperty)?.key == propertyKey
            } else {
                (element as? YAMLKeyValue)?.let { yamlKeyValue ->
                    YAMLUtil.getConfigFullName(yamlKeyValue)
                } == propertyKey
            }
        })
    }

    private fun preparation(replaceText: String) {
        myFixture.copyFileToProject("application.properties")
        myFixture.copyFileToProject("application.yaml")

        val testDataPath = getTestDataPath()
        val sourceFile = File(testDataPath, FileUtil.toSystemDependentName("TestComponent.java"))
        val processedSource = FileUtil.loadFile(sourceFile).replace(PLACE_TO_INSERT, replaceText)
        myFixture.configureByText("TestComponent.java", processedSource)
    }
}