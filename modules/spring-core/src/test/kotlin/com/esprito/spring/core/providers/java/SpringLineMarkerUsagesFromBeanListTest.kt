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
class SpringLineMarkerUsagesFromBeanListTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testListUsageTargetType() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.List;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<B> b;                
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

    fun testListUsageBaseType() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.List;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<I> i;                
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

    fun testListUsageNo() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.List;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<B> b;                
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

    fun testListOfListUsageBaseType() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.List;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<List<B>> listB;                
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """  
            import java.util.List;
            
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                public List<B> b() { return List.of(new B()); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("listB"), gutterTargetString[0])
    }

    fun testListOfListUsageNo() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.List;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<List<B>> listB;                
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """  
            import java.util.List;
            
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                public List<E> e() { return List.of(new E()); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(0, gutterTargetString.size)
    }

    fun testListCommonGeneric() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.List;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<?> list;                
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """     
            import java.util.*;
                               
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                List<I> beanListI() { return new ArrayList<>(); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("list"), gutterTargetString[0])
    }

    fun testListGenericExtendsI() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.List;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<? extends I> list;                
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
        assertEquals(listOf("list"), gutterTargetString[0])
    }

    fun testListGenericExtendsINo() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.List;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<? extends I> list;                
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

    fun testListGenericSuperB() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.List;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<? super B> list;                
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
        assertEquals(listOf("list"), gutterTargetString[0])
    }

    fun testListGenericSuperBNo() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.List;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<? super B> list;                
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

    fun testListOfMap() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.List;
            import java.util.Map;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<Map<String, I>> listOfMap;                
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """                        
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                public java.util.Map<String, I> beanMapStringI() { return new java.util.HashMap<>(); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("listOfMap"), gutterTargetString[0])
    }

    fun testListOfMapNo() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.List;
            import java.util.Map;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                List<Map<String, I>> listOfMap;                
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """                        
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                public java.util.Map<String, E> beanMapStringI() { return new java.util.HashMap<>(); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(0, gutterTargetString.size)
    }

    fun testCollectionBeanUsage() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.Collection;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}                        
                Collection<I> iCollection;
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """
            import java.util.ArrayList;
            import java.util.Collection;
                       
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                public Collection<I> collectionWithManyBeanI() { return new ArrayList<>(); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("iCollection"), gutterTargetString[0])
    }

    fun testSetBeanUsage() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.Collection;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}                        
                Collection<I> iCollection;
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """            
            import java.util.HashSet;
            import java.util.Set;
                       
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                public Set<I> collectionWithManyBeanI() { return new HashSet<>(); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("iCollection"), gutterTargetString[0])
    }

    fun testListBeanUsage() {
        myFixture.copyFileToProject("BeanUsagesClasses.java")
        myFixture.addClass(
            """            
            import java.util.Collection;
                
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}                        
                Collection<I> iCollection;
            }
        """.trimIndent()
        )

        @Language("JAVA") val text = """            
            import java.util.ArrayList;
            import java.util.List;
                       
            @${SpringCoreClasses.CONFIGURATION}
            public class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                public List<I> collectionWithManyBeanI() { return new ArrayList<>(); }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.java", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("iCollection"), gutterTargetString[0])
    }
}