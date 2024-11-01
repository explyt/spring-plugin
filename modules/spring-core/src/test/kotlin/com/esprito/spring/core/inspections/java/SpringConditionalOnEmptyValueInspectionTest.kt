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

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.providers.kotlin.ConfigurationPropertyLineMarkerProviderTest.Companion.APPLICATION_PROPERTIES_FILE_NAME
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import junit.framework.TestCase
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

class SpringConditionalOnEmptyValueInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("conditionalOnPropertyValue")
    fun testConditionalOnPropertyValue() =
        doTest(com.explyt.spring.core.inspections.SpringConditionalOnEmptyValueInspection())

    fun testPropertyCondition() {
        myFixture.addFileToProject(APPLICATION_PROPERTIES_FILE_NAME, "test.prop=1")

        @Language("JAVA") val text = """           
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CONDITIONAL_ON_PROPERTY}(name="test.prop")                
            public class TestConditional { }
            """.trimIndent()

        val psiFile = myFixture.configureByText("TestConditional.java", text)

        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertTrue(allActiveBeans.isNotEmpty())
        TestCase.assertTrue(allActiveBeans.any { it.containingFile == psiFile })
    }

    fun testPropertyConditionMatchIfMissing() {
        @Language("JAVA") val text = """           
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CONDITIONAL_ON_PROPERTY}(name="test.prop")                
            public class TestConditional { }
            """.trimIndent()

        val psiFile = myFixture.configureByText("TestConditional.java", text)

        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertTrue(allActiveBeans.isNotEmpty())
        TestCase.assertTrue(allActiveBeans.none { it.containingFile == psiFile })
    }

    fun testPropertyConditionMatchIfMissingNegative() {
        @Language("JAVA") val text = """           
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CONDITIONAL_ON_PROPERTY}(name="test.prop", matchIfMissing = true)                
            public class TestConditional { }
            """.trimIndent()

        val psiFile = myFixture.configureByText("TestConditional.java", text)

        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertTrue(allActiveBeans.isNotEmpty())
        TestCase.assertTrue(allActiveBeans.any { it.containingFile == psiFile })
    }

    fun testPropertyConditionHavingValue() {
        myFixture.addFileToProject(APPLICATION_PROPERTIES_FILE_NAME, "test.prop=1")

        @Language("JAVA") val text = """           
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CONDITIONAL_ON_PROPERTY}(name="test.prop", havingValue="1")                
            public class TestConditional { }
            """.trimIndent()

        val psiFile = myFixture.configureByText("TestConditional.java", text)

        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertTrue(allActiveBeans.isNotEmpty())
        TestCase.assertTrue(allActiveBeans.any { it.containingFile == psiFile })
    }

    fun testPropertyConditionHavingValueNegative() {
        myFixture.addFileToProject(APPLICATION_PROPERTIES_FILE_NAME, "test.prop=2")

        @Language("JAVA") val text = """           
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CONDITIONAL_ON_PROPERTY}(name="test.prop", havingValue="1")                
            public class TestConditional { }
            """.trimIndent()

        val psiFile = myFixture.configureByText("TestConditional.java", text)

        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertTrue(allActiveBeans.isNotEmpty())
        TestCase.assertTrue(allActiveBeans.none { it.containingFile == psiFile })
    }

    fun testPropertyConditionHavingValueMissing() {
        @Language("JAVA") val text = """           
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CONDITIONAL_ON_PROPERTY}(name="test.prop", havingValue="1")                
            public class TestConditional { }
            """.trimIndent()

        val psiFile = myFixture.configureByText("TestConditional.java", text)

        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertTrue(allActiveBeans.isNotEmpty())
        TestCase.assertTrue(allActiveBeans.none { it.containingFile == psiFile })
    }
}