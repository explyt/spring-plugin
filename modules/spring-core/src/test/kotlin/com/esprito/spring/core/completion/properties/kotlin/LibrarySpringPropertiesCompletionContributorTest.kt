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