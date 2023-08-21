package com.esprito.spring.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

abstract class EspritoJavaLightTestCase : LightJavaCodeInsightFixtureTestCase() {

    open val languageLevel = LanguageLevel.JDK_17

    open val libraries: Array<TestLibrary> = arrayOf()

    override fun getTestDataPath(): String {
        return "testdata/"
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return EspritoProjectDescriptor()
    }

    protected inner class EspritoProjectDescriptor : ProjectDescriptor(languageLevel) {

        override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
            super.configureModule(module, model, contentEntry)

            libraries.forEach {
                addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies)
            }
        }
    }
}