package com.esprito.spring.core.providers


import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.util.SpringGutterTestUtil
import com.esprito.spring.test.EspritoJavaLightTestCase
import junit.framework.TestCase


class AutoConfigurationLineMarkerProviderTest : EspritoJavaLightTestCase() {

    fun testSpringFactories() {
        myFixture.addFileToProject(
            "META-INF/" + SpringProperties.SPRING_FACTORIES_FILE_NAME,
            "org.springframework.boot.autoconfigure.EnableAutoConfiguration=MyAutoConfig"
        )

        myFixture.configureByText(
            "MyAutoConfig.java",
            "@" + SpringCoreClasses.CONFIGURATION + " " +
                    "public class My<caret>AutoConfig {}"
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringFactories }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(
            listOf("org.springframework.boot.autoconfigure.EnableAutoConfiguration"),
            gutterTargetsStrings
        )
    }

    fun testSpringImports() {
        myFixture.addFileToProject(
            "META-INF/spring/" + SpringProperties.AUTOCONFIGURATION_IMPORTS, "MyAutoConfig"
        )

        myFixture.configureByText(
            "MyAutoConfig.java",
            "@" + SpringCoreClasses.CONFIGURATION + " " +
                    "public class My<caret>AutoConfig {}"
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringFactories }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(listOf("MyAutoConfig"), gutterTargetsStrings)
    }

}