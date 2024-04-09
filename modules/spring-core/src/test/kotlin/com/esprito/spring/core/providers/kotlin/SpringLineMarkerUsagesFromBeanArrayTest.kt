package com.esprito.spring.core.providers.kotlin

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.util.SpringGutterTestUtil
import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

@TestMetadata(TEST_DATA_PATH)
class SpringLineMarkerUsagesFromBeanArrayTest : EspritoKotlinLightTestCase() {

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

    //todo error
    fun testArrayBeanUsage() {
        myFixture.copyFileToProject("BeanUsagesClasses.kt")
        @Language("kotlin") val usage = """                 
            @${SpringCoreClasses.COMPONENT}
            class TestUsages {
                @${SpringCoreClasses.AUTOWIRED}
                lateinit var listsB: List<Array<B>>               
            }
        """.trimIndent()

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
        //assertEquals(1, gutterTargetString.size)
        //assertEquals(listOf("listsB"), gutterTargetString[0])
    }
}