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

package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

@TestMetadata(TEST_DATA_PATH)
class SpringLineMarkerUsagesFromBeanArrayTest : ExplytKotlinLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testArrayUsageTargetType() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var b: Array<B>               
            }
        """.trimIndent()
        myFixture.createFile("TestUsages.kt", usage)

        @Language("kotlin") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                fun b(): B { return B() }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.kt", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("b"), gutterTargetString[0])
    }

    fun testArrayUsageBaseType() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var i: Array<I>               
            }
        """.trimIndent()
        myFixture.createFile("TestUsages.kt", usage)

        @Language("kotlin") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                fun b(): B { return B() }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.kt", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("i"), gutterTargetString[0])
    }

    fun testArrayBeanUsage() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var listsB: Array<B>               
            }
        """.trimIndent()
        myFixture.createFile("TestUsages.kt", usage)

        @Language("kotlin") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                fun carr(): Array<B> { return arrayOf(B()) }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.kt", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("listsB"), gutterTargetString[0])
    }

    fun testListOfArrayBeanUsage() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var listsB: List<Array<B>>               
            }
        """.trimIndent()
        myFixture.createFile("TestUsages.kt", usage)

        @Language("kotlin") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                fun carr(): Array<B> { return arrayOf(B()) }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.kt", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("listsB"), gutterTargetString[0])
    }
}