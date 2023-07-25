package com.esprito.spring.core.completion.properties

import com.esprito.spring.test.TestLibrary

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
}