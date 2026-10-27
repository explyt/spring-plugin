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
 * A method passed as a callable reference is invoked by the function it is passed to - `input.use(validator::validate)`
 * runs `validate` - so the trace follows it like a call written out.
 */
class CallChainTracerReferenceTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springJdbc_6_2_5,
        TestLibrary.kotlin_1_9_22,
    )

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/app/Logos.kt", """
            package com.example.app

            import org.springframework.jdbc.core.JdbcTemplate
            import org.springframework.stereotype.Component
            import org.springframework.stereotype.Service

            @Component
            class LogoValidator {
                fun validate(input: java.io.InputStream): ByteArray = input.readBytes()
            }

            interface LogoSource : java.util.function.Supplier<ByteArray>

            class Logo(val name: String) {
                val label: String get() = name.uppercase()
                fun render(): String = "<" + name + ">"
            }

            @Service
            class Logos(private val logoValidator: LogoValidator?, private val jdbc: JdbcTemplate) {
                fun upload(input: java.io.InputStream, names: List<String>): Int {
                    val validator = logoValidator ?: throw IllegalStateException("no validator")
                    val image = input.use(validator::validate)
                    names.map(String::trim).forEach(this::remember)
                    listOf("delete from logo").forEach(jdbc::update)
                    return image.size
                }

                fun describe(sources: List<LogoSource>, names: List<String>): List<String> {
                    val logos = names.map(::Logo)
                    val bytes = sources.map(LogoSource::get)
                    return logos.map(Logo::label) + bytes.map { it.size.toString() }
                }

                fun titles(logos: List<Logo>): List<String> = logos.map(Logo::render)

                private fun remember(name: String) = Unit
            }
            """.trimIndent()
        )
    }

    fun testReferencedMethodsAreCallsOfTheirKind() {
        assertEquals(
            listOf(
                "LogoValidator.validate" to CallKind.PROJECT,
                "Logos.remember" to CallKind.INTERNAL,
                "JdbcTemplate.update" to CallKind.EXTERNAL,
            ),
            callsOf("upload").map { it.target to it.kind }
        )
    }

    /**
     * None of these passes a request on:
     * - `::Logo` creates a value, as a constructor call does;
     * - `Logo::label` reads a property, although it resolves to the getter;
     * - `LogoSource::get` is qualified by a type, not by an object the trace could name.
     */
    fun testConstructorPropertyAndTypeQualifiedReferencesAreNotCalls() {
        assertEquals(emptyList<Pair<String, CallKind>>(), callsOf("describe").map { it.target to it.kind })
    }

    /**
     * A type-qualified reference names no object, so it is never an injected bean, but the project method it reaches is
     * still code the request runs: the receiving function invokes it on each element.
     */
    fun testTypeQualifiedReferenceToAProjectMethodIsFollowed() {
        assertEquals(listOf("Logo.render" to CallKind.PROJECT), callsOf("titles").map { it.target to it.kind })
    }

    /** A referenced project method is followed: it becomes a node of the chain, reached from the reference's line. */
    fun testReferencedProjectMethodIsTraced() {
        val chain = trace("upload")
        val validate = chain.methods.first().calls.single { it.target == "LogoValidator.validate" }

        assertEquals("validate", validate.reached?.name)
        assertNotNull("The referenced method is a chain node", chain.idOf(validate.reached!!))
        assertEquals(lineOf("input.use(validator::validate)"), validate.line)
    }

    private fun callsOf(method: String): List<TracedCall> = trace(method).methods.first().calls

    private fun trace(method: String): CallChain {
        val logos = JavaPsiFacade.getInstance(project)
            .findClass("com.example.app.Logos", GlobalSearchScope.projectScope(project))!!
        return CallChainTracer(project, maxMethods = 10).trace(logos.findMethodsByName(method, false).single(), depth = 2)
    }

    private fun lineOf(anchor: String): Int {
        val text = myFixture.findFileInTempDir("com/example/app/Logos.kt")!!.let { String(it.contentsToByteArray()) }
        val index = text.lines().indexOfFirst { anchor in it }
        assertTrue("Anchor '$anchor' is absent from the fixture", index >= 0)
        return index + 1
    }
}
