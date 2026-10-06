/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.addFromMaven
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * `explyt_trace_spring_call_chain` against methods the bundled Lombok plugin really generates: a `@Data` setter called
 * on a subclass receiver was traced as a node at line 1 on a real project, spending the method limit.
 */
class SpringBootApplicationMcpToolsetTraceLombokTest : JavaCodeInsightFixtureTestCase() {

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun tuneFixture(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        moduleBuilder.addJdkVersion(LanguageLevel.JDK_21)
    }

    override fun setUp() {
        super.setUp()
        IdeaTestUtil.setModuleLanguageLevel(myFixture.module, LanguageLevel.JDK_21, testRootDisposable)
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            LIBRARIES.forEach { addFromMaven(model, it, false) }
        }
        addSource("com/example/topics/Dto.java", DTO_SOURCE)
        addSource("com/example/topics/Sub.java", SUB_SOURCE)
        addSource("com/example/topics/PerEnv.java", PER_ENV_SOURCE)
        addSource("com/example/topics/LazyDto.java", LAZY_DTO_SOURCE)
        addSource("com/example/topics/TopicStore.java", STORE_SOURCE)
        addSource("com/example/topics/TopicService.java", SERVICE_SOURCE)
        addSource("com/example/topics/TopicController.java", CONTROLLER_SOURCE)
    }

    fun testInheritedSetterResolvesToALombokGeneratedMethod() {
        val sub = JavaPsiFacade.getInstance(project)
            .findClass("com.example.topics.Sub", GlobalSearchScope.projectScope(project))!!
        val setter = sub.findMethodsByName("setTopicExists", true).single()
        assertFalse("Precondition: the setter is generated", setter.isPhysical)
        assertTrue("Precondition: generated from the field", setter.navigationElement is PsiField)
        assertTrue(TrivialAccessors.isTrivial(setter))
    }



    fun testInheritedLombokSetterCalledOnASubclassIsAnAccessor() = runBlocking {
        val trace = trace()

        trace.assertAccessor(trace.call("Dto.setTopicExists", receiverLine("local.setTopicExists")))
        trace.assertAccessor(trace.call("PerEnv.setTopicExists", receiverLine("perEnv.setTopicExists")))
    }

    fun testLombokGetterIsAnAccessor() = runBlocking {
        val trace = trace()

        trace.assertAccessor(trace.call("Dto.getName", receiverLine("sub.getName")))
    }

    fun testLazyLombokGetterIsTracedAtItsFieldLine() = runBlocking {
        val trace = trace()

        val node = trace.assertTraced(trace.call("LazyDto.getCached", receiverLine("lazy.getCached")))
        assertEquals(lineOf(LAZY_DTO_SOURCE, "cached ="), node["line"].asInt())
    }

    fun testRepositoryCallIsTracedWithinTheLimit() = runBlocking {
        val trace = trace()

        assertFalse(trace.root["chainLimitReached"].asBoolean())
        trace.assertTraced(trace.call("TopicStore.save", receiverLine("store.save")))
    }

    fun testGeneratedMethodPositionIsItsFieldLine() {
        val lazy = JavaPsiFacade.getInstance(project)
            .findClass("com.example.topics.LazyDto", GlobalSearchScope.projectScope(project))!!
        val getter: PsiMethod = lazy.findMethodsByName("getCached", false).single()
        assertTrue("Precondition: a generated method has an empty range", getter.textRange?.isEmpty != false)
        val anchor = McpSourcePositions.sourceAnchorOf(getter)!!
        assertEquals(lineOf(LAZY_DTO_SOURCE, "cached ="), McpSourcePositions.lineOfAnchor(anchor))
    }

    private inner class Trace(val root: JsonNode) {
        val chain: JsonNode get() = root["chain"]
        val service: JsonNode = chain.single { it["className"].asText().endsWith(".TopicService") }

        fun call(target: String, line: Int): JsonNode = service["callsInto"].singleOrNull {
            it["target"].asText() == target && it["line"].asInt() == line
        } ?: error("No $target at line $line in ${service["callsInto"]}")

        fun assertAccessor(call: JsonNode) {
            assertTrue("$call must be an accessor", call["accessor"]?.asBoolean() == true)
            assertTrue("$call must not be traced", call["node"].isNull)
        }

        fun assertTraced(call: JsonNode): JsonNode {
            assertNull("$call is not an accessor", call["accessor"])
            assertFalse("$call must be traced", call["node"].isNull)
            return chain.single { it["id"].asInt() == call["node"].asInt() }
        }
    }

    private suspend fun trace(): Trace = Trace(
        mapper.readTree(
            toolset.traceCallChain(
                filePath = "$MAIN_ROOT/com/example/topics/TopicController.java",
                line = lineOf(CONTROLLER_SOURCE, "public void refresh("),
                projectPath = project.basePath!!,
                includeTests = false,
                limit = 50,
                maxChars = 16000,
            )
        )
    )

    private fun receiverLine(snippet: String): Int = lineOf(SERVICE_SOURCE, snippet)

    private fun lineOf(source: String, snippet: String): Int {
        val index = source.lines().indexOfFirst { it.contains(snippet) }
        assertTrue("'$snippet' is absent from the fixture", index >= 0)
        return index + 1
    }

    private fun addSource(relativePath: String, content: String) {
        val sourcesRoot = File(project.basePath!!, MAIN_ROOT).apply { mkdirs() }
        File(sourcesRoot, relativePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }

        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        val sourcesRootVf = VfsUtil.findFile(sourcesRoot.toPath(), true)
            ?: error("Sources root not visible in VFS: ${sourcesRoot.absolutePath}")
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            val alreadyAdded = model.contentEntries.any { it.file == sourcesRootVf }
            if (!alreadyAdded) model.addContentEntry(sourcesRootVf).addSourceFolder(sourcesRootVf, false)
        }
        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private companion object {
        const val MAIN_ROOT = "lombokMain"

        val LIBRARIES = listOf(
            TestLibrary.springWebMvc_6_0_7.mavenCoordinates,
            TestLibrary.springContext_6_0_7.mavenCoordinates,
            "org.projectlombok:lombok:1.18.46",
            "jakarta.validation:jakarta.validation-api:3.0.2",
        )

        val DTO_SOURCE = """
            package com.example.topics;

            @lombok.Data
            public class Dto {
                @jakarta.validation.constraints.NotNull boolean topicExists;
                String name;
            }
        """.trimIndent()

        val PER_ENV_SOURCE = """
            package com.example.topics;

            import jakarta.validation.constraints.NotNull;

            @lombok.Data
            public class PerEnv {
                @NotNull private boolean topicExists;
                private String error;
            }
        """.trimIndent()

        val SUB_SOURCE = """
            package com.example.topics;

            @lombok.Data
            public class Sub extends Dto {
                @jakarta.validation.constraints.NotNull private java.util.List<String> infos;
            }
        """.trimIndent()

        val LAZY_DTO_SOURCE = """
            package com.example.topics;

            public class LazyDto {
                @lombok.Getter(lazy = true)
                private final String cached = String.valueOf(System.nanoTime());
            }
        """.trimIndent()

        val STORE_SOURCE = """
            package com.example.topics;

            import org.springframework.stereotype.Component;

            @Component
            public class TopicStore {
                public void save(Dto dto) {
                    System.out.println(dto);
                }
            }
        """.trimIndent()

        val SERVICE_SOURCE = """
            package com.example.topics;

            import org.springframework.stereotype.Service;

            @Service
            public class TopicService {
                private final TopicStore store;

                public TopicService(TopicStore store) {
                    this.store = store;
                }

                public String refresh(Sub sub, Dto dto, LazyDto lazy) {
                    Sub local = new Sub();
                    local.setTopicExists(false);
                    PerEnv perEnv = new PerEnv();
                    perEnv.setTopicExists(true);
                    sub.setTopicExists(true);
                    String name = sub.getName();
                    dto.setTopicExists(false);
                    String cached = lazy.getCached();
                    store.save(sub);
                    return name + cached;
                }
            }
        """.trimIndent()

        val CONTROLLER_SOURCE = """
            package com.example.topics;

            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class TopicController {
                private final TopicService service;

                public TopicController(TopicService service) {
                    this.service = service;
                }

                @PostMapping("/topics")
                public void refresh(Sub sub, Dto dto, LazyDto lazy) {
                    service.refresh(sub, dto, lazy);
                }
            }
        """.trimIndent()
    }
}
