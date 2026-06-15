/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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