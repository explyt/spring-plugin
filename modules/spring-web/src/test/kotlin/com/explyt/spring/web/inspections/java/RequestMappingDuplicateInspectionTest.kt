/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.RequestMappingDuplicateInspection
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.intellij.lang.annotations.Language

class RequestMappingDuplicateInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWeb_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(RequestMappingDuplicateInspection::class.java)
    }

    fun testCorrectMethod() {
        @Language("JAVA") val trimIndent = """            
            import org.springframework.web.bind.annotation.*;

            import java.util.Map;
            import java.util.Optional;
            
            @RestController
            public class DuplicatedEndpointController {
                
                @RequestMapping(
                    path = <error descr="Duplicated endpoint 'conflictedGet'"><error descr="Duplicated endpoint 'conflictedPut'">"/same/uri"</error></error>, 
                    method = {RequestMethod.GET, RequestMethod.PUT}
                )
                @ResponseBody
                public String conflictedTwice() {
                    return "conflictedTwice";
                }
                
                @GetMapping(<error descr="Duplicated endpoint 'conflictedTwice'">"/same/uri"</error>)
                @ResponseBody
                public String conflictedGet() {
                    return "conflictedGet";
                }
                
                @PutMapping(<error descr="Duplicated endpoint 'conflictedTwice'">"/same/uri"</error>)
                @ResponseBody
                public String conflictedPut() {
                    return "conflictedPut";
                }
                
                @PostMapping("/same/uri")
                @ResponseBody
                public String postWithoutConflict() {
                    return "postWithoutConflict";
                }
                
                @PostMapping(<error descr="Duplicated endpoint 'postWithoutSlash'">"/slash/test"</error>)
                @ResponseBody
                public String postWithSlash() {
                    return "";
                }
                
                @PostMapping(<error descr="Duplicated endpoint 'postWithSlash'">"slash/test"</error>)
                @ResponseBody
                public String postWithoutSlash() {
                    return "";
                }
                
            }
            """.trimIndent()
        myFixture.configureByText("DuplicatedEndpointController.java", trimIndent)
        myFixture.testHighlighting("DuplicatedEndpointController.java")
    }

    fun testComposedMappingWithAliasDefaultPathDuplicatesAGetMapping() {
        addComposedMapping()
        @Language("JAVA") val controller = """
            import com.example.ApiGet;
            import org.springframework.web.bind.annotation.*;

            @RestController
            public class ComposedDuplicateController {

                @<error descr="Duplicated endpoint 'listed'">ApiGet</error>
                public String bare() {
                    return "bare";
                }

                @GetMapping(<error descr="Duplicated endpoint 'bare'">"/items"</error>)
                public String listed() {
                    return "listed";
                }
            }
            """.trimIndent()
        myFixture.configureByText("ComposedDuplicateController.java", controller)
        myFixture.testHighlighting("ComposedDuplicateController.java")
    }

    fun testComposedMappingWithPathSetAtUseSiteDoesNotDuplicateTheAliasDefault() {
        addComposedMapping()
        @Language("JAVA") val controller = """
            import com.example.ApiGet;
            import org.springframework.web.bind.annotation.*;

            @RestController
            public class ComposedDistinctController {

                @ApiGet(path = "/v2")
                public String useSite() {
                    return "useSite";
                }

                @GetMapping("/items")
                public String listed() {
                    return "listed";
                }

                @PostMapping(<error descr="Duplicated endpoint 'secondPost'">"/same"</error>)
                public String firstPost() {
                    return "firstPost";
                }

                @PostMapping(<error descr="Duplicated endpoint 'firstPost'">"/same"</error>)
                public String secondPost() {
                    return "secondPost";
                }
            }
            """.trimIndent()
        myFixture.configureByText("ComposedDistinctController.java", controller)
        myFixture.testHighlighting("ComposedDistinctController.java")
    }

    private fun addComposedMapping() {
        val composedFile = myFixture.addFileToProject(
            "com/example/ApiGet.java", """
            package com.example;

            import org.springframework.core.annotation.AliasFor;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RequestMethod;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
            @RequestMapping(method = RequestMethod.GET)
            public @interface ApiGet {
                @AliasFor(annotation = RequestMapping.class, attribute = "path")
                String[] path() default "/items";
            }
            """.trimIndent()
        )
        myFixture.allowTreeAccessForFile(composedFile.virtualFile)
        val composed = myFixture.findClass("com.example.ApiGet")
        assertTrue(
            "Precondition: @ApiGet is meta-annotated with @RequestMapping",
            composed.hasAnnotation("org.springframework.web.bind.annotation.RequestMapping")
        )
        assertNotNull(
            "Precondition: @RequestMapping resolves",
            JavaPsiFacade.getInstance(project)
                .findClass("org.springframework.web.bind.annotation.RequestMapping", GlobalSearchScope.allScope(project))
        )
    }
}
