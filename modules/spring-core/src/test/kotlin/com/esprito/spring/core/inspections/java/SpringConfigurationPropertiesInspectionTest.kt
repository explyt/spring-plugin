package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringConfigurationPropertiesInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringConfigurationPropertiesInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringConfigurationPropertiesInspection::class.java)
        myFixture.addClass(
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;

            @ConfigurationProperties(prefix = "abc")
            public class ConfigProperties { }
        """.trimIndent()
        )
    }

    fun testCorrectOnClass() {
        myFixture.configureByText(
            "TestClass.java",
            """
            @${SpringCoreClasses.CONFIGURATION_PROPERTIES}("prefix")            
            public class TestClass {}
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.java")
    }

    fun testCorrectOnMethod() {
        myFixture.configureByText(
            "TestClass.java",
            """                        
            public class TestClass {
                @${SpringCoreClasses.CONFIGURATION_PROPERTIES}("prefix")
                public Object method() { return "1";}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.java")
    }

    fun testEmptyOnClass() {
        myFixture.configureByText(
            "TestClass.java",
            """
            <warning>@${SpringCoreClasses.CONFIGURATION_PROPERTIES}()</warning>            
            public class TestClass {}
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.java")
    }

    fun testEmptyOnMethod() {
        myFixture.configureByText(
            "TestClass.java",
            """                        
            public class TestClass {
                <warning>@${SpringCoreClasses.CONFIGURATION_PROPERTIES}()</warning>
                public Object method() { return "1";}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.java")
    }

    fun testKebabOnClass() {
        myFixture.configureByText(
            "TestClass.java",
            """
            <warning>@${SpringCoreClasses.CONFIGURATION_PROPERTIES}("KEBAB")</warning>            
            public class TestClass {}
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.java")
    }

    fun testKebabOnMethod() {
        myFixture.configureByText(
            "TestClass.java",
            """                        
            public class TestClass {
                <warning>@${SpringCoreClasses.CONFIGURATION_PROPERTIES}("kebab_kebab")</warning>
                public Object method() { return "1";}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.java")
    }

    fun testDuplicateOnClass() {
        myFixture.configureByText(
            "TestClass.java",
            """
            <warning>@${SpringCoreClasses.CONFIGURATION_PROPERTIES}("abc")</warning>            
            public class TestClass {}
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.java")
    }

    fun testDuplicateOnMethod() {
        myFixture.configureByText(
            "TestClass.java",
            """                        
            public class TestClass {
                <warning>@${SpringCoreClasses.CONFIGURATION_PROPERTIES}("abc")</warning>
                public Object method() { return "1";}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.java")
    }
}
