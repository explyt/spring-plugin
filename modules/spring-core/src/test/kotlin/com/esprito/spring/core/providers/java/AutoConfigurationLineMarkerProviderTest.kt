package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.util.SpringGutterTestUtil
import junit.framework.TestCase

class AutoConfigurationLineMarkerProviderTest : ExplytJavaLightTestCase() {

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