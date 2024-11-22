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

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import java.io.File

class FileReferenceContributorTest : ExplytKotlinLightTestCase() {
    private val basicPrefix = setOf("file:", "classpath:", "classpath*:", "http:")

    override fun getTestDataPath(): String = super.getTestDataPath() + "reference/file"

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springTest_6_0_7)

    fun testPropertySourceMultiResolve() {
        myFixture.copyDirectoryToProject("properties", "")
        val vf = myFixture.copyFileToProject(
            "TestPropertySourceMultiResolve.kt",
            "com/example/demo/TestPropertySourceMultiResolve.kt"
        )

        myFixture.configureFromExistingVirtualFile(vf)

        val ref = myFixture.getReferenceAtCaretPosition()

        assertEquals(
            setOf("application2.properties", "application3.properties") + basicPrefix,
            ref?.variants?.map { (it as LookupElement).lookupString }?.toSet(),
        )
    }

    fun referencesToFile(fileName: String): List<PsiReference> {
        val findFileInTempDir = myFixture.findFileInTempDir(fileName) ?: return emptyList()
        val findFile = PsiManager.getInstance(project).findFile(findFileInTempDir) ?: return emptyList()
        return ReferencesSearch.search(findFile).findAll().toList()
    }

    fun referencesToDir(fileName: String): List<PsiReference> {
        val findFileInTempDir = myFixture.findFileInTempDir(fileName) ?: return emptyList()
        val findDir = PsiManager.getInstance(project).findDirectory(findFileInTempDir) ?: return emptyList()
        return ReferencesSearch.search(findDir).findAll().toList()
    }

    fun testPropertySourceReferences() {
        myFixture.copyDirectoryToProject("properties", "")

        val testDataPath = testDataPath
        val sourceFile = File(testDataPath, FileUtil.toSystemDependentName("TestPropertySourceReferences.kt"))
        var text = sourceFile.readText()

        text = text.replace("#base_dir", "", false)

        myFixture.configureByText("TestPropertySourceReferences.kt", text)

        setOf(
            "1/application2.properties",
            "1/application3.properties",
            "2/abc/application4.properties",
            "2/def/application5.properties",
            "3/application6.properties",
            "3/application7.properties",
            "4/application8.properties",
            "4/application9.properties"
        ).forEach {
            assertEquals(it, 1, referencesToFile(it).size)
        }

        assertEquals("dir \"1\"", 2, referencesToDir("1").size)
        assertEquals("dir \"2\"", 2, referencesToDir("2").size)
        assertEquals("dir \"2/abc\"", 1, referencesToDir("2/abc").size)
        assertEquals("dir \"2/def\"", 1, referencesToDir("2/def").size)
        assertEquals("dir \"3\"", 3, referencesToDir("3").size)
        assertEquals("dir \"4\"", 2, referencesToDir("4").size)

        setOf(
            "4/application10.properties",
            "4/application11.properties"
        ).forEach {
            assertEquals(it, 0, referencesToFile(it).size)
        }
    }

    fun testValueAnnotationResolve() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                open class TestClass {
                    @${SpringCoreClasses.VALUE}("<caret>")
                    String field;                                        
                }
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }

    fun testResourceUtilsResolve() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                object TestClass {
                    @JvmStatic
                    fun main(args: Array<String>) {
                        org.springframework.util.ResourceUtils.getFile("<caret>")
                    }
                }
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }

    fun testGetResourceResolve() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                object TestClass {
                    @JvmStatic
                    fun main(args: Array<String>) {
                        val context: org.springframework.context.ApplicationContext? = null
                        context!!.getResource("<caret>")
                    }
                }
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }

    fun testImportResourceDefault() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                @${SpringCoreClasses.IMPORT_RESOURCE}("<caret>")
                open class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }

    fun testImportResourceLocations() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                @${SpringCoreClasses.IMPORT_RESOURCE}(locations="<caret>")
                open class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }

    fun testContextConfigurationDefault() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                @${SpringCoreClasses.CONTEXT_CONFIGURATION}("<caret>")
                open class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }

    fun testContextConfigurationLocations() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                @${SpringCoreClasses.CONTEXT_CONFIGURATION}(locations="<caret>")
                open class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }

    fun testTestPropertySourceLocations() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                @${SpringCoreClasses.TEST_PROPERTY_SOURCE}(locations="<caret>")
                open class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }

    fun testTestPropertySourceDefault() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                @${SpringCoreClasses.TEST_PROPERTY_SOURCE}("<caret>")
                open class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }

    fun testPropertySourceDefault() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                @${SpringCoreClasses.PROPERTY_SOURCE}("<caret>")
                open class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }

    fun testSQLDefault() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                @${SpringCoreClasses.CONTEXT_SQL}("<caret>")
                open class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }

    fun testSQLScripts() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                @${SpringCoreClasses.CONTEXT_SQL}(scripts="<caret>")
                open class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }

    fun testConstructorClassPathResourceVariable() {
        myFixture.configureByText(
            "TestClass.kt",
            """           
                class TestClass {
                    val path = org.springframework.core.io.ClassPathResource("<caret>")
                }
            """.trimIndent()
        )

        assertVariants()
    }

    fun testConstructorUrlResourceVariable() {
        myFixture.configureByText(
            "TestClass.kt",
            """
                class TestClass {
                    val path = org.springframework.core.io.UrlResource("<caret>")
                }
            """.trimIndent()
        )

        assertVariants()
    }

    fun testConstructorFileUrlResourceVariable() {
        myFixture.configureByText(
            "TestClass.kt",
            """
                class TestClass {
                    val path = org.springframework.core.io.FileUrlResource("<caret>");
                }
            """.trimIndent()
        )

        assertVariants()
    }

    fun testGetResourceClassResolve() {
        myFixture.configureByText(
            "TestClass.kt",
            """        
                import kotlin.jvm
                open class TestClass {
                    fun getResource() {
                        TestClass::class.java.getResource("<caret>")
                    }
                }
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt"), variantSet)
    }

    fun testGetResourceClassLoaderResolve() {
        myFixture.configureByText(
            "TestClass.kt",
            """                
                object TestClass {
                    fun getResource() {
                        val url = TestClass::class.java.classLoader.getResource("<caret>")
                    }
                }
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt"), variantSet)
    }

    private fun assertVariants() {
        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.kt") + basicPrefix, variantSet)
    }
}