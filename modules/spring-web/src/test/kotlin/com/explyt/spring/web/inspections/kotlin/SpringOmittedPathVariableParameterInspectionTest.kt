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

package com.explyt.spring.web.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.inspections.SpringOmittedPathVariableParameterInspection
import org.intellij.lang.annotations.Language

class SpringOmittedPathVariableParameterInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springWeb_6_0_7
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringOmittedPathVariableParameterInspection::class.java)
    }

    fun testSimpleMapping() {
        @Language("kotlin") val code = """
import org.springframework.web.bind.annotation.*

@RestController
class PathVariableController {
    // Simple mapping
    @RequestMapping(path = ["/api/employees/{id}/{<warning>omittedInUrl</warning>}"], method = [RequestMethod.GET])
    @ResponseBody
    fun getEmployeesById(@PathVariable id: String, @PathVariable omittedInSignature: String?): String {
        return "ID: ${'$'}id"
    }
}
""".trimIndent()
        myFixture.configureByText("PathVariableController.kt", code)
        myFixture.testHighlighting("PathVariableController.kt")
    }

    fun testPathVariableSpecifyName() {
        @Language("kotlin") val code = """
import org.springframework.web.bind.annotation.*

@RestController
class PathVariableController {
    // Specifying name
    @GetMapping("/api/employeeswithvariable/{id}")
    @ResponseBody
    fun getEmployeesByIdWithVariableName(
        @PathVariable(ID_NAMED) employeeId: String
    ): String {
        return "ID: ${'$'}employeeId"
    }

    companion object {
        const val ID_NAMED: String = "id"
    }

}
""".trimIndent()
        myFixture.configureByText("PathVariableController.kt", code)
        myFixture.testHighlighting("PathVariableController.kt")
    }

    fun testPathVariableNotRequired() {
        @Language("kotlin") val code = """
import org.springframework.web.bind.annotation.*

@RestController
class PathVariableController {   
        // Not required
    @GetMapping(
        value = ["/api/employeeswithrequiredfalse", "/api/employeeswithrequiredfalse/{id}"]
    )
    @ResponseBody
    fun getEmployeesByIdWithRequiredFalse(@PathVariable(required = false) id: String?    ): String = "test"
}
""".trimIndent()
        myFixture.configureByText("PathVariableController.kt", code)
        myFixture.testHighlighting("PathVariableController.kt")
    }

    fun testPathVariableMap() {
        @Language("kotlin") val code = """
import org.springframework.web.bind.annotation.*

@RestController
class PathVariableController {   
    // Multiple variables in a map
    @GetMapping("/api/employeeswithmapvariable/{id}/{name}")
    @ResponseBody
    fun getEmployeesByIdAndNameWithMapVariable(
        @PathVariable pathVarsMap: Map<String?, String?>
    ): String = "test"
}
""".trimIndent()
        myFixture.configureByText("PathVariableController.kt", code)
        myFixture.testHighlighting("PathVariableController.kt")
    }

    fun testPathVariableOptional() {
        @Language("kotlin") val code = """
            import org.springframework.web.bind.annotation.*

@RestController
class PathVariableController {
    // Optional
    @GetMapping(
        value = ["/api/employeeswithoptional", "/api/employeeswithoptional/{id}"]
    )
    @ResponseBody
    fun getEmployeesByIdWithOptional(
        @PathVariable id: java.util.Optional<String?>
    ): String {
        return ""
    }
}
            """.trimIndent()
        myFixture.configureByText("PathVariableController.kt", code)
        myFixture.testHighlighting("PathVariableController.kt")
    }

    fun testKotlinStringExpression() {
        @Language("kotlin") val code = """
            import org.springframework.web.bind.annotation.PostMapping
            const val str = "str" 
            
            @${SpringCoreClasses.CONTROLLER}            
            class SpringComponent {
                                 
                @PostMapping("post/${'$'}{str}")
                fun postStr(): String {
                    return "postStr"
                }
            }
            """.trimIndent()
        myFixture.configureByText("SpringComponent.kt", code)
        myFixture.testHighlighting("SpringComponent.kt")
    }

    fun testClassRequestMapping() {
        @Language("kotlin") val code = """
            import org.springframework.web.bind.annotation.PostMapping             
            
            @${SpringWebClasses.REQUEST_MAPPING}("{data}")
            @${SpringCoreClasses.CONTROLLER}            
            class SpringComponent {
                                
                @PostMapping("post/{version}")
                fun postStr(
                    @${SpringWebClasses.PATH_VARIABLE} data: String,
                    @${SpringWebClasses.PATH_VARIABLE} version: String,
                ): String {
                    return "postStr"
                }
            }
            """.trimIndent()
        myFixture.configureByText("SpringComponent.kt", code)
        myFixture.testHighlighting("SpringComponent.kt")
    }
}