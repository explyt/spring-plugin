package com.esprito.spring.test

import com.intellij.codeInspection.GlobalInspectionTool
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.JavaInspectionTestCase
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PsiTestUtil
import com.intellij.util.PathUtil
import java.io.File

abstract class EspritoInspectionTestCase : JavaInspectionTestCase() {

    open val languageLevel = LanguageLevel.JDK_17

    open val libraries: Array<TestLibrary> = arrayOf()

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return EspritoProjectDescriptor()
    }

    fun doTest(tool: GlobalInspectionTool) {
        doTest(getTestName(true), tool)
    }

    fun doTest(tool: LocalInspectionTool) {
        doTest(getTestName(true), tool)
    }

    protected inner class EspritoProjectDescriptor: ProjectDescriptor(languageLevel) {

        override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
            super.configureModule(module, model, contentEntry)

            libraries.forEach {
                addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies)
            }
        }
    }
}