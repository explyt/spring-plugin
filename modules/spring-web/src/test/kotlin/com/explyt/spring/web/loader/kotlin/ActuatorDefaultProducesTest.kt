/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

class ActuatorDefaultProducesTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootActuatorAutoConfigure_4_1_0,
        TestLibrary.springBootHealth_4_1_0,
        TestLibrary.springWeb_6_1_4,
    )

    override fun setUp() {
        super.setUp()
        val scope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        assertNotNull(
            "precondition: Producible is on the classpath",
            facade.findClass("org.springframework.boot.actuate.endpoint.Producible", scope)
        )
        assertNotNull(
            "precondition: Resource is on the classpath",
            facade.findClass("org.springframework.core.io.Resource", scope)
        )
        assertNotNull(
            "precondition: MediaType is on the classpath",
            facade.findClass("org.springframework.http.MediaType", scope)
        )
    }

    fun testBuiltInHealthReadOperationProducesTheEndpointDefaults() {
        myFixture.addFileToProject("DemoApplication.kt", APPLICATION)

        val health = operation("HealthEndpoint", "health")

        assertEquals("/actuator/health", health.path)
        assertEquals(DEFAULT_PRODUCES, health.produces)
    }

    fun testProjectReadOperationReturningAnObjectProducesTheEndpointDefaults() {
        addEndpoint(
            """
            @ReadOperation
            fun report(): Report = Report("ok")
            """
        )

        assertEquals(DEFAULT_PRODUCES, operation("ReportEndpoint", "report").produces)
    }

    fun testDeclaredProducesReplacesTheDefaults() {
        addEndpoint(
            """
            @ReadOperation(produces = ["text/plain"])
            fun report(): String = "ok"
            """
        )

        assertEquals(listOf("text/plain"), operation("ReportEndpoint", "report").produces)
    }

    fun testVoidWriteOperationProducesNothing() {
        addEndpoint(
            """
            @WriteOperation
            fun reset(name: String) {
            }
            """
        )

        val reset = operation("ReportEndpoint", "reset")

        assertEquals(listOf("POST"), reset.requestMethods)
        assertEquals(emptyList<String>(), reset.produces)
    }


    fun testKotlinUnitReadOperationProducesNothing() {
        addEndpoint(
            """
            @ReadOperation
            fun report(): Unit = Unit
            """
        )

        assertEquals(emptyList<String>(), operation("ReportEndpoint", "report").produces)
    }

    fun testKotlinNullableVoidWriteOperationProducesNothing() {
        addEndpoint(
            """
            @WriteOperation
            fun reset(): Void? = null
            """
        )

        assertEquals(emptyList<String>(), operation("ReportEndpoint", "reset").produces)
    }

    fun testJavaVoidWriteOperationProducesNothing() {
        myFixture.addFileToProject(
            "JavaVoidEndpoint.java",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
            import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

            @Endpoint(id = "java-void")
            public class JavaVoidEndpoint {
                @WriteOperation
                public Void reset() { return null; }
            }
            """.trimIndent()
        )

        assertEquals(emptyList<String>(), operation("JavaVoidEndpoint", "reset").produces)
    }

    fun testResourceReadOperationProducesAnOctetStream() {
        addEndpoint(
            """
            @ReadOperation
            fun download(): org.springframework.core.io.Resource = org.springframework.core.io.ByteArrayResource(ByteArray(0))
            """
        )

        assertEquals(listOf(OCTET_STREAM), operation("ReportEndpoint", "download").produces)
    }

    fun testDeclaredProducesConstantIsEvaluated() {
        myFixture.addFileToProject(
            "ReportMediaTypes.kt",
            """
            object ReportMediaTypes {
                const val CSV = "text/csv"
            }
            """.trimIndent()
        )
        addEndpoint(
            """
            @ReadOperation(produces = [ReportMediaTypes.CSV, org.springframework.http.MediaType.TEXT_PLAIN_VALUE])
            fun report(): String = "ok"
            """
        )

        assertEquals(listOf("text/csv", "text/plain"), operation("ReportEndpoint", "report").produces)
    }

    fun testJavaDeclaredProducesConstantIsEvaluated() {
        addJavaEndpoint(
            """
            static final String CSV = "text/csv";

            @ReadOperation(produces = {CSV, org.springframework.http.MediaType.TEXT_PLAIN_VALUE})
            public String report() { return "ok"; }
            """
        )

        assertEquals(listOf("text/csv", "text/plain"), operation("JavaReportEndpoint", "report").produces)
    }

    fun testProducesFromIgnoresAConstructorStringTheMimeTypeIsNotReadFrom() {
        myFixture.addFileToProject(
            "ReportFormat.kt",
            """
            import org.springframework.boot.actuate.endpoint.Producible
            import org.springframework.util.MimeType

            enum class ReportFormat(val label: String, private val mimeType: String) : Producible<ReportFormat> {
                CSV("CSV", dynamicMime()),
                JSON("JSON", "application/json");

                override fun getProducedMimeType(): MimeType = MimeType.valueOf(mimeType)
            }

            fun dynamicMime(): String = "text/csv"
            """.trimIndent()
        )
        addEndpoint(
            """
            @ReadOperation(producesFrom = ReportFormat::class)
            fun report(format: ReportFormat): String = format.name
            """
        )

        assertEquals(listOf("application/json"), operation("ReportEndpoint", "report").produces)
    }

    fun testProducesFromReadsOnlyTheReturnedMimeType() {
        myFixture.addFileToProject(
            "ReportFormat.kt",
            """
            import org.springframework.boot.actuate.endpoint.Producible
            import org.springframework.util.MimeType

            enum class ReportFormat : Producible<ReportFormat> {
                CSV;

                override fun getProducedMimeType(): MimeType {
                    MimeType.valueOf("text/plain")
                    return MimeType.valueOf("text/csv")
                }
            }
            """.trimIndent()
        )
        addEndpoint(
            """
            @ReadOperation(producesFrom = ReportFormat::class)
            fun report(format: ReportFormat): String = format.name
            """
        )

        assertEquals(listOf("text/csv"), operation("ReportEndpoint", "report").produces)
    }

    fun testJavaProducesFromReadsTheConstructorArgumentOfTheReturnedField() {
        myFixture.addFileToProject(
            "JavaReportFormat.java",
            """
            import org.springframework.boot.actuate.endpoint.Producible;
            import org.springframework.util.MimeType;

            public enum JavaReportFormat implements Producible<JavaReportFormat> {
                CSV("CSV", "text/csv"),
                DYNAMIC("DYNAMIC", String.valueOf("text/x-dynamic"));

                private final String label;
                private final MimeType mimeType;

                JavaReportFormat(String label, String mimeType) {
                    this.label = label;
                    this.mimeType = MimeType.valueOf(mimeType);
                }

                @Override
                public MimeType getProducedMimeType() {
                    return mimeType;
                }
            }
            """.trimIndent()
        )
        addJavaEndpoint(
            """
            @ReadOperation(producesFrom = JavaReportFormat.class)
            public String report(JavaReportFormat format) { return format.name(); }
            """
        )

        assertEquals(listOf("text/csv"), operation("JavaReportEndpoint", "report").produces)
    }

    fun testProducesFromParsedMediaTypeConstantIsRead() {
        myFixture.addFileToProject(
            "ReportFormat.kt",
            """
            import org.springframework.boot.actuate.endpoint.Producible
            import org.springframework.http.MediaType
            import org.springframework.util.MimeType

            enum class ReportFormat : Producible<ReportFormat> {
                TEXT;

                override fun getProducedMimeType(): MimeType = MediaType.parseMediaType(MediaType.TEXT_PLAIN_VALUE)
            }
            """.trimIndent()
        )
        addEndpoint(
            """
            @ReadOperation(producesFrom = ReportFormat::class)
            fun report(format: ReportFormat): String = format.name
            """
        )

        assertEquals(listOf("text/plain"), operation("ReportEndpoint", "report").produces)
    }

    fun testWebEndpointResponseOfAResourceSubtypeProducesAnOctetStream() {
        addEndpoint(
            """
            @ReadOperation
            fun download(): org.springframework.boot.actuate.endpoint.web.WebEndpointResponse<org.springframework.core.io.ByteArrayResource> =
                org.springframework.boot.actuate.endpoint.web.WebEndpointResponse(org.springframework.core.io.ByteArrayResource(ByteArray(0)))
            """
        )

        assertEquals(listOf(OCTET_STREAM), operation("ReportEndpoint", "download").produces)
    }

    fun testJavaWebEndpointResponseOfAResourceSubtypeProducesAnOctetStream() {
        addJavaEndpoint(
            """
            @ReadOperation
            public org.springframework.boot.actuate.endpoint.web.WebEndpointResponse<org.springframework.core.io.ByteArrayResource> download() {
                return null;
            }
            """
        )

        assertEquals(listOf(OCTET_STREAM), operation("JavaReportEndpoint", "download").produces)
    }

    fun testDirectResourceSubtypeReturnProducesTheEndpointDefaults() {
        addEndpoint(
            """
            @ReadOperation
            fun download(): org.springframework.core.io.ByteArrayResource = org.springframework.core.io.ByteArrayResource(ByteArray(0))
            """
        )

        assertEquals(DEFAULT_PRODUCES, operation("ReportEndpoint", "download").produces)
    }

    fun testWebEndpointResponseOfResourceProducesAnOctetStream() {
        addEndpoint(
            """
            @ReadOperation
            fun download(): org.springframework.boot.actuate.endpoint.web.WebEndpointResponse<org.springframework.core.io.Resource> =
                org.springframework.boot.actuate.endpoint.web.WebEndpointResponse(org.springframework.core.io.ByteArrayResource(ByteArray(0)))
            """
        )

        assertEquals(listOf(OCTET_STREAM), operation("ReportEndpoint", "download").produces)
    }

    fun testProducesFromListsEveryConstantOfTheProducibleEnum() {
        myFixture.addFileToProject(
            "ReportFormat.kt",
            """
            import org.springframework.boot.actuate.endpoint.Producible
            import org.springframework.util.MimeType

            enum class ReportFormat(private val mimeType: String) : Producible<ReportFormat> {
                CSV("text/csv"),
                MARKDOWN("text/markdown");

                override fun getProducedMimeType(): MimeType = MimeType.valueOf(mimeType)
            }
            """.trimIndent()
        )
        addEndpoint(
            """
            @ReadOperation(producesFrom = ReportFormat::class)
            fun report(format: ReportFormat): String = format.name
            """
        )

        assertEquals(listOf("text/csv", "text/markdown"), operation("ReportEndpoint", "report").produces)
    }

    fun testDeclaredProducesPrecedesProducesFromTypes() {
        myFixture.addFileToProject(
            "ReportFormat.kt",
            """
            import org.springframework.boot.actuate.endpoint.Producible
            import org.springframework.util.MimeType

            enum class ReportFormat(private val mimeType: String) : Producible<ReportFormat> {
                CSV("text/csv");

                override fun getProducedMimeType(): MimeType = MimeType.valueOf(mimeType)
            }
            """.trimIndent()
        )
        addEndpoint(
            """
            @ReadOperation(produces = ["text/plain"], producesFrom = ReportFormat::class)
            fun report(format: ReportFormat): String = format.name
            """
        )

        assertEquals(listOf("text/plain", "text/csv"), operation("ReportEndpoint", "report").produces)
    }

    fun testUnreadableProducesFromConstantIsExcluded() {
        myFixture.addFileToProject(
            "ReportFormat.kt",
            """
            import org.springframework.boot.actuate.endpoint.Producible
            import org.springframework.util.MimeType

            enum class ReportFormat(private val mimeType: String) : Producible<ReportFormat> {
                CSV("text/csv"),
                UNKNOWN(unreadableMimeType());

                override fun getProducedMimeType(): MimeType = MimeType.valueOf(mimeType)
            }

            fun unreadableMimeType(): String = "text/x-unreadable"
            """.trimIndent()
        )
        addEndpoint(
            """
            @ReadOperation(producesFrom = ReportFormat::class)
            fun report(format: ReportFormat): String = format.name
            """
        )

        assertEquals(listOf("text/csv"), operation("ReportEndpoint", "report").produces)
    }

    private fun addEndpoint(operations: String) {
        myFixture.addFileToProject(
            "ReportEndpoint.kt",
            """
            |import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            |import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            |import org.springframework.boot.actuate.endpoint.annotation.WriteOperation
            |
            |data class Report(val status: String)
            |
            |@Endpoint(id = "report")
            |class ReportEndpoint {
            |${operations.trimIndent().prependIndent("    ")}
            |}
            """.trimMargin()
        )
    }

    private fun addJavaEndpoint(members: String) {
        myFixture.addFileToProject(
            "JavaReportEndpoint.java",
            """
            |import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
            |import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            |
            |@Endpoint(id = "java-report")
            |public class JavaReportEndpoint {
            |${members.trimIndent().prependIndent("    ")}
            |}
            """.trimMargin()
        )
    }

    private fun operation(className: String, methodName: String): EndpointElement {
        val operations = actuatorEndpoints().filter {
            it.containingClass?.name == className && (it.psiElement as? PsiMethod)?.name == methodName
        }
        assertEquals("precondition: one $className.$methodName operation is listed", 1, operations.size)
        return operations.single()
    }

    private fun actuatorEndpoints(): List<EndpointElement> =
        SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .toList()
}

private const val OCTET_STREAM = "application/octet-stream"

private val DEFAULT_PRODUCES = listOf(
    "application/vnd.spring-boot.actuator.v3+json",
    "application/vnd.spring-boot.actuator.v2+json",
    "application/json",
)
