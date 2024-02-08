package com.esprito.spring.test

import com.intellij.codeInspection.GlobalInspectionTool
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.JavaInspectionTestCase
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestDataPath

private const val TEST_DATA_PATH = "testdata/inspection/"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
abstract class EspritoInspectionBaseTestCase : JavaInspectionTestCase() {

    open val languageLevel = LanguageLevel.JDK_17

    open val libraries: Array<TestLibrary> = arrayOf()

    open val realJdk: Boolean = false

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return EspritoProjectDescriptor()
    }

    fun doTest(tool: GlobalInspectionTool) {
        doTest(getTestName(true), tool)
    }

    fun doTest(tool: LocalInspectionTool) {
        doTest(getTestName(true), tool)
    }

    override fun getTestDataPath(): String {
        return TEST_DATA_PATH
    }

    protected inner class EspritoProjectDescriptor : ProjectDescriptor(languageLevel) {

        override fun getSdk(): Sdk? {
            return if (realJdk) {
                JavaSdk.getInstance().createJdk("TEST_JDK", IdeaTestUtil.requireRealJdkHome(), false)
            } else {
                super.getSdk()
            }
        }

        override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
            super.configureModule(module, model, contentEntry)

            libraries.forEach {
                addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies)
            }
        }
    }
}