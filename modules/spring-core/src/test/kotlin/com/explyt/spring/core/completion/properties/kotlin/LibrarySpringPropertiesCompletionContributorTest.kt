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

package com.explyt.spring.core.completion.properties.kotlin


import com.explyt.spring.test.TestLibrary

class LibrarySpringPropertiesCompletionContributorTest : AbstractSpringPropertiesCompletionContributorTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7)

    fun testVariantsViaDot() {
        myFixture.configureByText("application.properties", "sp.da.tom.nam<caret>")
        doTest(
            "spring.datasource.tomcat.name",
            "spring.datasource.tomcat.driver-class-name",
            "spring.datasource.tomcat.validator-class-name"
        )
    }

    fun testVariantsWithoutDot() {
        myFixture.configureByText("application.properties", "tomenc<caret>")
        doTest("server.tomcat.accesslog.encoding", "server.tomcat.uri-encoding")
    }

    fun testVariantsFromConfigDir() {
        myFixture.configureFromExistingVirtualFile(
            myFixture.copyFileToProject("my-config.properties", "config/my-config.properties")
        )
        doTest(
            "logging.level",
            "logging.level.root",
            "logging.level.sql",
            "logging.level.web",
            "logging.pattern.level"
        )
    }

    fun testVariantsWithProfile() {
        myFixture.configureByText("application-prod.properties", "ert.ac.en<caret>")
        doTest("server.undertow.accesslog.enabled")
    }

    fun testVariantsViaPropertySource() {
        myFixture.copyFileToProject("MyConfig.kt")
        myFixture.configureFromExistingVirtualFile(
            myFixture.copyFileToProject("my-config.properties", "configuration-dir/abc.properties")
        )
        doTest(
            "logging.level",
            "logging.level.root",
            "logging.level.sql",
            "logging.level.web",
            "logging.pattern.level"
        )
    }
}