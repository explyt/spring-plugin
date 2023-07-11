package com.esprito.spring.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.util.PathUtil
import java.io.File

abstract class EspritoJavaLightTestCase : LightJavaCodeInsightFixtureTestCase() {

    open val languageLevel = LanguageLevel.JDK_17

    open val libraries: Array<TestLibrary> = arrayOf()

    open fun getTestLibsPath(): String {
        return PathUtil.toSystemIndependentName(File("../test-framework/testlibs").canonicalPath)!!
    }

    override fun getTestDataPath(): String {
        return "testdata/"
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return EspritoProjectDescriptor()
    }

    protected inner class EspritoProjectDescriptor(): ProjectDescriptor(languageLevel) {

        override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
            super.configureModule(module, model, contentEntry)

            addLibraries(model)
        }

        private fun addLibraries(rootModel: ModifiableRootModel) = libraries.forEach {
            PsiTestUtil.addLibrary(rootModel, it.name, getTestLibsPath(), it.jar)
        }
    }
}