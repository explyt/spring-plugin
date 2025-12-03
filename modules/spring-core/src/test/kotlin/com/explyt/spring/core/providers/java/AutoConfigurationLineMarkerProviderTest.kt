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


package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import junit.framework.TestCase

class AutoConfigurationLineMarkerProviderTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBoot_3_1_1, TestLibrary.springBootAutoConfigure_3_1_1
    )

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
