/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.core.properties.references.ActuatorEndpointKeys
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointAccess
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointExposure
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.explyt.spring.web.view.EndpointsTreeData
import com.explyt.spring.web.view.EndpointsViewFilter
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.toUElementOfType

/**
 * What an Actuator endpoint answers, operation by operation: the verbs and paths its mapping methods declare when it
 * is a controller endpoint, the media types an operation produces, the operations a web extension adds, and nothing
 * at all for an endpoint Boot does not serve because it declares no operation.
 */
class ActuatorEndpointOperationsTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootActuatorAutoConfigure_4_1_0,
        TestLibrary.springBootHealth_4_1_0,
        TestLibrary.springWeb_6_1_4,
    )

    private var testSourceRoot: VirtualFile? = null

    override fun tearDown() {
        try {
            testSourceRoot?.let { PsiTestUtil.removeSourceRoot(module, it) }
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    override fun setUp() {
        super.setUp()
        val scope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        assertNotNull(
            "precondition: @RestControllerEndpoint is on the classpath",
            facade.findClass("org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint", scope)
        )
        assertNotNull(
            "precondition: @GetMapping is on the classpath",
            facade.findClass("org.springframework.web.bind.annotation.GetMapping", scope)
        )
    }

    fun testRestControllerEndpointListsEachMappingWithItsVerbAndMethod() {
        myFixture.addFileToProject(
            "CustomEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PostMapping

            @RestControllerEndpoint(id = "custom")
            class CustomEndpoint {
                @GetMapping("/items")
                fun items(): List<String> = emptyList()

                @PostMapping("/items")
                fun add(): String = "added"
            }
            """.trimIndent()
        )

        val endpoints = endpointsOf("CustomEndpoint")

        assertEquals(
            listOf(
                Triple("/actuator/custom/items", listOf("GET"), "items"),
                Triple("/actuator/custom/items", listOf("POST"), "add"),
            ),
            endpoints.map { Triple(it.path, it.requestMethods, (it.psiElement as? PsiMethod)?.name) }
                .sortedBy { it.second.joinToString() }
        )
        assertEquals(listOf(null, null), endpoints.map { it.producesSource })
    }

    /** A bare `@RequestMapping` accepts any verb, which the endpoint model writes as no verb restriction. */
    fun testControllerEndpointWithABareRequestMappingAcceptsAnyVerb() {
        myFixture.addFileToProject(
            "LegacyEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint
            import org.springframework.web.bind.annotation.RequestMapping

            @ControllerEndpoint(id = "legacy")
            class LegacyEndpoint {
                @RequestMapping
                fun any(): String = "any"
            }
            """.trimIndent()
        )

        val endpoint = endpointsOf("LegacyEndpoint").single()

        assertEquals("/actuator/legacy", endpoint.path)
        assertEquals(emptyList<String>(), endpoint.requestMethods)
        assertEquals("any", (endpoint.psiElement as? PsiMethod)?.name)
    }

    fun testProjectOperationCarriesTheMediaTypesItProduces() {
        myFixture.addFileToProject(
            "ReportEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation

            @Endpoint(id = "report")
            class ReportEndpoint {
                @ReadOperation(produces = ["text/plain"])
                fun text(): String = "report"
            }
            """.trimIndent()
        )

        val endpoint = endpointsOf("ReportEndpoint").single()

        assertEquals(listOf("GET"), endpoint.requestMethods)
        assertEquals(listOf("text/plain"), endpoint.produces)
    }

    fun testBuiltInTextThreadDumpCarriesItsMediaType() {
        myFixture.addFileToProject("DemoApplication.kt", APPLICATION)
        val threadDump = endpointsOf("ThreadDumpEndpoint")
        val text = threadDump.single { (it.psiElement as? PsiMethod)?.name == "textThreadDump" }
        val json = threadDump.single { (it.psiElement as? PsiMethod)?.name == "threadDump" }

        assertTrue("textThreadDump produces text/plain, got ${text.produces}", text.produces.any { it.startsWith("text/plain") })
        assertEquals(
            listOf(
                "application/vnd.spring-boot.actuator.v3+json",
                "application/vnd.spring-boot.actuator.v2+json",
                "application/json",
            ),
            json.produces
        )
    }

    /** Boot discovers an endpoint only when it has an operation, so one declaring none answers 404 and is not listed. */
    fun testEndpointWithoutOperationsIsNotListed() {
        myFixture.addFileToProject(
            "EmptyEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint

            @Endpoint(id = "empty")
            class EmptyEndpoint
            """.trimIndent()
        )
        assertAnnotationResolves(ENDPOINT)
        assertDeclaredEndpoint("empty", "EmptyEndpoint")

        assertNotListed("/actuator/empty")
        assertFalse(
            "no element names its endpoint type as a verb",
            allActuatorEndpoints().any { EndpointType.ACTUATOR.name in it.requestMethods }
        )
    }

    fun testWebEndpointWithoutOperationsIsNotListed() {
        myFixture.addFileToProject(
            "EmptyWebEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint

            @WebEndpoint(id = "web")
            class EmptyWebEndpoint
            """.trimIndent()
        )
        assertAnnotationResolves(WEB_ENDPOINT)
        assertDeclaredEndpoint("web", "EmptyWebEndpoint")

        assertNotListed("/actuator/web")
    }

    /** Boot serves the operations an `@EndpointWebExtension` bean declares under the id of the endpoint it extends. */
    fun testKotlinWebExtensionOperationIsListedUnderItsEndpoint() {
        myFixture.addFileToProject("DemoApplication.kt", APPLICATION)
        myFixture.addFileToProject(
            "ExtEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension
            import org.springframework.stereotype.Component

            @Endpoint(id = "ext")
            @Component
            class ExtEndpoint

            @EndpointWebExtension(endpoint = ExtEndpoint::class)
            @Component
            class ExtWebExtension {
                @ReadOperation
                fun read(): String = "x"
            }
            """.trimIndent()
        )

        assertWebExtensionReadIsListed()
    }

    fun testJavaWebExtensionOperationIsListedUnderItsEndpoint() {
        myFixture.addFileToProject("DemoApplication.kt", APPLICATION)
        myFixture.addFileToProject(
            "ExtEndpoint.java",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
            import org.springframework.stereotype.Component;

            @Endpoint(id = "ext")
            @Component
            public class ExtEndpoint {
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "ExtWebExtension.java",
            """
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
            import org.springframework.stereotype.Component;

            @EndpointWebExtension(endpoint = ExtEndpoint.class)
            @Component
            public class ExtWebExtension {
                @ReadOperation
                public String read() { return "x"; }
            }
            """.trimIndent()
        )

        assertWebExtensionReadIsListed()
    }

    fun testWebExtensionOperationJoinsTheEndpointOwnOperations() {
        myFixture.addFileToProject("DemoApplication.kt", APPLICATION)
        myFixture.addFileToProject(
            "MixEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.boot.actuate.endpoint.annotation.WriteOperation
            import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension
            import org.springframework.stereotype.Component

            @Endpoint(id = "mix")
            @Component
            class MixEndpoint {
                @ReadOperation
                fun read(): String = "x"
            }

            @EndpointWebExtension(endpoint = MixEndpoint::class)
            @Component
            class MixWebExtension {
                @WriteOperation
                fun write(value: String) {}
            }
            """.trimIndent()
        )
        assertAnnotationResolves(ENDPOINT_WEB_EXTENSION)
        assertDeclaredEndpoint("mix", "MixEndpoint")
        assertWebExtensionOf("MixWebExtension")

        val elements = allActuatorEndpoints().filter { it.path == "/actuator/mix" }

        assertEquals(
            listOf(
                Triple(listOf("GET"), "MixEndpoint", "read"),
                Triple(listOf("POST"), "MixWebExtension", "write"),
            ),
            elements.map {
                val method = it.psiElement as? PsiMethod
                Triple(it.requestMethods, method?.containingClass?.name, method?.name)
            }.sortedBy { it.first.joinToString() }
        )
    }

    /** A servlet endpoint has no operation yet Boot serves it, so it stays listed. */
    fun testServletEndpointStaysListed() {
        myFixture.addFileToProject(
            "SrvEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.web.EndpointServlet
            import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint
            import java.util.function.Supplier

            @Suppress("DEPRECATION", "removal")
            @ServletEndpoint(id = "srv")
            class SrvEndpoint : Supplier<EndpointServlet> {
                override fun get(): EndpointServlet = TODO()
            }
            """.trimIndent()
        )
        assertAnnotationResolves(SERVLET_ENDPOINT)
        assertDeclaredEndpoint("srv", "SrvEndpoint")

        val endpoint = allActuatorEndpoints().single { it.path == "/actuator/srv" }

        assertEquals("SrvEndpoint", endpoint.containingClass?.name)
        assertEquals(emptyList<String>(), endpoint.requestMethods)
    }

    fun testEndpointWithAReadOperationIsListedAsGet() {
        myFixture.addFileToProject(
            "PlainEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation

            @Endpoint(id = "plain")
            class PlainEndpoint {
                @ReadOperation
                fun read(): String = "x"
            }
            """.trimIndent()
        )
        assertDeclaredEndpoint("plain", "PlainEndpoint")

        val endpoint = allActuatorEndpoints().single { it.path == "/actuator/plain" }

        assertEquals(listOf("GET"), endpoint.requestMethods)
        assertEquals("read", (endpoint.psiElement as? PsiMethod)?.name)
    }

    /** Boot keys an operation by its request predicate, so an extension operation with the same one replaces it. */
    fun testWebExtensionOperationReplacesTheEndpointOperationWithTheSamePredicate() {
        addExtendedEndpoint(
            endpointBody = "@ReadOperation fun read(): String = \"endpoint\"",
            extensionBody = "@ReadOperation fun read(): String = \"extension\"",
        )

        assertEquals(
            listOf(Triple(listOf("GET"), "XWebExtension", "read")),
            operationsAt("/actuator/x")
        )
    }

    fun testJavaWebExtensionOperationReplacesTheEndpointOperationWithTheSamePredicate() {
        myFixture.addFileToProject("DemoApplication.kt", APPLICATION)
        myFixture.addFileToProject(
            "XEndpoint.java",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            import org.springframework.stereotype.Component;

            @Endpoint(id = "x")
            @Component
            public class XEndpoint {
                @ReadOperation
                public String read() { return "endpoint"; }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "XWebExtension.java",
            """
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
            import org.springframework.stereotype.Component;

            @EndpointWebExtension(endpoint = XEndpoint.class)
            @Component
            public class XWebExtension {
                @ReadOperation
                public String read() { return "extension"; }
            }
            """.trimIndent()
        )
        assertExtendedEndpointPreconditions()

        assertEquals(
            listOf(Triple(listOf("GET"), "XWebExtension", "read")),
            operationsAt("/actuator/x")
        )
    }

    /** A non-selector parameter is a query parameter, so the predicate and the replaced operation stay the same. */
    fun testWebExtensionOperationWithAnotherSignatureReplacesTheSamePredicate() {
        addExtendedEndpoint(
            endpointBody = "@ReadOperation fun read(): String = \"endpoint\"",
            extensionBody = "@ReadOperation fun read(filter: String?): String = filter.orEmpty()",
        )

        assertEquals(
            listOf(Triple(listOf("GET"), "XWebExtension", "read")),
            operationsAt("/actuator/x")
        )
        val extensionRead = allActuatorEndpoints().single { it.path == "/actuator/x" }.psiElement as PsiMethod
        assertEquals(listOf("filter"), extensionRead.parameterList.parameters.map { it.name })
    }

    fun testWebExtensionRootOperationKeepsTheEndpointSelectorOperation() {
        addExtendedEndpoint(
            endpointBody = "@ReadOperation fun read(@Selector name: String): String = name",
            extensionBody = "@ReadOperation fun read(): String = \"extension\"",
        )

        assertEquals(
            listOf(
                Triple("/actuator/x", listOf("GET"), "XWebExtension"),
                Triple("/actuator/x/{name}", listOf("GET"), "XEndpoint"),
            ),
            allActuatorEndpoints().filter { it.path.startsWith("/actuator/x") }
                .map { Triple(it.path, it.requestMethods, (it.psiElement as? PsiMethod)?.containingClass?.name) }
                .sortedBy { it.first }
        )
    }

    fun testWebExtensionOperationWithOtherProducesJoinsTheEndpointOperation() {
        addExtendedEndpoint(
            endpointBody = "@ReadOperation(produces = [\"text/plain\"]) fun read(): String = \"endpoint\"",
            extensionBody = "@ReadOperation(produces = [\"application/json\"]) fun read(): String = \"extension\"",
        )

        assertEquals(
            listOf(
                Triple(listOf("GET"), listOf("application/json"), "XWebExtension"),
                Triple(listOf("GET"), listOf("text/plain"), "XEndpoint"),
            ),
            allActuatorEndpoints().filter { it.path == "/actuator/x" }
                .map { Triple(it.requestMethods, it.produces, (it.psiElement as? PsiMethod)?.containingClass?.name) }
                .sortedBy { it.second.joinToString() }
        )
    }

    fun testProjectWebExtensionOfABuiltInEndpointReplacesItsOperation() {
        myFixture.addFileToProject("DemoApplication.kt", APPLICATION)
        myFixture.addFileToProject(
            "CustomInfoWebExtension.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension
            import org.springframework.boot.actuate.info.InfoEndpoint
            import org.springframework.stereotype.Component

            @EndpointWebExtension(endpoint = InfoEndpoint::class)
            @Component
            class CustomInfoWebExtension {
                @ReadOperation
                fun info(): Map<String, Any> = emptyMap()
            }
            """.trimIndent()
        )
        assertAnnotationResolves(ENDPOINT_WEB_EXTENSION)
        assertAnnotationResolves(INFO_ENDPOINT)
        assertEquals(
            "precondition: the loader discovers the built-in InfoEndpoint under id 'info'",
            listOf(INFO_ENDPOINT),
            ActuatorEndpointKeys.libraryEndpointsById(module)["info"].orEmpty().map { it.psiClass.qualifiedName }
        )
        assertWebExtensionOf("CustomInfoWebExtension")

        assertEquals(
            listOf(Triple(listOf("GET"), "CustomInfoWebExtension", "info")),
            operationsAt("/actuator/info")
        )
    }

    /** Boot applies a test-source extension only in a test context, so it must not reshape a production endpoint. */
    fun testTestSourceWebExtensionLeavesAProductionEndpointUnchanged() {
        addTestSourceRoot()
        myFixture.addFileToProject("DemoApplication.kt", APPLICATION)
        myFixture.addFileToProject("XEndpoint.kt", X_ENDPOINT)
        myFixture.addFileToProject("$TEST_SOURCES/XTestWebExtension.kt", X_TEST_WEB_EXTENSION)
        assertAnnotationResolves(ENDPOINT)
        assertAnnotationResolves(ENDPOINT_WEB_EXTENSION)
        assertDeclaredEndpoint("x", "XEndpoint")
        assertWebExtensionOf("XTestWebExtension")
        assertTestSource("XTestWebExtension", expected = true)
        assertTestSource("XEndpoint", expected = false)

        assertEquals(
            listOf(Triple(listOf("GET"), "XEndpoint", "read")),
            operationsAt("/actuator/x")
        )
    }

    fun testTestSourceWebExtensionAppliesToATestSourceEndpoint() {
        addTestSourceRoot()
        myFixture.addFileToProject("DemoApplication.kt", APPLICATION)
        myFixture.addFileToProject("$TEST_SOURCES/XEndpoint.kt", X_ENDPOINT)
        myFixture.addFileToProject("$TEST_SOURCES/XTestWebExtension.kt", X_TEST_WEB_EXTENSION)
        assertAnnotationResolves(ENDPOINT)
        assertAnnotationResolves(ENDPOINT_WEB_EXTENSION)
        assertDeclaredEndpoint("x", "XEndpoint")
        assertWebExtensionOf("XTestWebExtension")
        assertTestSource("XTestWebExtension", expected = true)
        assertTestSource("XEndpoint", expected = true)

        assertEquals(
            listOf(
                Triple(listOf("GET"), "XTestWebExtension", "read"),
                Triple(listOf("POST"), "XTestWebExtension", "write"),
            ),
            operationsAt("/actuator/x").sortedBy { it.first.joinToString() }
        )
    }

    /** Boot compares path variables by position, so selectors named differently still share one request predicate. */
    fun testWebExtensionSelectorWithAnotherNameReplacesTheEndpointSelectorOperation() {
        addExtendedEndpoint(
            endpointBody = "@ReadOperation fun read(@Selector name: String): String = name",
            extensionBody = "@ReadOperation fun read(@Selector id: String): String = id",
        )

        assertEquals(
            listOf(Triple("/actuator/x/{id}", listOf("GET"), "XWebExtension")),
            selectorOperationsOfX()
        )
    }

    fun testWebExtensionAllRemainingSelectorWithAnotherNameReplacesTheEndpointOne() {
        addExtendedEndpoint(
            endpointBody = "@ReadOperation fun read(@Selector(match = Selector.Match.ALL_REMAINING) path: String): String = path",
            extensionBody = "@ReadOperation fun read(@Selector(match = Selector.Match.ALL_REMAINING) segments: String): String = segments",
        )

        assertEquals(
            listOf(Triple("/actuator/x/{*segments}", listOf("GET"), "XWebExtension")),
            selectorOperationsOfX()
        )
    }

    fun testWebExtensionAllRemainingSelectorKeepsTheEndpointSingleSegmentSelector() {
        addExtendedEndpoint(
            endpointBody = "@ReadOperation fun read(@Selector name: String): String = name",
            extensionBody = "@ReadOperation fun read(@Selector(match = Selector.Match.ALL_REMAINING) rest: String): String = rest",
        )

        assertEquals(
            listOf(
                Triple("/actuator/x/{*rest}", listOf("GET"), "XWebExtension"),
                Triple("/actuator/x/{name}", listOf("GET"), "XEndpoint"),
            ),
            selectorOperationsOfX()
        )
    }

    /**
     * Boot sets `consumes` for a POST operation with a body, so a body write and a body-less write are two request
     * predicates: a JSON body reaches the endpoint's `write(value)`, an empty POST the extension's `write()`.
     */
    fun testWebExtensionBodylessWriteKeepsTheEndpointBodyWrite() {
        addExtendedEndpoint(
            endpointBody = "$WRITE_OPERATION fun write(value: String) {}",
            extensionBody = "$WRITE_OPERATION fun write() {}",
        )

        assertEquals(
            listOf(
                WriteOperation("/actuator/x", "XEndpoint", listOf("value")),
                WriteOperation("/actuator/x", "XWebExtension", emptyList()),
            ),
            writeOperationsOfX()
        )
    }

    fun testWebExtensionBodyWriteKeepsTheEndpointBodylessWrite() {
        addExtendedEndpoint(
            endpointBody = "$WRITE_OPERATION fun write() {}",
            extensionBody = "$WRITE_OPERATION fun write(value: String) {}",
        )

        assertEquals(
            listOf(
                WriteOperation("/actuator/x", "XEndpoint", emptyList()),
                WriteOperation("/actuator/x", "XWebExtension", listOf("value")),
            ),
            writeOperationsOfX()
        )
    }

    fun testWebExtensionBodyWriteReplacesTheEndpointBodyWrite() {
        addExtendedEndpoint(
            endpointBody = "$WRITE_OPERATION fun write(value: String) {}",
            extensionBody = "$WRITE_OPERATION fun write(value: String) {}",
        )

        assertEquals(
            listOf(WriteOperation("/actuator/x", "XWebExtension", listOf("value"))),
            writeOperationsOfX()
        )
    }

    /** A `@Selector` parameter is a path variable, not a body, so neither write consumes anything. */
    fun testWebExtensionSelectorOnlyWriteReplacesTheEndpointSelectorOnlyWrite() {
        addExtendedEndpoint(
            endpointBody = "$WRITE_OPERATION fun write(@Selector name: String) {}",
            extensionBody = "$WRITE_OPERATION fun write(@Selector id: String) {}",
        )

        assertEquals(
            listOf(WriteOperation("/actuator/x/{id}", "XWebExtension", listOf("id"))),
            writeOperationsOfX()
        )
    }

    fun testWebExtensionSelectorWriteWithABodyKeepsTheEndpointSelectorOnlyWrite() {
        addExtendedEndpoint(
            endpointBody = "$WRITE_OPERATION fun write(@Selector name: String) {}",
            extensionBody = "$WRITE_OPERATION fun write(@Selector id: String, value: String) {}",
        )

        assertEquals(
            listOf(
                WriteOperation("/actuator/x/{id}", "XWebExtension", listOf("id", "value")),
                WriteOperation("/actuator/x/{name}", "XEndpoint", listOf("name")),
            ),
            writeOperationsOfX()
        )
    }

    private data class WriteOperation(val path: String, val declaringClass: String?, val parameters: List<String>)

    private fun writeOperationsOfX(): List<WriteOperation> {
        assertAnnotationResolves(WRITE_OPERATION.removePrefix("@"))
        return allActuatorEndpoints()
            .filter { (it.path == "/actuator/x" || it.path.startsWith("/actuator/x/")) && it.requestMethods == listOf("POST") }
            .map {
                val method = it.psiElement as? PsiMethod
                WriteOperation(
                    it.path,
                    method?.containingClass?.name,
                    method?.parameterList?.parameters?.map { parameter -> parameter.name }.orEmpty()
                )
            }
            .sortedWith(compareBy({ it.path }, { it.declaringClass }))
    }

    private fun selectorOperationsOfX(): List<Triple<String, List<String>, String?>> =
        allActuatorEndpoints().filter { it.path.startsWith("/actuator/x/") || it.path == "/actuator/x" }
            .map { Triple(it.path, it.requestMethods, (it.psiElement as? PsiMethod)?.containingClass?.name) }
            .sortedBy { it.first }

    private fun addTestSourceRoot() {
        val root = myFixture.tempDirFixture.findOrCreateDir(TEST_SOURCES)
        PsiTestUtil.addSourceRoot(module, root, true)
        testSourceRoot = root
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private fun assertTestSource(className: String, expected: Boolean) {
        val file = JavaPsiFacade.getInstance(project)
            .findClass(className, GlobalSearchScope.projectScope(project))?.containingFile?.virtualFile
        assertNotNull("precondition: $className is in the project", file)
        assertEquals(
            "precondition: $className is ${if (expected) "" else "not "}in test sources",
            expected,
            ProjectFileIndex.getInstance(project).isInTestSourceContent(file!!)
        )
    }

    private fun addExtendedEndpoint(endpointBody: String, extensionBody: String) {
        myFixture.addFileToProject("DemoApplication.kt", APPLICATION)
        myFixture.addFileToProject(
            "XEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.boot.actuate.endpoint.annotation.Selector
            import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension
            import org.springframework.stereotype.Component

            @Endpoint(id = "x")
            @Component
            class XEndpoint {
                $endpointBody
            }

            @EndpointWebExtension(endpoint = XEndpoint::class)
            @Component
            class XWebExtension {
                $extensionBody
            }
            """.trimIndent()
        )
        assertExtendedEndpointPreconditions()
    }

    private fun assertExtendedEndpointPreconditions() {
        assertAnnotationResolves(ENDPOINT)
        assertAnnotationResolves(ENDPOINT_WEB_EXTENSION)
        assertDeclaredEndpoint("x", "XEndpoint")
        assertWebExtensionOf("XWebExtension")
    }

    private fun operationsAt(path: String): List<Triple<List<String>, String?, String?>> =
        allActuatorEndpoints().filter { it.path == path }.map {
            val method = it.psiElement as? PsiMethod
            Triple(it.requestMethods, method?.containingClass?.name, method?.name)
        }

    private fun assertWebExtensionReadIsListed() {
        myFixture.addFileToProject(
            "application.properties",
            """
            management.endpoints.web.exposure.include=ext
            management.endpoint.ext.access=read-only
            """.trimIndent()
        )
        assertAnnotationResolves(ENDPOINT_WEB_EXTENSION)
        assertDeclaredEndpoint("ext", "ExtEndpoint")
        assertWebExtensionOf("ExtWebExtension")

        val elements = allActuatorEndpoints().filter { it.path == "/actuator/ext" }

        assertEquals(
            listOf(Triple(listOf("GET"), "ExtWebExtension", "read")),
            elements.map {
                val method = it.psiElement as? PsiMethod
                Triple(it.requestMethods, method?.containingClass?.name, method?.name)
            }
        )
        val element = elements.single()
        assertEquals(EndpointExposure.EXPOSED, element.exposure)
        assertEquals(EndpointAccess.READ_ONLY, element.access)
    }

    private fun assertAnnotationResolves(fqn: String) {
        assertNotNull(
            "precondition: $fqn is on the classpath",
            JavaPsiFacade.getInstance(project).findClass(fqn, module.getModuleWithDependenciesAndLibrariesScope(false))
        )
    }

    private fun assertDeclaredEndpoint(id: String, className: String) {
        assertEquals(
            "precondition: the loader discovers $className under id '$id'",
            listOf(className),
            ActuatorEndpointKeys.endpointsById(module)[id].orEmpty().map { it.psiClass.name }
        )
    }

    private fun assertWebExtensionOf(className: String) {
        val extension = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.projectScope(project))
        assertNotNull("precondition: $className is in the project", extension)
        assertNotNull(
            "precondition: $className carries a resolved @EndpointWebExtension",
            extension!!.getAnnotation(ENDPOINT_WEB_EXTENSION)
        )
    }

    private fun assertNotListed(path: String) {
        val endpoints = allActuatorEndpoints()
        assertEquals(
            "no element at $path",
            emptyList<String>(),
            endpoints.filter { it.path == path }.map { "${it.requestMethods} ${it.path}" }
        )
        assertEquals(
            "no tool-window row at $path",
            emptyList<String>(),
            endpoints.flatMap { EndpointsTreeData.rowsOf(it) }.filter { it.path == path }.map { "${it.method} ${it.path}" }
        )
    }

    fun testHealthAllRemainingSelectorDeclaresACaptureRestPath() {
        val endpoint = healthForPath()

        assertEquals("/actuator/health/{*path}", endpoint.path)
    }

    fun testToolWindowFindsHealthForANestedComponentPath() {
        val endpoint = healthForPath()
        val rows = EndpointsTreeData.rowsOf(endpoint)

        val matched = EndpointsViewFilter.apply(
            rows, "/actuator/health/db/redis", emptySet(), emptySet(), EndpointsViewFilter.declaredBasePaths()
        )

        assertEquals(listOf("/actuator/health/{*path}"), matched.map { it.path })
    }

    fun testJavaProjectSelectorCapturesAllRemaining() {
        myFixture.addFileToProject("ProjectEndpoint.java", """
            import org.springframework.boot.actuate.endpoint.annotation.*;
            @Endpoint(id = "project")
            public class ProjectEndpoint {
                @ReadOperation
                public String read(@Selector(match = Selector.Match.ALL_REMAINING) String path) { return path; }
            }
        """.trimIndent())

        assertProjectSelectorPath("/actuator/project/{*path}", 0)
    }

    fun testJavaStaticImportedSelectorCapturesAllRemaining() {
        myFixture.addFileToProject("ProjectEndpoint.java", """
            import org.springframework.boot.actuate.endpoint.annotation.*;
            import static org.springframework.boot.actuate.endpoint.annotation.Selector.Match.ALL_REMAINING;
            @Endpoint(id = "project")
            public class ProjectEndpoint {
                @ReadOperation
                public String read(@Selector(match = ALL_REMAINING) String path) { return path; }
            }
        """.trimIndent())

        assertProjectSelectorPath("/actuator/project/{*path}", 0)
    }

    fun testKotlinProjectSelectorCapturesAllRemaining() {
        myFixture.addFileToProject("ProjectEndpoint.kt", """
            import org.springframework.boot.actuate.endpoint.annotation.*
            @Endpoint(id = "project")
            class ProjectEndpoint {
                @ReadOperation
                fun read(@Selector(match = Selector.Match.ALL_REMAINING) path: String) = path
            }
        """.trimIndent())

        assertProjectSelectorPath("/actuator/project/{*path}", 0)
    }

    fun testOnlyTheLastSelectorCapturesRemainingSegments() {
        myFixture.addFileToProject("ProjectEndpoint.java", """
            import org.springframework.boot.actuate.endpoint.annotation.*;
            @Endpoint(id = "project")
            public class ProjectEndpoint {
                @ReadOperation
                public String read(@Selector String a, @Selector(match = Selector.Match.ALL_REMAINING) String b) { return b; }
            }
        """.trimIndent())

        assertProjectSelectorPath("/actuator/project/{a}/{*b}", 1)
    }

    fun testInvalidNonLastAllRemainingSelectorStaysASingleSegment() {
        myFixture.addFileToProject("ProjectEndpoint.java", """
            import org.springframework.boot.actuate.endpoint.annotation.*;
            @Endpoint(id = "project")
            public class ProjectEndpoint {
                @ReadOperation
                public String read(@Selector(match = Selector.Match.ALL_REMAINING) String a, @Selector String b) { return b; }
            }
        """.trimIndent())

        assertProjectSelectorPath("/actuator/project/{a}/{b}", 0)
    }

    private fun assertProjectSelectorPath(expected: String, allRemainingIndex: Int) {
        val endpoint = endpointsOf("ProjectEndpoint").single()
        val method = endpoint.psiElement as PsiMethod
        val selector = method.parameterList.parameters[allRemainingIndex].getAnnotation(
            "org.springframework.boot.actuate.endpoint.annotation.Selector"
        )
        assertNotNull("precondition: project parameter carries @Selector", selector)
        val psiMatch = (selector!!.findAttributeValue("match") as? PsiReferenceExpression)?.resolve() as? PsiEnumConstant
        val uastMatch = selector.toUElementOfType<UAnnotation>()?.findAttributeValue("match") as? UResolvable
        val match = psiMatch ?: uastMatch?.resolve() as? PsiEnumConstant
        assertEquals("precondition: selector carries ALL_REMAINING", "ALL_REMAINING", match?.name)
        assertEquals("org.springframework.boot.actuate.endpoint.annotation.Selector.Match", match?.containingClass?.qualifiedName)
        assertEquals(listOf("GET"), endpoint.requestMethods)
        assertEquals(expected, endpoint.path)
    }

    private fun healthForPath(): EndpointElement {
        myFixture.addFileToProject("DemoApplication.kt", APPLICATION)
        val endpoint = endpointsOf("HealthEndpoint").single { (it.psiElement as? PsiMethod)?.name == "healthForPath" }
        val method = endpoint.psiElement as PsiMethod
        val selector = method.parameterList.parameters.last().getAnnotation(
            "org.springframework.boot.actuate.endpoint.annotation.Selector"
        )
        assertNotNull("precondition: healthForPath carries @Selector", selector)
        val match = (selector!!.findAttributeValue("match") as? PsiReferenceExpression)?.resolve() as? PsiEnumConstant
        assertEquals("precondition: healthForPath captures ALL_REMAINING", "ALL_REMAINING", match?.name)
        assertEquals(
            "org.springframework.boot.actuate.endpoint.annotation.Selector.Match",
            match?.containingClass?.qualifiedName
        )
        return endpoint
    }

    private fun endpointsOf(className: String): List<EndpointElement> =
        allActuatorEndpoints().filter { it.containingClass?.name == className }

    private fun allActuatorEndpoints(): List<EndpointElement> =
        SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .toList()

    private companion object {
        const val ENDPOINT = "org.springframework.boot.actuate.endpoint.annotation.Endpoint"
        const val WEB_ENDPOINT = "org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint"
        const val SERVLET_ENDPOINT = "org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint"
        const val ENDPOINT_WEB_EXTENSION = "org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension"
        const val INFO_ENDPOINT = "org.springframework.boot.actuate.info.InfoEndpoint"
        const val WRITE_OPERATION = "@org.springframework.boot.actuate.endpoint.annotation.WriteOperation"
        const val TEST_SOURCES = "src/test/kotlin"

        val X_ENDPOINT = """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.stereotype.Component

            @Endpoint(id = "x")
            @Component
            class XEndpoint {
                @ReadOperation
                fun read(): String = "endpoint"
            }
            """.trimIndent()

        val X_TEST_WEB_EXTENSION = """
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.boot.actuate.endpoint.annotation.WriteOperation
            import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension
            import org.springframework.stereotype.Component

            @EndpointWebExtension(endpoint = XEndpoint::class)
            @Component
            class XTestWebExtension {
                @ReadOperation
                fun read(): String = "test extension"

                @WriteOperation
                fun write(value: String) {}
            }
            """.trimIndent()
    }
}
