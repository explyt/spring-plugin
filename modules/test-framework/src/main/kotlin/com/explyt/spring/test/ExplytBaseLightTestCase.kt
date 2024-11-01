package com.explyt.spring.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

private const val TEST_DATA_PATH = "testdata/"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
abstract class ExplytBaseLightTestCase : LightJavaCodeInsightFixtureTestCase() {

    open val languageLevel = LanguageLevel.JDK_21

    open val libraries: Array<TestLibrary> = arrayOf()

    override fun getTestDataPath(): String {
        return TEST_DATA_PATH
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return ExplytProjectDescriptor()
    }

    protected inner class ExplytProjectDescriptor : ProjectDescriptor(languageLevel) {

        override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
            super.configureModule(module, model, contentEntry)

            libraries.forEach {
                addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies)
            }
        }
    }
}