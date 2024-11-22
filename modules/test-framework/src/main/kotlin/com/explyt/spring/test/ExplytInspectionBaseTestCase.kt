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

package com.explyt.spring.test

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
abstract class ExplytInspectionBaseTestCase : JavaInspectionTestCase() {

    open val languageLevel = LanguageLevel.JDK_21

    open val libraries: Array<TestLibrary> = arrayOf()

    open val realJdk: Boolean = false

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return ExplytProjectDescriptor()
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

    protected inner class ExplytProjectDescriptor : ProjectDescriptor(languageLevel) {

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