/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.RequestMappingDuplicateInspection
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
}