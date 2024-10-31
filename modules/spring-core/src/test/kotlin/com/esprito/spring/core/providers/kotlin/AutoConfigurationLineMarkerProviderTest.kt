package com.esprito.spring.core.providers.kotlin


import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.test.ExplytKotlinLightTestCase
import com.esprito.spring.test.util.SpringGutterTestUtil
import junit.framework.TestCase


class AutoConfigurationLineMarkerProviderTest : ExplytKotlinLightTestCase() {

    fun testSpringFactories() {
        myFixture.addFileToProject(
            "META-INF/" + SpringProperties.SPRING_FACTORIES_FILE_NAME,
            "org.springframework.boot.autoconfigure.EnableAutoConfiguration=MyAutoConfig"
        )

        myFixture.configureByText(
            "MyAutoConfig.kt",
            "@" + SpringCoreClasses.CONFIGURATION + " " +
                    "open class My<caret>AutoConfig {}"
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
            "MyAutoConfig.kt",
            "@" + SpringCoreClasses.CONFIGURATION + " " +
                    "open class My<caret>AutoConfig {}"
        )
        val gutterMarks = myFixture.findGuttersAtCaret()
        val gutterMark = gutterMarks.find { it.icon == SpringIcons.SpringFactories }
        TestCase.assertNotNull(gutterMark)
        val gutterTargetsStrings = SpringGutterTestUtil.getGutterTargetsStrings(gutterMark)
        TestCase.assertEquals(listOf("MyAutoConfig"), gutterTargetsStrings)
    }

}