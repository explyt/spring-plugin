/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties.java

import com.explyt.spring.test.TestLibrary

class ProjectSpringPropertiesCompletionContributorTest : AbstractSpringPropertiesCompletionContributorTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBoot_3_1_1, TestLibrary.springContext_6_0_7)

    fun testAllProperties() {
        myFixture.copyFileToProject("ExternalSettings.java")
        myFixture.copyFileToProject("TestConfig.java")
        myFixture.configureByText("application.properties", "mail.<caret>")
        doTest(
            "mail.ert",
            "mail.external-settings",
            "mail.form-and-port",
            "mail.from",
            "mail.host-name",
            "mail.external-settings",
            "mail.external-settings.yet-property",
            "mail.external-settings.camel-case-long-property-very-long-property",
            "mail.nested-settings",
            "mail.nested-settings.another-nested-settings",
            "mail.nested-settings.another-nested-settings.camel-case-long-property-very-long-property",
            "mail.nested-settings.another-nested-settings.property2",
            "mail.nested-settings.another-nested-settings.property3",
            "mail.nested-settings.f1",
            "mail.new-property",
            "mail.port"
        )
    }

    fun testAllPropertiesExistedOne() {
        myFixture.copyFileToProject("ExternalSettings.java")
        myFixture.copyFileToProject("TestConfig.java")
        myFixture.configureByText("application.properties",
            """mail.from=me
            mail.<caret>"""".trimMargin())
        doTest(
            "mail.ert",
            "mail.external-settings",
            "mail.external-settings.yet-property",
            "mail.external-settings.camel-case-long-property-very-long-property",
            "mail.form-and-port",
            "mail.host-name",
            "mail.nested-settings",
            "mail.nested-settings.another-nested-settings",
            "mail.nested-settings.another-nested-settings.camel-case-long-property-very-long-property",
            "mail.nested-settings.another-nested-settings.property2",
            "mail.nested-settings.another-nested-settings.property3",
            "mail.nested-settings.f1",
            "mail.new-property",
            "mail.port"
        )
    }

    fun testAllPropertiesOneAvailable() {
        myFixture.copyFileToProject("ExternalSettings.java")
        myFixture.copyFileToProject("TestConfig.java")
        myFixture.configureByText("application.properties",
            """mail.from=me
            mail.ert=ert
            mail.external-settings=ext
            mail.external-settings.yet-property=yet
            mail.external-settings.camel-case-long-property-very-long-property=camel
            mail.form-and-port=port
            mail.host-name=name
            mail.nested-settings=nested
            mail.nested-settings.another-nested-settings=another-nested
            mail.nested-settings.another-nested-settings.camel-case-long-property-very-long-property=long
            mail.nested-settings.another-nested-settings.property2=p2
            mail.nested-settings.another-nested-settings.property3=p3
            mail.nested-settings.f1=f1
            mail.new-property=newp",
            mail.<caret>"""".trimMargin())
        doTest(
            "mail.port"
        )
    }

    fun testLoggingLevelOne() {
        myFixture.configureByText(
            "application.properties",
            """
logging.level.<caret>
            """.trimMargin()
        )
        doTest(
            "java", "org", "web", "sql", "root"
        )
    }

    fun testLoggingLevelSecond() {
        myFixture.configureByText(
            "application.properties",
            """
logging.level.org.<caret>
            """.trimMargin()
        )
        doTest(
            "aopalliance", "apache", "intellij", "jetbrains", "springframework"
        )
    }
}