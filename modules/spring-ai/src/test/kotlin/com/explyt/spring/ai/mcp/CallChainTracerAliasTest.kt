/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope

/**
 * A library call on a local copy of an injected dependency is where the request leaves the application, exactly as
 * the same call on the field would be.
 */
class CallChainTracerAliasTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springJdbc_6_2_5,
        TestLibrary.kotlin_1_9_22,
    )

    fun testLibraryCallOnALocalCopyOfAnInjectedDependencyIsExternal() {
        myFixture.addFileToProject(
            "com/example/app/Reports.kt", """
            package com.example.app

            import org.springframework.jdbc.core.JdbcTemplate
            import org.springframework.stereotype.Repository

            @Repository
            class Reports(private val jdbc: JdbcTemplate?) {
                fun rows(): List<String> {
                    val template = jdbc ?: error("no database")
                    return template.queryForList("select name from report", String::class.java)
                }

                fun fresh(): List<String> {
                    val template = JdbcTemplate()
                    return template.queryForList("select name from report", String::class.java)
                }
            }
            """.trimIndent()
        )

        assertEquals(listOf("JdbcTemplate.queryForList" to CallKind.EXTERNAL), callsOf("rows"))
        assertEquals("A template the method builds itself is not an injected dependency", emptyList<Any>(), callsOf("fresh"))
    }

    private fun callsOf(method: String): List<Pair<String, CallKind>> {
        val reports = JavaPsiFacade.getInstance(project)
            .findClass("com.example.app.Reports", GlobalSearchScope.projectScope(project))!!
        val start = reports.findMethodsByName(method, false).single()
        return CallChainTracer(project, maxMethods = 10).trace(start, depth = 2)
            .methods.first().calls.map { it.target to it.kind }
    }
}
