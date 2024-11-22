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
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

@TestMetadata(TEST_DATA_PATH)
class SpringLineMarkerUsagesFromBeanArrayTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testArrayUsageTargetType() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """                                    
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                B[] b;                
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                public B b() { return new B(); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("b"), gutterTargetString[0])
    }

    fun testArrayUsageBaseType() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """                                    
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                I[] i;                
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                public B b() { return new B(); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("i"), gutterTargetString[0])
    }

    fun testArrayBeanUsage() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """                                    
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                B[] listsB;               
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                B[] carr() { return new B[0]; }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("listsB"), gutterTargetString[0])
    }

    fun testListOfArrayBeanUsage() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """                
            import java.util.List;
            
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<B[]> listsB;               
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                B[] carr() { return new B[0]; }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("listsB"), gutterTargetString[0])
    }
}