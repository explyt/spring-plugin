package com.esprito.spring.core.providers


import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.util.SpringGutterTestUtil
import com.esprito.spring.test.EspritoJavaLightTestCase
import junit.framework.TestCase
import kotlin.io.path.Path


class AutoConfigurationLineMarkerProviderTest : EspritoJavaLightTestCase() {

    fun testSpringFactories() {
        myFixture.addFileToProject(
            Path("META-INF", SpringProperties.SPRING_FACTORIES_FILE_NAME).toString(),
            "org.springframework.boot.autoconfigure.EnableAutoConfiguration=MyAutoConfig"
        )

        myFixture.configureByText(
            "MyAutoConfig.java",
            "@" + SpringCoreClasses.CONFIGURATION + " " +
                    "public class My<caret>AutoConfig {}"
        );
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
            Path("META-INF", "spring", SpringProperties.AUTOCONFIGURATION_IMPORTS).toString(),
            "MyAutoConfig"
        );

        myFixture.configureByText(
            "MyAutoConfig.java",
            "@" + SpringCoreClasses.CONFIGURATION + " " +
                    "public class My<caret>AutoConfig {}"
        );
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringFactories }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(listOf("MyAutoConfig"), gutterTargetsStrings)
    }

}