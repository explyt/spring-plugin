/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

private const val TEST_DATA_PATH = "testdata/"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
abstract class ExplytBaseLightTestCase : LightJavaCodeInsightFixtureTestCase() {

    open val languageLevel = LanguageLevel.JDK_21

    open val libraries: Array<TestLibrary> = arrayOf()

    /** Whether the module gets the JDK the tests run on instead of the mock JDK, which lacks e.g. `java.net.http`. */
    open val realJdk: Boolean = false

    override fun getTestDataPath(): String {
        return TEST_DATA_PATH
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return ExplytProjectDescriptor()
    }

    protected inner class ExplytProjectDescriptor : ProjectDescriptor(languageLevel) {

        override fun getSdk(): Sdk? =
            if (realJdk) JavaSdk.getInstance().createJdk("TEST_JDK", IdeaTestUtil.requireRealJdkHome(), false)
            else super.getSdk()

        override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
            super.configureModule(module, model, contentEntry)

            libraries.forEach {
                addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies)
            }
        }
    }
}