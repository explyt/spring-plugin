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

package com.explyt.spring.core.tracker.java

import com.explyt.spring.core.action.UastModelTrackerInvalidateAction
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import java.io.IOException


class ExplytModelModificationTrackerTest : LightJavaCodeInsightFixtureTestCase() {

    @Throws(IOException::class)
    fun testMoveFile() {
        myFixture.configureByText("anything.txt", "")
        myFixture.tempDirFixture.findOrCreateDir("subDir")
        runModificationTriggeredTest { myFixture.moveFile("anything.txt", "/subDir") }
    }

    fun testCreateFile() {
        runModificationTriggeredTest { myFixture.addClass("public class Test{}") }
    }

    fun testPropertiesFileAnyChange() {
        myFixture.configureByText("myProps.properties", "")
        runModificationTriggeredTest()
    }

    fun testJavaAddNewAnnotationOnClass() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
            @<caret>
            public class MyConfiguration {}
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testJavaRemoveAnnotationOnClass() {
        myFixture.configureByText(
            "MyConfiguration.java",
            "@A<caret> public class MyConfiguration {}"
        )
        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testJavaEditAnnotationOnClass() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
            @SomeAnno<caret>
            public class MyConfiguration {}
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testJavaAddNewAnnotationOnMethod() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """public class MyConfiguration {
                @<caret>
                public void main() {}
            }"""
        )
        runModificationTriggeredTest()
    }

    fun testJavaRemoveAnnotationOnMethod() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """public class MyConfiguration {
                @A<caret>
                public void main() {}
            }"""
        )
        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testJavaEditAnnotationOnMethod() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """public class MyConfiguration 
              @SomeAnno("paramValue<caret>")
              public void method() {}
            }"""
        )
        runModificationTriggeredTest()
    }

    fun testJavaAddAnnotationOnField() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
            public class MyConfiguration { 
                @<caret>
                private String param;
            }
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testJavaEditAnnotationOnField() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
            public class MyConfiguration { 
                @SomeAnno("paramValue<caret>")
                private String param;
            }
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testJavaRemoveAnnotationOnField() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
            public class MyConfiguration { 
                @A<caret>
                private String param;
            }
            """.trimIndent()
        )
        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testJavaEditAnnotationOnMethodParameter() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                public class MyConfiguration {  
                    public void method(@SomeAnno("paramValue<caret>") String param) {}
                }
            """.trimIndent()
        )
        runIsNotModifiedTest()
    }

    fun testJavaAddInnerClass() {
        myFixture.configureByText(
            "MyConfiguration.java",
            "public class MyConfiguration {<caret>}"
        )
        runModificationTriggeredTest {
            myFixture.type("public static ")
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            myFixture.type("class Inner {}")
        }
    }

    fun testJavaEditMethodName() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                public class MyConfiguration {  
                    public void method<caret>(String param) {}
                }
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testJavaEditConstructorName() {
        myFixture.configureByText(
            "MyConfiguration.java",
            "public class MyConfiguration { MyConfiguration<caret>() {} }"
        )
        runModificationTriggeredTest()
    }

    fun testJavaEditConstructorParameterName() {
        myFixture.configureByText(
            "MyConfiguration.java",
            "public class MyConfiguration { MyConfiguration(String param<caret>) {} }"
        )
        runModificationTriggeredTest()
    }

    fun testJavaEditFieldName() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                public class MyConfiguration {  
                    private String param<caret>;                    
                }
            """.trimIndent()
        )
        runModificationTriggeredTest()
    }

    fun testJavaEditMethodParameterName() {
        myFixture.configureByText(
            "MyConfiguration.java",
            "public class MyConfiguration { public void method(String param<caret>) {} }"
        )
        runModificationTriggeredTest()
    }

    fun testJavaEditLocalVariableName() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """public class MyConfiguration { 
                public void method(String param) {
                    var s<caret> = "string"
                }
            }""".trimMargin()
        )
        runIsNotModifiedTest()
    }

    fun testJavaEditClassName() {
        myFixture.configureByText(
            "MyConfiguration.java",
            "public class MyConfiguration<caret> {}"
        )
        runModificationTriggeredTest()
    }

    fun testForceInvalidate() {
        myFixture.configureByText(
            "MyConfiguration.java",
            "public class MyConfiguration {<caret>}"
        )
        runOuterModelModificationTrackerTest(true) { UastModelTrackerInvalidateAction.invalidate(project) }
    }

    fun testJavaStartInputParameterName() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """public class MyConfiguration { 
                public void method(String <caret>) {                    
                }
            }""".trimMargin()
        )
        runModificationTriggeredTest()
    }

    fun testJavaEditReturnType() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """public class MyConfiguration { 
                public void method(String param) {
                    return <caret>
                }
            }""".trimMargin()
        )
        runModificationTriggeredTest()

        myFixture.configureByText(
            "MyConfiguration.java",
            """public class MyConfiguration { 
                public void method(String param) {
                    return <caret>
                }
            }""".trimMargin()
        )
        runModificationTriggeredTest { myFixture.type("new D()") }

        myFixture.configureByText(
            "MyConfiguration.java",
            """public class MyConfiguration { 
                public void method(String param) {
                    return new D(<caret>
                }
            }""".trimMargin()
        )
        runModificationTriggeredTest { myFixture.type(")") }

        myFixture.configureByText(
            "MyConfiguration.java",
            """public class MyConfiguration { 
                public void method(String param) {
                    return new D()<caret>
                }
            }""".trimMargin()
        )
        runModificationTriggeredTest { myFixture.type(";") }
    }

    fun testJavaEditReturnKeyword() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """public class MyConfiguration { 
                public void method(String param) {
                    <caret> new D();
                }
            }""".trimMargin()
        )
        runModificationTriggeredTest { myFixture.type("return") }

        myFixture.configureByText(
            "MyConfiguration.java",
            """public class MyConfiguration { 
                public void method(String param) {
                    retur<caret> new D();
                }
            }""".trimMargin()
        )
        runModificationTriggeredTest { myFixture.type("n") }
    }

    fun testJavaInsertClass() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class MyConfiguration {
                    @Bean
                    public E getE() {   return new E();   }
                }
                <caret>
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("class E {}") }
    }

    fun testJavaRemoveClass() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class MyConfiguration {
                    @Bean
                    public E getE() {   return new E();   }
                }
                
                class E{}<caret>
            """.trimMargin()
        )

        runModificationTriggeredTest { typeBackspaces(10) }
    }

    fun testJavaInsertMethod() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class MyConfiguration {
                    <caret>   
                }                
                class E {}
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("@Bean public E getE() {return new E();}") }
    }

    fun testJavaRemoveMethod() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class MyConfiguration {
                       @Bean public E getE() {return new E();}<caret>   
                }                
                class E {}
            """.trimMargin()
        )

        runModificationTriggeredTest { typeBackspaces(40) }
    }

    fun testJavaCommentClass() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class MyConfiguration {
                       @Bean public E getE() {return new E();}   
                }                
                <caret>class E {}
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("//") }
    }

    fun testJavaUncommentClass() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class MyConfiguration {
                       @Bean public E getE() {return new E();}   
                }                
                //<caret>class E {}
            """.trimMargin()
        )

        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testJavaCommentMethod() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class MyConfiguration {
                       <caret>@Bean public E getE() {return new E();}   
                }                
                class E {}
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("//") }
    }

    fun testJavaUncommentMethod() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class MyConfiguration {
                       //<caret>@Bean public E getE() {return new E();}   
                }                
                class E {}
            """.trimMargin()
        )

        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testJavaCommentField() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.stereotype.Component;
                import org.springframework.beans.factory.annotation.Autowired;

                @Configuration
                public class MyConfiguration {
                       @Bean public E getE() {return new E();}   
                }                
                class E {}
                
                @Component
                class JavaComponent {
                    <caret>@Autowired E service;                       
                }
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("//") }
    }

    fun testJavaUncommentField() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.stereotype.Component;
                import org.springframework.beans.factory.annotation.Autowired;

                @Configuration
                public class MyConfiguration {
                       @Bean public E getE() {return new E();}   
                }                
                class E {}
                
                @Component
                class JavaComponent {
                    //<caret>@Autowired E service;                       
                }
            """.trimMargin()
        )

        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testJavaCommentFieldConstructor() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.stereotype.Component;
                import org.springframework.beans.factory.annotation.Autowired;

                @Configuration
                public class MyConfiguration {
                       @Bean public E getE() {return new E();}   
                }                
                class E {}
                
                @Component
                class JavaComponent {
                   public JavaComponent(
                    <caret>E service
                   ) {}                                           
                }
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("//") }
    }

    fun testJavaUncommentFieldConstructor() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.stereotype.Component;
                import org.springframework.beans.factory.annotation.Autowired;

                @Configuration
                public class MyConfiguration {
                       @Bean public E getE() {return new E();}   
                }                
                class E {}
                
                @Component
                class JavaComponent {
                   public JavaComponent(
                    //<caret>E service
                   ) {}                                           
                }
            """.trimMargin()
        )

        runModificationTriggeredTest { typeBackspaces(2) }
    }

    fun testCommentFieldWithJavaDoc() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.stereotype.Component;
                import org.springframework.beans.factory.annotation.Autowired;

                @Configuration
                public class MyConfiguration {
                       @Bean public E getE() {return new E();}   
                }                
                class E {}
                
                @Component
                class JavaComponent {
                    <caret>@Autowired E service;   /** Target test {}*/                    
                }
            """.trimMargin()
        )

        runModificationTriggeredTest { myFixture.type("//") }
    }

    fun testUncommentFieldWithJavaDoc() {
        myFixture.configureByText(
            "MyConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.stereotype.Component;
                import org.springframework.beans.factory.annotation.Autowired;

                @Configuration
                public class MyConfiguration {
                       @Bean public E getE() {return new E();}   
                }                
                class E {}
                
                @Component
                class JavaComponent {
                    //<caret>@Autowired E service;   /** Target test {}*/                    
                }
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