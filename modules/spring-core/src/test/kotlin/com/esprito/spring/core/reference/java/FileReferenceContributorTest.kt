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

package com.explyt.spring.core.reference.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import java.io.File

class FileReferenceContributorTest : ExplytJavaLightTestCase() {
    private val basicPrefix = setOf("file:", "classpath:", "classpath*:", "http:")

    override fun getTestDataPath(): String = super.getTestDataPath() + "reference/file"

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testPropertySourceMultiResolve() {
        myFixture.copyDirectoryToProject("properties", "")
        val vf = myFixture.copyFileToProject(
            "TestPropertySourceMultiResolve.java",
            "com/example/demo/TestPropertySourceMultiResolve.java"
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
        val sourceFile = File(testDataPath, FileUtil.toSystemDependentName("TestPropertySourceReferences.java"))
        var text = sourceFile.readText()

        text = text.replace("#base_dir", "", false)

        myFixture.configureByText("TestPropertySourceReferences.java", text)

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
            "TestClass.java",
            """                
                public class TestClass {
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
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }

    fun testResourceUtilsResolve() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                public class TestClass {
                    public static void main(String[] args) {
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
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }

    fun testGetResourceResolve() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                public class TestClass {
                    public static void main(String[] args) {
                        org.springframework.context.ApplicationContext context = null;
                        context.getResource("<caret>");                        
                    }                                      
                }
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }

    fun testImportResourceDefault() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                @${SpringCoreClasses.IMPORT_RESOURCE}("<caret>")
                public class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }

    fun testImportResourceLocations() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                @${SpringCoreClasses.IMPORT_RESOURCE}(locations="<caret>")
                public class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }

    fun testContextConfigurationDefault() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                @${SpringCoreClasses.CONTEXT_CONFIGURATION}("<caret>")
                public class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }

    fun testContextConfigurationLocations() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                @${SpringCoreClasses.CONTEXT_CONFIGURATION}(locations="<caret>")
                public class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }

    fun testTestPropertySourceLocations() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                @${SpringCoreClasses.TEST_PROPERTY_SOURCE}(locations="<caret>")
                public class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }

    fun testTestPropertySourceDefault() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                @${SpringCoreClasses.TEST_PROPERTY_SOURCE}("<caret>")
                public class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }

    fun testPropertySourceDefault() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                @${SpringCoreClasses.PROPERTY_SOURCE}("<caret>")
                public class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }

    fun testSQLDefault() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                @${SpringCoreClasses.CONTEXT_SQL}("<caret>")
                public class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }

    fun testSQLScripts() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                @${SpringCoreClasses.CONTEXT_SQL}(scripts="<caret>")
                public class TestClass {}
            """.trimIndent()
        )

        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }

    fun testConstructorClassPathResourceVariable() {
        myFixture.configureByText(
            "TestClass.java",
            """           
                import org.springframework.core.io.ClassPathResource;
                
                @Service
                public class TestClass {
                    ClassPathResource path = new ClassPathResource("<caret>");
                }
            """.trimIndent()
        )

        assertVariants()
    }

    fun testConstructorUrlResourceVariable() {
        myFixture.configureByText(
            "TestClass.java",
            """
                import org.springframework.core.io.UrlResource;
                
                @Service
                public class TestClass {
                    UrlResource path = new UrlResource("<caret>");
                }
            """.trimIndent()
        )

        assertVariants()
    }

    fun testConstructorFileUrlResourceVariable() {
        myFixture.configureByText(
            "TestClass.java",
            """
                import org.springframework.core.io.FileUrlResource;
                
                @Service
                public class TestClass {
                    FileUrlResource path = new FileUrlResource("<caret>");
                }
            """.trimIndent()
        )

        assertVariants()
    }

    fun testGetResourceClassResolve() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                import java.net.URL;
                import org.springframework.stereotype.Service;
                import jakarta.annotation.PostConstruct;
                
                @Service
                public class TestClass {
                    @PostConstruct
                    public void init() {
                        URL credentialsResource = TestClass.class.getResource("<caret>");
                    }
                }
            """.trimIndent()
        )

        assertVariants()
    }

    fun testGetResourceClassLoaderResolve() {
        myFixture.configureByText(
            "TestClass.java",
            """                
                import java.net.URL;
                import org.springframework.stereotype.Service;
                import jakarta.annotation.PostConstruct;
                
                @Service
                public class TestClass {
                    @PostConstruct
                    public void init() {
                        URL credentialsResource = TestClass.class.getClassLoader().getResource("<caret>");
                    }
                }
            """.trimIndent()
        )

        assertVariants()
    }

    private fun assertVariants() {
        val variants = myFixture.getReferenceAtCaretPosition()?.variants ?: emptyArray()
        val variantSet = variants.asSequence()
            .filterIsInstance(LookupElement::class.java)
            .map { it.lookupString }
            .toSet()
        assertEquals(setOf("TestClass.java") + basicPrefix, variantSet)
    }


}