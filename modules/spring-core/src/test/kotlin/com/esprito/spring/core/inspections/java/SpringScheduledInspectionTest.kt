package com.esprito.spring.core.inspections.java

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.inspections.SpringScheduledInspection
import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary

class SpringScheduledInspectionTest : EspritoInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringScheduledInspection::class.java)
    }
    fun testValidCron() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}     
            public class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(cron="* * * * * *")
                public void scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testValidFixedDelay() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}     
            public class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedDelay=1234)
                public void scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testValidFixedDelayString() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}     
            public class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedDelayString="1")
                public void scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testValidFixedRate() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}     
            public class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedRate=1234)
                public void scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testValidFixedRateString() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}     
            public class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedRateString="1")
                public void scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testEmptyScheduled() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}     
            public class SpringBean {
                <warning>@${SpringCoreClasses.SCHEDULED}()</warning>
                public void scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testInitDelayString() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}     
            public class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(initialDelayString="123", fixedRate=1)
                public void scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testInitDelay() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}     
            public class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(initialDelay=123, fixedRate=1)
                public void scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testInitValuesInvalid() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}     
            public class SpringBean {
                <warning>@${SpringCoreClasses.SCHEDULED}(initialDelay=12, initialDelayString="123", fixedRate=1)</warning>
                public void scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testValueFromConfig() {
        myFixture.addFileToProject("application.properties", "test.property=1")
        val propertyString = "\${test.property}"
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}     
            public class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedRateString="$propertyString")
                public void scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testValueFromConfigInvalid() {
        myFixture.addFileToProject("application.properties", "test.property=1a")
        val propertyString = "\${test.property}"
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}     
            public class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedRateString=<warning>"$propertyString"</warning>)
                public void scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testValueDefault() {
        val propertyString = "\${test.property:10}"
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}     
            public class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedRateString="$propertyString")
                public void scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }
}
