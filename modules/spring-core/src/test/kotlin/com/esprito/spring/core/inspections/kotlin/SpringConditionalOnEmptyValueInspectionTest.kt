package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringConditionalOnEmptyValueInspection
import com.explyt.spring.core.providers.kotlin.ConfigurationPropertyLineMarkerProviderTest.Companion.APPLICATION_PROPERTIES_FILE_NAME
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import junit.framework.TestCase
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

class SpringConditionalOnEmptyValueInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("conditionalOnPropertyValue")
    fun testConditionalOnPropertyValue() = doTest(SpringConditionalOnEmptyValueInspection())

    fun testPropertyCondition() {
        myFixture.addFileToProject(APPLICATION_PROPERTIES_FILE_NAME, "test.prop=1")

        @Language("kotlin") val text = """           
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CONDITIONAL_ON_PROPERTY}(name="test.prop")                
            open class TestConditional { }
            """.trimIndent()

        val psiFile = myFixture.configureByText("TestConditional.kt", text)

        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertTrue(allActiveBeans.isNotEmpty())
        TestCase.assertTrue(allActiveBeans.any { it.containingFile.name == psiFile.name })
    }

    fun testPropertyConditionMatchIfMissing() {
        @Language("kotlin") val text = """           
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CONDITIONAL_ON_PROPERTY}(name="test.prop")                
            class TestConditional { }
            """.trimIndent()

        val psiFile = myFixture.configureByText("TestConditional.kt", text)

        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertTrue(allActiveBeans.isNotEmpty())
        TestCase.assertTrue(allActiveBeans.none { it.containingFile.name == psiFile.name })
    }

    fun testPropertyConditionMatchIfMissingNegative() {
        @Language("kotlin") val text = """           
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CONDITIONAL_ON_PROPERTY}(name="test.prop", matchIfMissing = true)                
            class TestConditional { }
            """.trimIndent()

        val psiFile = myFixture.configureByText("TestConditional.kt", text)

        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertTrue(allActiveBeans.isNotEmpty())
        TestCase.assertTrue(allActiveBeans.any { it.containingFile.name == psiFile.name })
    }

    fun testPropertyConditionHavingValue() {
        myFixture.addFileToProject(APPLICATION_PROPERTIES_FILE_NAME, "test.prop=1")

        @Language("kotlin") val text = """           
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CONDITIONAL_ON_PROPERTY}(name="test.prop", havingValue="1")                
            class TestConditional { }
            """.trimIndent()

        val psiFile = myFixture.configureByText("TestConditional.kt", text)

        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertTrue(allActiveBeans.isNotEmpty())
        TestCase.assertTrue(allActiveBeans.any { it.containingFile.name == psiFile.name })
    }

    fun testPropertyConditionHavingValueNegative() {
        myFixture.addFileToProject(APPLICATION_PROPERTIES_FILE_NAME, "test.prop=2")

        @Language("kotlin") val text = """           
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CONDITIONAL_ON_PROPERTY}(name="test.prop", havingValue="1")                
            class TestConditional { }
            """.trimIndent()

        val psiFile = myFixture.configureByText("TestConditional.kt", text)

        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertTrue(allActiveBeans.isNotEmpty())
        TestCase.assertTrue(allActiveBeans.none { it.containingFile.name == psiFile.name })
    }

    fun testPropertyConditionHavingValueMissing() {
        @Language("kotlin") val text = """           
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CONDITIONAL_ON_PROPERTY}(name="test.prop", havingValue="1")                
            class TestConditional { }
            """.trimIndent()

        val psiFile = myFixture.configureByText("TestConditional.kt", text)

        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertTrue(allActiveBeans.isNotEmpty())
        TestCase.assertTrue(allActiveBeans.none { it.containingFile.name == psiFile.name })
    }
}