/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.addFromMaven
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor
import java.io.File
import kotlin.reflect.full.findAnnotation

/**
 * What `explyt_trace_spring_call_chain` reports about a call the IDE cannot resolve.
 *
 * A repository calling jOOQ on an injected `DSLContext` whose jar was never downloaded reported `callsInto: []`,
 * indistinguishable from a method that calls nothing. The fixture declares `org.jooq.DSLContext` without the library
 * and calls a method that does not exist on a project type, in Kotlin and in Java.
 */
class SpringBootApplicationMcpToolsetTraceUnresolvedTest : JavaCodeInsightFixtureTestCase() {

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun tuneFixture(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        moduleBuilder.addJdkVersion(LanguageLevel.JDK_21)
    }

    override fun setUp() {
        super.setUp()
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            LIBRARIES.forEach { addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies) }
        }
        addSources(
            KOTLIN_FILE to KOTLIN_SOURCE,
            JAVA_FILE to JAVA_SOURCE,
            TEMPLATES_FILE to TEMPLATES_SOURCE,
            JAVA_TEMPLATES_FILE to JAVA_TEMPLATES_SOURCE,
        )
    }

    fun testUnresolvedMethodOnAnInjectedProjectTypeIsAnExternalLeafMarkedUnresolved() = runBlocking {
        assertEquals(
            listOf(unresolvedExternal("RuleStore.missingMethod", lineOf(KOTLIN_SOURCE, "store.missingMethod(rule)"))),
            kotlinCallsOf("fun rename(rule: Rule)")
        )
        assertEquals(
            listOf(unresolvedExternal("JavaRuleStore.missingMethod", lineOf(JAVA_SOURCE, "store.missingMethod(name)"))),
            javaCallsOf("public String rename(String name)")
        )
    }

    fun testCallOnAnInjectedDependencyOfAnUnresolvedTypeIsNamedAfterTheDeclaredType() = runBlocking {
        assertEquals(
            listOf(unresolvedExternal("DSLContext.selectFrom", lineOf(KOTLIN_SOURCE, "dsl.selectFrom(rule.name)"))),
            kotlinCallsOf("fun update(rule: Rule)")
        )
        assertEquals(
            listOf(unresolvedExternal("DSLContext.selectFrom", lineOf(JAVA_SOURCE, "dsl.selectFrom(name)"))),
            javaCallsOf("public int update(String name)")
        )
    }

    /** The resolved leaves name the simple class, `JdbcTemplate.query`, so an unresolved one does too - type arguments left out. */
    fun testTargetNamesTheDeclaredTypeWithoutItsTypeArguments() = runBlocking {
        assertEquals(
            listOf(unresolvedExternal("RuleCache.missingMethod", lineOf(KOTLIN_SOURCE, "cache.missingMethod(rule)"))),
            kotlinCallsOf("fun remember(rule: Rule)")
        )
        assertEquals(
            listOf(unresolvedExternal("JavaRuleCache.missingMethod", lineOf(JAVA_SOURCE, "cache.missingMethod(name)"))),
            javaCallsOf("public String remember(String name)")
        )
    }

    fun testTargetNamesANullableDeclaredTypeWithoutTheQuestionMark() = runBlocking {
        assertEquals(
            listOf(unresolvedExternal("RuleCache.missingMethod", lineOf(KOTLIN_SOURCE, "spare?.missingMethod(rule)"))),
            kotlinCallsOf("fun spareRemember(rule: Rule)")
        )
        assertEquals(
            listOf(unresolvedExternal("DSLContext.fetch", lineOf(KOTLIN_SOURCE, "maybe?.fetch()"))),
            kotlinCallsOf("fun maybeFetch()")
        )
    }

    fun testUnresolvedCallThroughALocalCopyOrACallableReferenceIsListed() = runBlocking {
        assertEquals(
            listOf(unresolvedExternal("DSLContext.fetch", lineOf(KOTLIN_SOURCE, "return copy.fetch(sql)"))),
            kotlinCallsOf("fun fetchAll(sql: String)")
        )
        assertEquals(
            listOf(unresolvedExternal("DSLContext.delete", lineOf(KOTLIN_SOURCE, "ids.forEach(dsl::delete)"))),
            kotlinCallsOf("fun purge(ids: List<Long>)")
        )
        assertEquals(
            listOf(unresolvedExternal("DSLContext.fetch", lineOf(JAVA_SOURCE, "ids.forEach(dsl::fetch)"))),
            javaCallsOf("public void purge(List<String> ids)")
        )
    }

    fun testUnresolvedCallOnTheFieldThroughThisOrInsideALambdaIsListedAtItsOwnLine() = runBlocking {
        assertEquals(
            listOf(unresolvedExternal("DSLContext.fetch", lineOf(KOTLIN_SOURCE, "this.dsl.fetch()"))),
            kotlinCallsOf("fun explicitThis()")
        )
        assertEquals(
            listOf(unresolvedExternal("DSLContext.fetch", lineOf(KOTLIN_SOURCE, "dsl.fetch(it)"))),
            kotlinCallsOf("fun each(ids: List<Long>)")
        )
    }

    /**
     * A resolved and an unresolved call sharing a target are two calls - two libraries declare a `JdbcTemplate`, one
     * of them missing - and folding them into the first hid the unresolved one. A call with the wrong arguments is
     * not the shape: Kotlin resolves `jdbc.execute()` to the inapplicable candidate, so the trace sees it as resolved.
     */
    fun testResolvedAndUnresolvedCallsOfTheSameTargetAreBothListed() = runBlocking {
        assertTrue("Kotlin 'both' must make an unresolved call", hasUnresolvedCall(KOTLIN_TEMPLATES, "both"))
        assertEquals(
            listOf(
                resolvedExternal("JdbcTemplate.execute", lineOf(TEMPLATES_SOURCE, "jdbc.execute(sql)")),
                unresolvedExternal("JdbcTemplate.execute", lineOf(TEMPLATES_SOURCE, "legacy.execute(sql)")),
            ),
            callsOf(TEMPLATES_FILE, TEMPLATES_SOURCE, "fun both(sql: String)")
        )
        assertTrue("Java 'both' must make an unresolved call", hasUnresolvedCall(JAVA_TEMPLATES, "both"))
        assertEquals(
            listOf(
                resolvedExternal("JdbcTemplate.execute", lineOf(JAVA_TEMPLATES_SOURCE, "jdbc.execute(sql);")),
                unresolvedExternal("JdbcTemplate.execute", lineOf(JAVA_TEMPLATES_SOURCE, "legacy.execute(sql);")),
            ),
            callsOf(JAVA_TEMPLATES_FILE, JAVA_TEMPLATES_SOURCE, "public void both(String sql)")
        )
    }

    /**
     * Only the call made on the field itself is attributable to the dependency. The rest of a fluent chain is made on
     * what the previous call returned, which an unresolved call cannot name; a call with an implicit receiver, inside
     * `with(dsl)` or `dsl.apply`, names no receiver at all; and a fresh object, an unresolved type, the class itself,
     * a parameter shadowing the field or no receiver is never an injected dependency.
     */
    fun testUnresolvedCallOnAnythingButAnInjectedDependencyIsLeftOut() = runBlocking {
        assertEquals(
            listOf(unresolvedExternal("DSLContext.select", lineOf(KOTLIN_SOURCE, "dsl.select().from(rule.name)"))),
            kotlinCallsOf("fun chain(rule: Rule)")
        )
        for (method in listOf("scoped", "applied", "scratch", "typeQualified", "unqualified", "self")) {
            assertTrue("$method must make an unresolved call for its exclusion to prove anything", hasUnresolvedCall(KOTLIN_REPOSITORY, method))
            assertEquals("$method calls nothing the trace can attribute", emptyList<JsonNode>(), kotlinCallsOf("fun $method()"))
        }
        assertTrue("'shadowed' must make an unresolved call", hasUnresolvedCall(KOTLIN_REPOSITORY, "shadowed"))
        assertEquals(emptyList<JsonNode>(), kotlinCallsOf("fun shadowed(dsl: Rule)"))
    }

    /** `dsl()` invokes the dependency itself; without its type the call has no receiver UAST can name, and is not attributed. */
    fun testInvokeConventionOnAnUnresolvedTypeIsNotAttributed() = runBlocking {
        assertTrue("'invoked' must make an unresolved call", hasUnresolvedCall(KOTLIN_REPOSITORY, "invoked"))
        assertEquals(emptyList<JsonNode>(), kotlinCallsOf("fun invoked()"))
    }

    fun testDescriptionPromisesTheResolvedFlag() {
        val description = SpringBootApplicationMcpToolset::traceCallChain.findAnnotation<McpDescription>()!!.description

        assertTrue("The description must say how an unresolved call is reported", description.contains("'resolved': false"))
        assertTrue("The description must say that an implicit receiver is not attributed", description.contains("implicit receiver"))
    }

    fun testResolvedCallCarriesNoResolvedKey() = runBlocking {
        val projectCall = kotlinCallsOf("fun keep(rule: Rule)").single()
        val externalCall = kotlinCallsOf("fun rows()").single()

        assertEquals("PROJECT", projectCall["kind"].asText())
        assertEquals("EXTERNAL", externalCall["kind"].asText())
        for (call in listOf(projectCall, externalCall)) {
            assertEquals(RESOLVED_CALL_KEYS, call.fieldNames().asSequence().toSet())
        }
    }

    private fun hasUnresolvedCall(className: String, method: String): Boolean {
        val owner = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.projectScope(project))!!
        val uMethod = owner.findMethodsByName(method, false).single().toUElement() as UMethod
        var unresolved = 0
        uMethod.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (node.resolve() == null) unresolved++
                return false
            }
        })
        return unresolved > 0
    }

    private suspend fun kotlinCallsOf(methodAnchor: String): List<JsonNode> =
        callsOf(KOTLIN_FILE, KOTLIN_SOURCE, methodAnchor)

    private suspend fun javaCallsOf(methodAnchor: String): List<JsonNode> =
        callsOf(JAVA_FILE, JAVA_SOURCE, methodAnchor)

    private suspend fun callsOf(file: String, source: String, methodAnchor: String): List<JsonNode> {
        val response = mapper.readTree(
            toolset.traceCallChain(
                filePath = "$MAIN_ROOT/$file",
                line = lineOf(source, methodAnchor),
                projectPath = project.basePath!!,
                depth = 2,
                includeTests = false,
            )
        )
        return response["chain"][0]["callsInto"].toList()
    }

    private fun resolvedExternal(target: String, line: Int): JsonNode = mapper.createObjectNode().apply {
        put("target", target)
        put("kind", "EXTERNAL")
        put("line", line)
        putNull("node")
        putNull("via")
    }

    private fun unresolvedExternal(target: String, line: Int): JsonNode =
        (resolvedExternal(target, line) as com.fasterxml.jackson.databind.node.ObjectNode).put("resolved", false)

    private fun lineOf(source: String, anchor: String): Int {
        val index = source.lines().indexOfFirst { it.contains(anchor) }
        assertTrue("Anchor '$anchor' is absent from the fixture", index >= 0)
        return index + 1
    }

    private fun addSources(vararg files: Pair<String, String>) {
        val sourcesRoot = File(project.basePath!!, MAIN_ROOT).apply { mkdirs() }
        files.forEach { (relativePath, content) ->
            File(sourcesRoot, relativePath).apply {
                parentFile.mkdirs()
                writeText(content)
            }
        }

        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        val sourcesRootVf = VfsUtil.findFile(sourcesRoot.toPath(), true)
            ?: error("Sources root not visible in VFS: ${sourcesRoot.absolutePath}")
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            model.addContentEntry(sourcesRootVf).addSourceFolder(sourcesRootVf, false)
        }
        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private companion object {
        const val MAIN_ROOT = "traceMain"
        const val KOTLIN_FILE = "com/example/alerts/Alerts.kt"
        const val JAVA_FILE = "com/example/alerts/JavaAlertRepository.java"
        const val TEMPLATES_FILE = "com/example/templates/Templates.kt"
        const val JAVA_TEMPLATES_FILE = "com/example/templates/JavaTemplates.java"
        const val KOTLIN_REPOSITORY = "com.example.alerts.AlertRepository"
        const val KOTLIN_TEMPLATES = "com.example.templates.Templates"
        const val JAVA_TEMPLATES = "com.example.templates.JavaTemplates"

        val RESOLVED_CALL_KEYS = setOf("target", "kind", "line", "node", "via")

        val LIBRARIES = listOf(
            TestLibrary.springContext_6_0_7,
            TestLibrary.springJdbc_6_2_5,
            TestLibrary.kotlin_1_9_22,
        )

        val KOTLIN_SOURCE = """
            package com.example.alerts

            import org.jooq.DSLContext
            import org.springframework.jdbc.core.JdbcTemplate
            import org.springframework.stereotype.Repository
            import org.springframework.stereotype.Service

            class Rule(val id: Long, val name: String)

            @Repository
            class RuleStore {
                fun save(rule: Rule): Rule = rule
            }

            class RuleCache<T> {
                fun put(value: T): T = value
            }

            @Service
            class RuleService(
                private val store: RuleStore,
                private val cache: RuleCache<Rule>,
                private val spare: RuleCache<Rule>?,
            ) {
                fun rename(rule: Rule): Rule = store.missingMethod(rule)

                fun remember(rule: Rule): Rule = cache.missingMethod(rule)

                fun spareRemember(rule: Rule): Rule? = spare?.missingMethod(rule)

                fun keep(rule: Rule): Rule = store.save(rule)
            }

            @Repository
            class AlertRepository(
                private val dsl: DSLContext,
                private val maybe: DSLContext?,
                private val jdbc: JdbcTemplate,
            ) {
                fun update(rule: Rule): Int = dsl.selectFrom(rule.name)

                fun fetchAll(sql: String): Int {
                    val copy = dsl
                    return copy.fetch(sql)
                }

                fun purge(ids: List<Long>) = ids.forEach(dsl::delete)

                fun explicitThis(): Int = this.dsl.fetch()

                fun maybeFetch(): Int? = maybe?.fetch()

                fun each(ids: List<Long>) {
                    ids.forEach {
                        dsl.fetch(it)
                    }
                }

                fun chain(rule: Rule): Int = dsl.select().from(rule.name)

                fun scoped(): Int = with(dsl) { fetch() }

                fun applied(): DSLContext = dsl.apply { fetch() }

                fun shadowed(dsl: Rule): Int = dsl.missing()

                fun invoked(): Int = dsl()

                fun scratch(): Int = Scratch().bar()

                fun typeQualified(): Int = Missing.lookup()

                fun unqualified(): Int = missing()

                fun self(): Int = this.missing()

                fun rows(): List<String> = jdbc.queryForList("select 1", String::class.java)
            }
        """.trimIndent()

        val JAVA_SOURCE = """
            package com.example.alerts;

            import java.util.List;
            import org.jooq.DSLContext;
            import org.springframework.stereotype.Repository;

            @Repository
            public class JavaAlertRepository {
                private final DSLContext dsl;
                private final JavaRuleStore store;
                private final JavaRuleCache<String> cache;

                public JavaAlertRepository(DSLContext dsl, JavaRuleStore store, JavaRuleCache<String> cache) {
                    this.dsl = dsl;
                    this.store = store;
                    this.cache = cache;
                }

                public int update(String name) {
                    return dsl.selectFrom(name);
                }

                public String rename(String name) {
                    return store.missingMethod(name);
                }

                public String remember(String name) {
                    return cache.missingMethod(name);
                }

                public void purge(List<String> ids) {
                    ids.forEach(dsl::fetch);
                }
            }

            class JavaRuleStore {
                String save(String name) {
                    return name;
                }
            }

            class JavaRuleCache<T> {
                T put(T value) {
                    return value;
                }
            }
        """.trimIndent()

        val TEMPLATES_SOURCE = """
            package com.example.templates

            import org.springframework.stereotype.Repository

            @Repository
            class Templates(
                private val jdbc: org.springframework.jdbc.core.JdbcTemplate,
                private val legacy: JdbcTemplate,
            ) {
                fun both(sql: String) {
                    jdbc.execute(sql)
                    legacy.execute(sql)
                }
            }
        """.trimIndent()

        val JAVA_TEMPLATES_SOURCE = """
            package com.example.templates;

            import org.springframework.stereotype.Repository;

            @Repository
            public class JavaTemplates {
                private final org.springframework.jdbc.core.JdbcTemplate jdbc;
                private final JdbcTemplate legacy;

                public JavaTemplates(org.springframework.jdbc.core.JdbcTemplate jdbc, JdbcTemplate legacy) {
                    this.jdbc = jdbc;
                    this.legacy = legacy;
                }

                public void both(String sql) {
                    jdbc.execute(sql);
                    legacy.execute(sql);
                }
            }
        """.trimIndent()
    }
}
