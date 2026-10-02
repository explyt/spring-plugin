/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.psi.JavaPsiFacade
import kotlinx.coroutines.runBlocking

/**
 * The response schema `explyt_get_spring_endpoint_contract` reports, against what Jackson writes to the wire.
 *
 * A client generated from the schema relies on it: an enum written as a string must not be described as an object of
 * `java.lang.Enum`'s own fields, a member Jackson leaves out must not be listed, and a renamed member must appear under
 * the name a client reads.
 */
class SpringBootApplicationMcpToolsetResponseSchemaTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.jacksonAnnotations_2_15_2,
        TestLibrary.kotlin_1_9_22,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        assertNotNull(
            "Precondition: jackson-annotations is on the fixture classpath",
            JavaPsiFacade.getInstance(project)
                .findClass("com.fasterxml.jackson.annotation.JsonValue", module.moduleWithLibrariesScope)
        )
    }

    fun testEnumWithoutAnnotationsIsItsConstantNames() = runBlocking<Unit> {
        addJavaController("Status", "Status.PAID", """
            public enum Status { PAID, SHIPPED }
        """)

        val schema = responseSchema("/api/schema/status")

        assertEquals(setOf("className", "enumValues"), schema.fieldNames().asSequence().toSet())
        assertEquals(listOf("PAID", "SHIPPED"), schema["enumValues"].map { it.asText() })
    }

    fun testEnumConstantRenamedWithJsonProperty() = runBlocking<Unit> {
        addJavaController("Status", "Status.PAID", """
            import com.fasterxml.jackson.annotation.JsonProperty;

            public enum Status {
                @JsonProperty("paid") PAID,
                SHIPPED
            }
        """)

        assertEquals(listOf("paid", "SHIPPED"), responseSchema("/api/schema/status")["enumValues"].map { it.asText() })
    }

    fun testJavaEnumWithJsonValueMethodNamesThatMember() = runBlocking<Unit> {
        addJavaController("Status", "Status.PAID", """
            import com.fasterxml.jackson.annotation.JsonValue;

            public enum Status {
                PAID, SHIPPED;

                @JsonValue
                public String wireName() { return name().toLowerCase(); }
            }
        """)

        val schema = responseSchema("/api/schema/status")

        assertEquals(setOf("className", "jsonValue", "valueType"), schema.fieldNames().asSequence().toSet())
        assertEquals("wireName", schema["jsonValue"].asText())
        assertEquals("java.lang.String", schema["valueType"].asText())
    }

    fun testJavaEnumWithJsonValueFieldNamesThatField() = runBlocking<Unit> {
        addJavaController("Status", "Status.PAID", """
            import com.fasterxml.jackson.annotation.JsonValue;

            public enum Status {
                PAID(1), SHIPPED(2);

                @JsonValue
                private final int code;

                Status(int code) { this.code = code; }
            }
        """)

        val schema = responseSchema("/api/schema/status")

        assertEquals(setOf("className", "jsonValue", "valueType"), schema.fieldNames().asSequence().toSet())
        assertEquals("code", schema["jsonValue"].asText())
        assertEquals("int", schema["valueType"].asText())
    }

    fun testKotlinEnumWithJsonValuePropertyNamesThatMember() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/schema/KotlinStatusController.kt", """
            package com.example.schema

            import com.fasterxml.jackson.annotation.JsonValue
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            enum class Basis {
                RENDERED, HEURISTIC;

                @get:JsonValue
                val wireName: String = name.lowercase()
            }

            data class Activity(val basis: Basis)

            @RestController
            class KotlinStatusController {
                @GetMapping("/api/schema/activity")
                fun activity(): Activity = Activity(Basis.RENDERED)
            }
            """.trimIndent()
        )

        val basis = field(responseSchema("/api/schema/activity"), "basis")["nested"]

        assertEquals(setOf("className", "jsonValue", "valueType"), basis.fieldNames().asSequence().toSet())
        assertEquals("wireName", basis["jsonValue"].asText())
        assertEquals("java.lang.String", basis["valueType"].asText())
        assertTrue("No JDK internals of java.lang.Enum, got $basis", basis["fields"] == null)
    }

    fun testTransientAndIgnoredMembersAreNotWritten() = runBlocking<Unit> {
        addJavaController("Order", "new Order()", """
            import com.fasterxml.jackson.annotation.JsonIgnore;

            public class Order {
                public long id;
                public transient String cache;
                @JsonIgnore public String secret;
            }
        """)

        assertEquals(listOf("id"), responseSchema("/api/schema/order")["fields"].map { it["name"].asText() })
    }

    /** A field a `java.*` superclass declares is the JDK's own state, not part of the DTO a client receives. */
    fun testFieldsOfAJdkSuperclassAreNotWritten() = runBlocking<Unit> {
        addJavaController("Batch", "new Batch()", """
            public class Batch extends java.util.Observable {
                public long id;
            }
        """)

        assertEquals(listOf("id"), responseSchema("/api/schema/batch")["fields"].map { it["name"].asText() })
    }

    fun testJsonPropertyRenamesAKotlinProperty() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/schema/RenamedController.kt", """
            package com.example.schema

            import com.fasterxml.jackson.annotation.JsonProperty
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            data class Receipt(
                @JsonProperty("order_id") val orderId: Long,
                @get:JsonProperty("store_id") val storeId: Long,
                @field:JsonProperty("total_cents") val totalCents: Long,
                val currency: String,
            )

            @RestController
            class RenamedController {
                @GetMapping("/api/schema/receipt")
                fun receipt(): Receipt = Receipt(1, 2, 3, "EUR")
            }
            """.trimIndent()
        )

        val fields = responseSchema("/api/schema/receipt")["fields"].associateBy { it["name"].asText() }

        assertEquals(setOf("order_id", "store_id", "total_cents", "currency"), fields.keys)
        assertEquals("orderId", fields.getValue("order_id")["declaredName"].asText())
        assertEquals("storeId", fields.getValue("store_id")["declaredName"].asText())
        assertEquals("totalCents", fields.getValue("total_cents")["declaredName"].asText())
        assertEquals(
            "A field written under its own name keeps the original shape",
            setOf("name", "type", "nullable", "nested"),
            fields.getValue("currency").fieldNames().asSequence().toSet()
        )
    }

    fun testPlainDtoKeepsItsShape() = runBlocking<Unit> {
        addJavaController("Item", "new Item()", """
            public class Item {
                public long id;
                public String name;
            }
        """)

        val schema = responseSchema("/api/schema/item")

        assertEquals(setOf("className", "fields"), schema.fieldNames().asSequence().toSet())
        assertEquals(
            listOf(setOf("name", "type", "nullable", "nested"), setOf("name", "type", "nullable", "nested")),
            schema["fields"].map { it.fieldNames().asSequence().toSet() }
        )
    }

    private fun addJavaController(typeName: String, instance: String, declaration: String) {
        myFixture.addFileToProject("com/example/schema/$typeName.java", "package com.example.schema;\n\n" + declaration.trimIndent())
        val path = "/api/schema/" + typeName.lowercase()
        myFixture.addFileToProject(
            "com/example/schema/${typeName}Controller.java", """
            package com.example.schema;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class ${typeName}Controller {
                @GetMapping("$path")
                public $typeName get() { return $instance; }
            }
            """.trimIndent()
        )
    }

    private suspend fun responseSchema(url: String): JsonNode {
        val endpoints = mapper.readTree(
            toolset.getEndpointContract(urlPattern = url, projectPath = project.basePath, httpMethod = "GET")
        )["endpoints"]
        assertEquals("Exactly one contract for $url", 1, endpoints.size())
        return endpoints[0]["responseSchema"]
    }

    private fun field(schema: JsonNode, name: String): JsonNode =
        schema["fields"].single { it["name"].asText() == name }
}
