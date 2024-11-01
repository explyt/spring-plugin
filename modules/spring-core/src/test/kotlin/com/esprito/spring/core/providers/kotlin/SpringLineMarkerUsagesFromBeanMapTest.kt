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
class SpringLineMarkerUsagesFromBeanMapTest : ExplytKotlinLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testMapUsageTargetType() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var mapB: Map<String, B>        
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
        assertEquals(listOf("mapB"), gutterTargetString[0])
    }

    fun testMapUsageBaseType() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var mapI: Map<String, I>        
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
        assertEquals(listOf("mapI"), gutterTargetString[0])
    }

    fun testMapUsageNo() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var mapI: Map<String, I>        
            }
        """.trimIndent()
        myFixture.createFile("TestUsages.kt", usage)

        @Language("kotlin") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                fun e(): E { return E() }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.kt", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(0, gutterTargetString.size)
    }

    fun testMapUsageExtendsI() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var mapI: Map<String, out I>        
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
        assertEquals(listOf("mapI"), gutterTargetString[0])
    }

    fun testMapOfListENo() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var mapI: Map<String, List<I>>        
            }
        """.trimIndent()
        myFixture.createFile("TestUsages.kt", usage)

        @Language("kotlin") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                fun beanList(): List<E> { return listOf(E()) }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.kt", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(0, gutterTargetString.size)
    }
}