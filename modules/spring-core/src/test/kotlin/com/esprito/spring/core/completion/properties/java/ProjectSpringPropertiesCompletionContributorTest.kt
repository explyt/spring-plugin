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
}