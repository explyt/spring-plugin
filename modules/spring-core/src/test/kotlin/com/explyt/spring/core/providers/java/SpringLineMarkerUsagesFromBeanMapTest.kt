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
class SpringLineMarkerUsagesFromBeanMapTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testMapUsageTargetType() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """           
            import java.util.Map;
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                Map<String, B> mapB;                
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
        assertEquals(listOf("mapB"), gutterTargetString[0])
    }

    fun testMapUsageBaseType() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """           
            import java.util.Map;
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                Map<String, I> mapI;                
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
        assertEquals(listOf("mapI"), gutterTargetString[0])
    }

    fun testMapUsageNo() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """           
            import java.util.Map;
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                Map<String, I> mapI;                
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                public E e() { return new E(); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(0, gutterTargetString.size)
    }

    fun testMapUsageExtendsI() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """           
            import java.util.Map;
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                Map<String, ? extends I> mapI;                
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
        assertEquals(listOf("mapI"), gutterTargetString[0])
    }

    fun testMapUsageSuperB() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """           
            import java.util.Map;
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                Map<String, ? super B> mapB;                
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
        assertEquals(listOf("mapB"), gutterTargetString[0])
    }

    fun testMapOfListI() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """           
            import java.util.Map;
            import java.util.List;
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                Map<String, List<I>> mapOfListI;                
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """
            import java.util.List;
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                List<I> beanListI() { return List.of(new B()); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("mapOfListI"), gutterTargetString[0])
    }

    fun testMapOfListENo() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """           
            import java.util.Map;
            import java.util.List;
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                Map<String, List<I>> mapOfListI;                
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """
            import java.util.List;
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                List<E> beanList() { return List.of(new E()); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(0, gutterTargetString.size)
    }
}