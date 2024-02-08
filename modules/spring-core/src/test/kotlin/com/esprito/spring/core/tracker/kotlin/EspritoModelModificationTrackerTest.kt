package com.esprito.spring.core.tracker.kotlin

import com.esprito.spring.core.action.UastModelTrackerInvalidateAction
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase


class EspritoModelModificationTrackerTest : LightJavaCodeInsightFixtureTestCase() {

    fun testJavaAddNewAnnotationOnClass() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
            @<caret>
            class KotlinClass {}
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testJavaRemoveAnnotationOnClass() {
        myFixture.configureByText(
            "KotlinClass.kt",
            "@A<caret> class KotlinClass {}"
        )
        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testJavaEditAnnotationOnClass() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
            @SomeAnno<caret>
            class KotlinClass
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testJavaAddNewAnnotationOnMethod() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """ class KotlinClass {
                @<caret>
                fun main() {}
            }"""
        )
        runModificationTriggeredTest()
    }

    fun testJavaRemoveAnnotationOnMethod() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """ class KotlinClass {
                @A<caret>
                fun main() {}
            }"""
        )
        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testJavaEditAnnotationOnMethod() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """ class KotlinClass {
              @SomeAnno("paramValue<caret>")
              fun method() {}
            }"""
        )
        runModificationTriggeredTest()
    }

    fun testJavaAddAnnotationOnField() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
             class KotlinClass { 
                @<caret>
                private val param: String?
            }
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testJavaEditAnnotationOnField() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
             class KotlinClass { 
                @SomeAnno("paramValue<caret>")
                private val param: String = "1"
            }
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testJavaRemoveAnnotationOnField() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
             class KotlinClass { 
                @A<caret>
                private val param ="1"
            }
            """.trimIndent()
        )
        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testJavaEditMethodName() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                 class KotlinClass {  
                     fun method<caret>(param: String) {}
                }
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testJavaEditConstructorName() {
        myFixture.configureByText(
            "KotlinClass.kt",
            " class  KotlinClass<caret>() { }"
        )
        runModificationTriggeredTest()
    }

    fun testJavaEditConstructorParameterName() {
        myFixture.configureByText(
            "KotlinClass.kt",
            "class  KotlinClass(val param<caret> : String)"
        )
        runModificationTriggeredTest()
    }

    fun testJavaEditMethodParameterName() {
        myFixture.configureByText(
            "KotlinClass.kt",
            " class  KotlinClass {  fun method(param<caret> : String ) {} }"
        )
        runModificationTriggeredTest()
    }

    /*fun testJavaEditLocalVariableName() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """ class KotlinClass {
                 fun method(String param) {
                    val s<caret> = "string"
                }
            }""".trimMargin()
        )
        runIsNotModifiedTest()
    }*/

    fun testJavaEditClassName() {
        myFixture.configureByText(
            "KotlinClass.kt",
            " class KotlinClass<caret> {}"
        )
        runModificationTriggeredTest()
    }

    fun testForceInvalidate() {
        myFixture.configureByText(
            "KotlinClass.kt",
            " class  KotlinClass {<caret>}"
        )
        runOuterModelModificationTrackerTest(true) { UastModelTrackerInvalidateAction.invalidate(project) }
    }

    fun testJavaEditReturnType() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """ class  KotlinClass { 
                 fun method(String param): String {
                    return <caret>
                }
            }""".trimMargin()
        )
        runModificationTriggeredTest()

        myFixture.configureByText(
            "KotlinClass.kt",
            """ class  KotlinClass { 
                 fun method(String param): String {
                    return <caret>
                }
            }""".trimMargin()
        )
        runModificationTriggeredTest { myFixture.type(" D()") }

        myFixture.configureByText(
            "KotlinClass.kt",
            """ class  KotlinClass { 
                 fun method(String param): String {
                    return D(<caret>
                }
            }""".trimMargin()
        )
        runModificationTriggeredTest { myFixture.type(")") }
    }

    fun testJavaEditReturnKeyword() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """ class  KotlinClass { 
                 fun method(String param): String {
                    <caret> "1"
                }
            }""".trimMargin()
        )
        runModificationTriggeredTest { myFixture.type("return") }

        myFixture.configureByText(
            "KotlinClass.kt",
            """ class  KotlinClass { 
                 fun method(String param): String {
                    retur<caret> "234"
                }
            }""".trimMargin()
        )
        runModificationTriggeredTest { myFixture.type("n") }
    }

    private fun runModificationTriggeredTest() {
        runModificationTriggeredTest { myFixture.type('A') }
    }

    private fun runModificationTriggeredTest(r: Runnable) {
        runOuterModelModificationTrackerTest(true, r)
    }

    private fun runIsNotModifiedTest() {
        runOuterModelModificationTrackerTest(false) { myFixture.type('A') }
    }

    private fun typeBackspaces(count: Int) {
        for (i in 0 until count) {
            LightPlatformCodeInsightTestCase.backspace(editor, project)
        }
    }

    private fun runOuterModelModificationTrackerTest(modified: Boolean, r: Runnable) {
        val tracker = ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
        val before = tracker.modificationCount
        r.run()
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val after = tracker.modificationCount
        if (modified) {
            assertTrue("$before < $after", before < after)
        } else {
            assertEquals(before, after)
        }
    }
}