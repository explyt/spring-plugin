package com.esprito.spring.core.reference

import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.testFramework.TestDataPath
import java.io.File

private const val TEST_DATA_PATH = "testdata/reference/file"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
class FileReferenceContributorTest : EspritoJavaLightTestCase() {

    override fun getTestDataPath(): String = TEST_DATA_PATH

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
            setOf("application2.properties", "application3.properties", "file:", "classpath:"),
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
}