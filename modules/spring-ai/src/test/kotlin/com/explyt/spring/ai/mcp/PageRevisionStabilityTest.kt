/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.ai.mcp.UnrelatedPsiChange.DUMB_MODE_CYCLED
import com.explyt.spring.ai.mcp.UnrelatedPsiChange.NOTE_EDITED
import com.explyt.spring.ai.mcp.UnrelatedPsiChange.PSI_CACHES_DROPPED
import com.explyt.spring.ai.mcp.beans.NativeBeanListingFixture
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope

/**
 * A page revision names one answer of the Spring model, so a continuation must survive what leaves that model
 * as it was: a note edited next to the sources, dropped PSI caches, an indexing pass. A real project saw three
 * identical calls answer three revisions and page 2 refused with `RESULT_CHANGED` although nothing was edited.
 *
 * Each tool family is continued after each such change; the controls change the model itself between the pages,
 * so a revision that never moves cannot pass them.
 */
class PageRevisionStabilityTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.jakarta_persistence_3_1_0,
        TestLibrary.kotlin_1_9_22,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()
    private val nativeFixture by lazy { NativeBeanListingFixture(project) }
    private lateinit var notes: PsiFile

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("springBootApp", "")
        notes = myFixture.addFileToProject("notes.md", "# Notes\n")
    }

    override fun tearDown() {
        try {
            nativeFixture.clear()
        } finally {
            super.tearDown()
        }
    }

    fun testListingContinuesAfterANoteIsEdited() = listingContinuesAfter(NOTE_EDITED)
    fun testListingContinuesAfterPsiCachesAreDropped() = listingContinuesAfter(PSI_CACHES_DROPPED)
    fun testListingContinuesAfterDumbMode() = listingContinuesAfter(DUMB_MODE_CYCLED)

    fun testListingRejectsItsRevisionAfterANewMapping() = runBlockingReleasingWriteIntent<Unit> {
        val first = listingPage()
        assertTruncatedFirstPage(LIST, first)

        appendBeforeLastBrace(ROUTE_CONTROLLER, "@GetMapping(\"/added\") public String added() { return \"added\"; }")

        assertResultChanged(LIST, "a new @GetMapping", listingPage(first))
    }

    fun testListingRejectsItsRevisionAfterAPathConstantIsEdited() = runBlockingReleasingWriteIntent<Unit> {
        addConstantRoute()
        val first = listingPage()
        assertTruncatedFirstPage(LIST, first)

        replaceInSource(REVISION_PATHS, "\"/a\"", "\"/b\"")

        assertResultChanged(LIST, "the path constant of a @GetMapping edited", listingPage(first))
    }

    fun testListingServesTheEditedPathConstant() = runBlockingReleasingWriteIntent<Unit> {
        addConstantRoute()
        assertEquals(listOf("/api/constant/a"), constantRoutePaths())

        replaceInSource(REVISION_PATHS, "\"/a\"", "\"/b\"")

        assertEquals(listOf("/api/constant/b"), constantRoutePaths())
    }

    fun testContractRejectsItsRevisionAfterAServiceCallIsAddedToAHandlerBody() = runBlockingReleasingWriteIntent<Unit> {
        myFixture.addFileToProject(REVISION_CONTROLLER, REVISION_CONTROLLER_SOURCE)
        val maxChars = budgetForOneContract(REVISION_URL)
        val first = contractPage(maxChars, first = null, url = REVISION_URL)
        assertTruncatedFirstPage(CONTRACT, first)


        replaceInSource(REVISION_CONTROLLER, "return \"second\";", "audit.record();\n        return \"second\";")

        assertResultChanged(CONTRACT, "a service call added to the page 2 handler body", contractPage(maxChars, first, REVISION_URL))
    }

    fun testContractRejectsItsRevisionAfterADtoFieldChangesItsType() = runBlockingReleasingWriteIntent<Unit> {
        val maxChars = budgetForOneContract(DEMO_ITEMS_URL)
        val first = contractPage(maxChars, first = null, url = DEMO_ITEMS_URL)
        assertTruncatedFirstPage(CONTRACT, first)

        replaceInSource(DEMO_DTO, "private String name;", "private int name;")
        replaceInSource(DEMO_DTO, "public String getName()", "public int getName()")

        assertResultChanged(CONTRACT, "a DTO field changed from String to int", contractPage(maxChars, first, DEMO_ITEMS_URL))
    }

    fun testContractContinuesAfterANoteIsEdited() = contractContinuesAfter(NOTE_EDITED)
    fun testContractContinuesAfterPsiCachesAreDropped() = contractContinuesAfter(PSI_CACHES_DROPPED)
    fun testContractContinuesAfterDumbMode() = contractContinuesAfter(DUMB_MODE_CYCLED)

    fun testEntitiesContinueAfterANoteIsEdited() = entitiesContinueAfter(NOTE_EDITED)
    fun testEntitiesContinueAfterPsiCachesAreDropped() = entitiesContinueAfter(PSI_CACHES_DROPPED)
    fun testEntitiesContinueAfterDumbMode() = entitiesContinueAfter(DUMB_MODE_CYCLED)

    fun testEntitiesRejectTheirRevisionAfterANewColumn() = runBlockingReleasingWriteIntent<Unit> {
        val first = entitiesPage()
        assertTruncatedFirstPage(ENTITIES, first)

        appendBeforeLastBrace(DEMO_ENTITY, "@Column(name = \"added\") private String added;")

        assertResultChanged(ENTITIES, "a new @Entity field", entitiesPage(first))
    }

    fun testStaticBeanListingContinuesAfterANoteIsEdited() = beansContinueAfter(NOTE_EDITED, STATIC)
    fun testStaticBeanListingContinuesAfterPsiCachesAreDropped() = beansContinueAfter(PSI_CACHES_DROPPED, STATIC)
    fun testStaticBeanListingContinuesAfterDumbMode() = beansContinueAfter(DUMB_MODE_CYCLED, STATIC)

    fun testStaticBeanListingRejectsItsRevisionAfterANewComponent() = runBlockingReleasingWriteIntent<Unit> {
        val first = beansPage(STATIC)
        assertTruncatedFirstPage(BEANS, first)

        appendAtEnd(DEMO_SERVICE, "\n@org.springframework.stereotype.Component\nclass AddedLater {}\n")

        assertResultChanged(BEANS, "a new @Component", beansPage(STATIC, first))
    }

    fun testNativeBeanListingContinuesAfterANoteIsEdited() = beansContinueAfter(NOTE_EDITED, NATIVE)
    fun testNativeBeanListingContinuesAfterPsiCachesAreDropped() = beansContinueAfter(PSI_CACHES_DROPPED, NATIVE)
    fun testNativeBeanListingContinuesAfterDumbMode() = beansContinueAfter(DUMB_MODE_CYCLED, NATIVE)

    private fun listingContinuesAfter(change: UnrelatedPsiChange) = runBlockingReleasingWriteIntent<Unit> {
        val first = listingPage()
        assertTruncatedFirstPage(LIST, first)

        change.applyTo(project) { notes }

        assertContinued(LIST, change.description, listingPage(first))
    }

    private fun contractContinuesAfter(change: UnrelatedPsiChange) = runBlockingReleasingWriteIntent<Unit> {
        val maxChars = budgetForOneContract()
        val first = contractPage(maxChars)
        assertTruncatedFirstPage(CONTRACT, first)

        change.applyTo(project) { notes }

        assertContinued(CONTRACT, change.description, contractPage(maxChars, first))
    }

    private fun entitiesContinueAfter(change: UnrelatedPsiChange) = runBlockingReleasingWriteIntent<Unit> {
        val first = entitiesPage()
        assertTruncatedFirstPage(ENTITIES, first)

        change.applyTo(project) { notes }

        assertContinued(ENTITIES, change.description, entitiesPage(first))
    }

    private fun beansContinueAfter(change: UnrelatedPsiChange, source: String) = runBlockingReleasingWriteIntent<Unit> {
        if (source == NATIVE) installNativeComponents()
        val first = beansPage(source)
        assertTruncatedFirstPage("$BEANS $source", first)

        change.applyTo(project) { notes }

        assertContinued("$BEANS $source", change.description, beansPage(source, first))
    }

    private suspend fun listingPage(first: JsonNode? = null): JsonNode = mapper.readTree(
        toolset.getHttpEndpoints(
            projectPath = project.basePath,
            controllerFilter = WEB_PACKAGE,
            compact = true,
            limit = 1,
            offset = first?.get("nextOffset")?.asInt() ?: 0,
            expectedRevision = first?.get("revision")?.asText(),
        )
    )

    private suspend fun contractPage(maxChars: Int, first: JsonNode? = null, url: String = ROUTES_URL): JsonNode = mapper.readTree(
        toolset.getEndpointContract(
            urlPattern = url,
            projectPath = project.basePath,
            maxChars = maxChars,
            offset = first?.get("nextOffset")?.asInt() ?: 0,
            expectedRevision = first?.get("revision")?.asText(),
        )
    )

    private suspend fun entitiesPage(first: JsonNode? = null): JsonNode = mapper.readTree(
        toolset.getSpringDataEntities(
            projectPath = project.basePath,
            limit = 2,
            offset = first?.get("nextOffset")?.asInt() ?: 0,
            expectedRevision = first?.get("revision")?.asText(),
        )
    )

    private suspend fun beansPage(source: String, first: JsonNode? = null): JsonNode = mapper.readTree(
        toolset.applicationBeans(
            applicationClassName = DEMO_APPLICATION,
            projectPath = project.basePath,
            beanType = "COMPONENT",
            source = source,
            limit = 1,
            offset = first?.get("nextOffset")?.asInt() ?: 0,
            expectedRevision = first?.get("revision")?.asText(),
        )
    )

    private suspend fun constantRoutePaths(): List<String> = mapper.readTree(
        toolset.getHttpEndpoints(projectPath = project.basePath, controllerFilter = "ConstantController", compact = true)
    )["endpoints"].map { it["fullPath"].asText() }

    private fun addConstantRoute() {
        myFixture.addFileToProject(REVISION_PATHS, REVISION_PATHS_SOURCE)
        myFixture.addFileToProject(CONSTANT_CONTROLLER, CONSTANT_CONTROLLER_SOURCE)
    }

    private fun replaceInSource(path: String, old: String, new: String) {
        val file = myFixture.findFileInTempDir(path)
        assertNotNull("Precondition: $path is in the fixture", file)
        val documents = PsiDocumentManager.getInstance(project)
        val document = myFixture.psiManager.findFile(file!!)?.let(documents::getDocument)
        assertNotNull("Precondition: $path has a document", document)
        val start = document!!.text.indexOf(old)
        assertTrue("Precondition: '$old' occurs once in $path", start >= 0 && document.text.indexOf(old, start + 1) < 0)
        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, start + old.length, new)
            documents.commitDocument(document)
        }
    }

    private suspend fun budgetForOneContract(url: String = ROUTES_URL): Int {
        val unbounded = mapper.readTree(toolset.getEndpointContract(urlPattern = url, projectPath = project.basePath))
        val contracts = unbounded["endpoints"].toList()
        assertTrue("Precondition: at least two contracts match $url, got $unbounded", contracts.size >= 2)
        val envelope = encodedLength(unbounded) - encodedLength(unbounded["endpoints"]) + "[]".length
        return envelope + PAGING_KEYS_ALLOWANCE + contracts.maxOf(::encodedLength)
    }

    private fun installNativeComponents() {
        val application = JavaPsiFacade.getInstance(project)
            .findClass(DEMO_APPLICATION, GlobalSearchScope.projectScope(project))
        assertNotNull("Precondition: $DEMO_APPLICATION resolves", application)
        nativeFixture.install(application!!, listOf(
            "demoService" to "com.example.app.service.DemoService",
            "demoServiceCopy" to "com.example.app.service.DemoService",
        ))
    }

    private fun appendBeforeLastBrace(path: String, member: String) = editSource(path) { text ->
        text.lastIndexOf('}').also { assertTrue("Precondition: $path ends with a brace", it > 0) } to "    $member\n"
    }

    private fun appendAtEnd(path: String, declaration: String) = editSource(path) { text -> text.length to declaration }

    private fun editSource(path: String, insertion: (String) -> Pair<Int, String>) {
        val file = myFixture.findFileInTempDir(path)
        assertNotNull("Precondition: $path is in the fixture", file)
        val documents = PsiDocumentManager.getInstance(project)
        val document = myFixture.psiManager.findFile(file!!)?.let(documents::getDocument)
        assertNotNull("Precondition: $path has a document", document)
        val (offset, text) = insertion(document!!.text)
        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(offset, text)
            documents.commitDocument(document)
        }
    }

    private fun encodedLength(node: JsonNode): Int = mapper.writeValueAsString(node).length

    private companion object {
        const val LIST = "explyt_get_spring_http_endpoints"
        const val CONTRACT = "explyt_get_spring_endpoint_contract"
        const val ENTITIES = "explyt_get_spring_data_entities"
        const val BEANS = "explyt_get_spring_beans"
        const val STATIC = "STATIC"
        const val NATIVE = "NATIVE"

        const val DEMO_APPLICATION = "com.example.app.DemoApplication"
        const val WEB_PACKAGE = "com.example.app.web"
        const val ROUTES_URL = "/api/routes"
        const val ROUTE_CONTROLLER = "com/example/app/web/RouteController.java"
        const val DEMO_ENTITY = "com/example/app/entity/DemoEntity.java"
        const val DEMO_SERVICE = "com/example/app/service/DemoService.java"
        const val PAGING_KEYS_ALLOWANCE = 200


        const val REVISION_URL = "/api/revision"
        const val DEMO_ITEMS_URL = "/api/demo/items"
        const val DEMO_DTO = "com/example/app/dto/DemoDto.java"
        const val REVISION_CONTROLLER = "com/example/app/web/RevisionController.java"
        const val REVISION_PATHS = "com/example/app/web/RevisionPaths.java"
        const val CONSTANT_CONTROLLER = "com/example/app/web/ConstantController.java"

        val REVISION_CONTROLLER_SOURCE = """
            package com.example.app.web;

            import com.example.app.service.DemoService;
            import org.springframework.stereotype.Service;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("/api/revision")
            public class RevisionController {
                private final DemoService service;
                private final RevisionAudit audit;

                public RevisionController(DemoService service, RevisionAudit audit) {
                    this.service = service;
                    this.audit = audit;
                }

                @GetMapping("/first")
                public String first(
                    @org.springframework.web.bind.annotation.RequestParam("alpha") String alpha,
                    @org.springframework.web.bind.annotation.RequestParam("beta") String beta,
                    @org.springframework.web.bind.annotation.RequestParam("gamma") String gamma,
                    @org.springframework.web.bind.annotation.RequestParam("delta") String delta,
                    @org.springframework.web.bind.annotation.RequestParam("epsilon") String epsilon
                ) {
                    return "first";
                }

                @GetMapping("/second")
                public String second() {
                    return "second";
                }
            }

            @Service
            class RevisionAudit {
                public void record() {
                }
            }
        """.trimIndent()

        val REVISION_PATHS_SOURCE = """
            package com.example.app.web;

            public final class RevisionPaths {
                public static final String PATH = "/a";
            }
        """.trimIndent()

        val CONSTANT_CONTROLLER_SOURCE = """
            package com.example.app.web;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("/api/constant")
            public class ConstantController {
                @GetMapping(RevisionPaths.PATH)
                public String constant() {
                    return "constant";
                }
            }
        """.trimIndent()
    }
}
