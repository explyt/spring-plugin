/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
import com.intellij.openapi.application.AccessToken
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.JavaInspectionTestCase
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.TestDataPath

private const val TEST_DATA_PATH = "testdata/inspection/"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
abstract class ExplytInspectionBaseTestCase : JavaInspectionTestCase() {

    open val languageLevel = LanguageLevel.JDK_21

    open val libraries: Array<TestLibrary> = arrayOf()

    open val realJdk: Boolean = false

    private var loggedErrorProcessorToken: AccessToken? = null

    override fun setUp() {
        super.setUp()
        // 2026.2 Kotlin K2 folding builder logs "Invalid PSI Element: KtPackage" for
        // fully-qualified package references in Kotlin test sources (platform bug);
        // ignore it so highlighting tests don't fail on an unrelated folding pass
        loggedErrorProcessorToken = LoggedErrorProcessor.executeWith(object : LoggedErrorProcessor() {
            override fun processError(
                category: String, message: String, details: Array<String>, t: Throwable?
            ): Set<Action> {
                if (message.contains("Invalid PSI Element") && message.contains("KtPackage")) {
                    return setOf(Action.LOG)
                }
                return super.processError(category, message, details, t)
            }
        })
    }

    override fun tearDown() {
        try {
            loggedErrorProcessorToken?.finish()
            loggedErrorProcessorToken = null
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

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