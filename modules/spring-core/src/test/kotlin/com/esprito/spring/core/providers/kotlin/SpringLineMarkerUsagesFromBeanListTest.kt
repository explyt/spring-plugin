package com.esprito.spring.core.providers.kotlin

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.test.util.SpringGutterTestUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

@TestMetadata(TEST_DATA_PATH)
class SpringLineMarkerUsagesFromBeanListTest : EspritoKotlinLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testListUsageTargetType() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var b: List<B>               
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

    fun testListUsageBaseType() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")

        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var i: List<I>               
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

    fun testListUsageNo() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var b: List<B>               
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

    fun testListOfListUsageBaseType() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var listB: List<List<B>>               
            }
        """.trimIndent()
        myFixture.createFile("TestUsages.kt", usage)

        @Language("kotlin") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                fun b(): List<B> { return listOf(B()) }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.kt", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("listB"), gutterTargetString[0])
    }

    fun testListOfListUsageNo() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var listB: List<List<B>>               
            }
        """.trimIndent()
        myFixture.createFile("TestUsages.kt", usage)

        @Language("kotlin") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                fun e(): List<E> { return listOf(E()) }
            }
            """.trimIndent()
        myFixture.configureByText("TestMarker.kt", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(0, gutterTargetString.size)
    }

    fun testListCommonGeneric() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")

        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var list: List<*>               
            }
        """.trimIndent()
        myFixture.createFile("TestUsages.kt", usage)

        @Language("kotlin") val text = """           
            @${SpringCoreClasses.CONFIGURATION}
            class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                open fun beanListI(): List<I> { return emptyList() }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.kt", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("list"), gutterTargetString[0])
    }

    fun testListGenericExtendsI() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var list: List<out I>               
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
        assertEquals(listOf("list"), gutterTargetString[0])
    }

    fun testListGenericExtendsINo() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var list: List<out I>               
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

    fun testListGenericSuperB() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var list: MutableList<in B>               
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
        assertEquals(listOf("list"), gutterTargetString[0])
    }

    fun testListGenericSuperBNo() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var list: List<B>               
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

    fun testCollectionBeanUsage() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var iCollection: Collection<I>               
            }
        """.trimIndent()
        myFixture.createFile("TestUsages.kt", usage)

        @Language("kotlin") val text = """           
            import java.util.*

            @${SpringCoreClasses.CONFIGURATION}
            class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                fun e(): Collection<I> { return ArrayList() }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.kt", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("iCollection"), gutterTargetString[0])
    }

    fun testSetBeanUsage() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var iCollection: Collection<I>               
            }
        """.trimIndent()
        myFixture.createFile("TestUsages.kt", usage)

        @Language("kotlin") val text = """           
            import java.util.*

            @${SpringCoreClasses.CONFIGURATION}
            class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                fun e(): Set<I> { return HashSet() }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.kt", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("iCollection"), gutterTargetString[0])
    }

    fun testListBeanUsage() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var iCollection: Collection<I>               
            }
        """.trimIndent()
        myFixture.createFile("TestUsages.kt", usage)

        @Language("kotlin") val text = """           
            import java.util.*

            @${SpringCoreClasses.CONFIGURATION}
            class TestMarker {                               
                @${SpringCoreClasses.BEAN}
                fun e(): List<I> { return ArrayList() }
            }
            """.trimIndent()

        myFixture.configureByText("TestMarker.kt", text)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)
        assertEquals(1, gutterTargetString.size)
        assertEquals(listOf("iCollection"), gutterTargetString[0])
    }
}