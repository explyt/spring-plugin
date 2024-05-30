package com.esprito.spring.core.tracker.kotlin

import com.esprito.spring.core.action.UastModelTrackerInvalidateAction
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase


class EspritoModelModificationTrackerTest : LightJavaCodeInsightFixtureTestCase() {

    fun testAddNewAnnotationOnClass() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
            @<caret>
            class KotlinClass {}
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testRemoveAnnotationOnClass() {
        myFixture.configureByText(
            "KotlinClass.kt",
            "@A<caret> class KotlinClass {}"
        )
        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testEditAnnotationOnClass() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
            @SomeAnno<caret>
            class KotlinClass
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testAddNewAnnotationOnMethod() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """ class KotlinClass {
                @<caret>
                fun main() {}
            }"""
        )
        runModificationTriggeredTest()
    }

    fun testRemoveAnnotationOnMethod() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """ class KotlinClass {
                @A<caret>
                fun main() {}
            }"""
        )
        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testEditAnnotationOnMethod() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """ class KotlinClass {
              @SomeAnno("paramValue<caret>")
              fun method() {}
            }"""
        )
        runModificationTriggeredTest()
    }

    fun testAddAnnotationOnField() {
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

    fun testEditAnnotationOnField() {
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

    fun testRemoveAnnotationOnField() {
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

    fun testEditMethodName() {
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

    fun testEditConstructorName() {
        myFixture.configureByText(
            "KotlinClass.kt",
            " class  KotlinClass<caret>() { }"
        )
        runModificationTriggeredTest()
    }

    fun testEditConstructorParameterName() {
        myFixture.configureByText(
            "KotlinClass.kt",
            "class  KotlinClass(val param<caret> : String)"
        )
        runModificationTriggeredTest()
    }

    fun testEditMethodParameterName() {
        myFixture.configureByText(
            "KotlinClass.kt",
            " class  KotlinClass {  fun method(param<caret> : String ) {} }"
        )
        runModificationTriggeredTest()
    }

    /*fun testEditLocalVariableName() {
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

    fun testEditClassName() {
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

    fun testEditReturnType() {
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

    fun testEditReturnKeyword() {
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

    fun testInsertClass() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                class KotlinClass {
                    @Bean
                    fun getE(): E { return E() }
                }
                <caret>
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("class E {}") }
    }

    fun testRemoveClass() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                class KotlinClass {
                    @Bean
                    fun getE(): E { return E() }
                }
                
                class E{}<caret>
            """.trimMargin()
        )

        runModificationTriggeredTest { typeBackspaces(10) }
    }

    fun testInsertMethod() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                class KotlinClass {
                    <caret>   
                }                
                class E {}
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("@Bean fun getE(): E { return E() }") }
    }

    fun testRemoveMethod() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                class KotlinClass {
                         @Bean fun getE(): E { return E() }<caret>   
                }                
                class E {}
            """.trimMargin()
        )

        runModificationTriggeredTest { typeBackspaces(35) }
    }

    fun testCommentClass() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                class KotlinClass {
                       @Bean fun getE(): E { return E() }
                }                
                <caret>class E {}
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("//") }
    }

    fun testUncommentClass() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                class KotlinClass {
                       fun getE(): E { return E() }  
                }                
                //<caret>class E {}
            """.trimMargin()
        )

        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testCommentMethod() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                class KotlinClass {
                       <caret>@Bean fun getE(): E { return E() } 
                }                
                class E {}
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("//") }
    }

    fun testUncommentMethod() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                class KotlinClass {
                       //<caret>@Bean fun getE(): E { return E() }
                }                
                class E {}
            """.trimMargin()
        )

        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testCommentField() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                import org.springframework.context.annotation.Bean
                import org.springframework.context.annotation.Configuration
                import org.springframework.stereotype.Component
                import org.springframework.beans.factory.annotation.Autowired

                @Configuration
                class KotlinClass {                        
                    @Bean fun getE(): E { return E() }
                }                
                class E {}
                
                @Component
                class KotlinComponent {
                    <caret>@Autowired lateinit var service: E                       
                }
                
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("//") }
    }

    fun testUncommentField() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                import org.springframework.context.annotation.Bean
                import org.springframework.context.annotation.Configuration
                import org.springframework.stereotype.Component
                import org.springframework.beans.factory.annotation.Autowired

                @Configuration
                class KotlinClass {                        
                    @Bean fun getE(): E { return E() }
                }                
                class E {}
                
                @Component
                class KotlinComponent {
                    //<caret>@Autowired lateinit var service: E                       
                }
                
            """.trimMargin()
        )

        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testCommentFieldConstructor() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                import org.springframework.context.annotation.Bean
                import org.springframework.context.annotation.Configuration
                import org.springframework.stereotype.Component
                import org.springframework.beans.factory.annotation.Autowired

                @Configuration
                class KotlinClass {                        
                    @Bean fun getE(): E { return E() }
                }                
                class E {}
                
                @Component
                class KotlinComponent(
                    <caret>val service: E
                ) {}
                
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("//") }
    }

    fun testUncommentFieldConstructor() {
        myFixture.configureByText(
            "KotlinClass.kt",
            """
                import org.springframework.context.annotation.Bean
                import org.springframework.context.annotation.Configuration
                import org.springframework.stereotype.Component
                import org.springframework.beans.factory.annotation.Autowired

                @Configuration
                class KotlinClass {                        
                    @Bean fun getE(): E { return E() }
                }                
                class E {}
                
                @Component
                class KotlinComponent(
                    //<caret>val service: E
                ) {}
                
            """.trimMargin()
        )

        runModificationTriggeredTest { typeBackspaces(2) }
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