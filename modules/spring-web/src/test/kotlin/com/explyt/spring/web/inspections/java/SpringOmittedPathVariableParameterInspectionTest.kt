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

package com.explyt.spring.web.inspections.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.inspections.SpringOmittedPathVariableParameterInspection
import org.intellij.lang.annotations.Language

class SpringOmittedPathVariableParameterInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springWeb_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringOmittedPathVariableParameterInspection::class.java)
    }

    fun testPathVariableSimpleMapping() {
        @Language("java") val code = """
            import org.springframework.web.bind.annotation.*;

@RestController
public class PathVariableController {   
    // Simple mapping
    @RequestMapping(path = "/api/employees/{id}/{<warning>omittedInUrl</warning>}", method = RequestMethod.GET)
    @ResponseBody
    public String getEmployeesById(@PathVariable String id, <warning>@PathVariable String omittedInSignature</warning>) {
        return "ID: " + id;
    }
}
            """.trimIndent()
        myFixture.configureByText("PathVariableController.java", code)
        myFixture.testHighlighting("PathVariableController.java")
    }

    fun testPathVariableSpecifyName() {
        @Language("java") val code = """
import org.springframework.web.bind.annotation.*;

@RestController
public class PathVariableController {
    final static String ID_NAMED = "id";
    
    // Specifying name
    @GetMapping("/api/employeeswithvariable/{id}")
    @ResponseBody
    public String getEmployeesByIdWithVariableName(
            @PathVariable(ID_NAMED) String employeeId
    ) {
        return "ID: " + employeeId;
    }
}
            """.trimIndent()
        myFixture.configureByText("PathVariableController.java", code)
        myFixture.testHighlighting("PathVariableController.java")
    }

    fun testPathVariableNotRequired() {
        @Language("java") val code = """
            import org.springframework.web.bind.annotation.*;

@RestController
public class PathVariableController {    
    
    // Not required
    @GetMapping(value = {"/api/employeeswithrequiredfalse", "/api/employeeswithrequiredfalse/{id}" })
    @ResponseBody
    public String getEmployeesByIdWithRequiredFalse(
            @PathVariable(required = false) String id
    ) {
       return "test";
    }
}
            """.trimIndent()
        myFixture.configureByText("PathVariableController.java", code)
        myFixture.testHighlighting("PathVariableController.java")
    }

    fun testPathVariableMap() {
        @Language("java") val code = """
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class PathVariableController {
    final static String ID_NAMED = "id";
    
   // Multiple variables in a map
    @GetMapping("/api/employeeswithmapvariable/{id}/{name}")
    @ResponseBody
    public String getEmployeesByIdAndNameWithMapVariable(
            @PathVariable Map<String, String> pathVarsMap
    ) {
        return "test";
    }
}
""".trimIndent()
        myFixture.configureByText("PathVariableController.java", code)
        myFixture.testHighlighting("PathVariableController.java")
    }

    fun testPathVariableOptional() {
        @Language("java") val code = """
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
public class PathVariableController {
    final static String ID_NAMED = "id";
    
    // Optional
    @GetMapping(value = {"/api/employeeswithoptional", "/api/employeeswithoptional/{id}"})
    @ResponseBody
    public String getEmployeesByIdWithOptional(   @PathVariable Optional<String> id  ) {
        return "test";
    }
}
            """.trimIndent()
        myFixture.configureByText("PathVariableController.java", code)
        myFixture.testHighlighting("PathVariableController.java")
    }

    fun testClassRequestMapping() {
        @Language("java") val code = """
            import org.springframework.web.bind.annotation.PostMapping;             
            
            @${SpringWebClasses.REQUEST_MAPPING}("{data}")
            @${SpringCoreClasses.CONTROLLER}            
            public class SpringComponent {
                                
                @PostMapping("post/{version}")
                public String postStr(
                    @${SpringWebClasses.PATH_VARIABLE} String data,
                    @${SpringWebClasses.PATH_VARIABLE} String version
                ) {
                    return "postStr";
                }
            }
            """.trimIndent()
        myFixture.configureByText("SpringComponent.java", code)
        myFixture.testHighlighting("SpringComponent.java")
    }

    fun testClassRequestMappingNoPathVariable() {
        @Language("java") val code = """
            import org.springframework.web.bind.annotation.PostMapping;             
            
            @${SpringWebClasses.REQUEST_MAPPING}("{data}")
            @${SpringCoreClasses.CONTROLLER}            
            public class SpringComponent {
                                
                @PostMapping("post")
                public String postStr(
                    <warning>@${SpringWebClasses.PATH_VARIABLE} String data1</warning>                    
                ) {
                    return "postStr";
                }
            }
            """.trimIndent()
        myFixture.configureByText("SpringComponent.java", code)
        myFixture.testHighlighting("SpringComponent.java")
    }

    fun testMethodRequestMappingNoPathVariable() {
        @Language("java") val code = """
            import org.springframework.web.bind.annotation.PostMapping;             
            
            @${SpringWebClasses.REQUEST_MAPPING}("{data}")
            @${SpringCoreClasses.CONTROLLER}            
            public class SpringComponent {
                               
                @PostMapping("post/{<warning>version</warning>}")                                                
                public String postStr(
                    @${SpringWebClasses.PATH_VARIABLE} String data,                    
                    <warning>@${SpringWebClasses.PATH_VARIABLE} String ver</warning>
                ) {
                    return "postStr";
                }
            }
            """.trimIndent()
        myFixture.configureByText("SpringComponent.java", code)
        myFixture.testHighlighting("SpringComponent.java")
    }
}