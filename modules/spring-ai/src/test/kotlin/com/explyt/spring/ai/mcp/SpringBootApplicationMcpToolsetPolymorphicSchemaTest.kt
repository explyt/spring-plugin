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
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.runBlocking

class SpringBootApplicationMcpToolsetPolymorphicSchemaTest : ExplytJavaLightTestCase() {

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
            JavaPsiFacade.getInstance(project).findClass(JSON_TYPE_INFO, module.moduleWithLibrariesScope)
        )
    }

    fun testKotlinSealedInterfaceWithTypeInfoListsVariantsUnderTheirDefaultIds() = runBlocking<Unit> {
        addKotlinController(
            "Payment", "Payment", "Card(\"4242\")", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            sealed interface Payment

            data class Card(val number: String) : Payment

            data class Cash(val amount: Long) : Payment
            """
        )
        assertAnnotated("$PACKAGE.Payment", JSON_TYPE_INFO)

        val schema = responseSchema("/api/payment")

        assertEquals("$PACKAGE.Payment", schema["className"].asText())
        assertPolymorphic(schema)
        assertEquals(json("""{"use":"NAME","include":"PROPERTY","property":"@type"}"""), schema["discriminator"])
        assertEquals(
            mapOf(
                "$PACKAGE.Card" to VariantView("Card", listOf("number")),
                "$PACKAGE.Cash" to VariantView("Cash", listOf("amount")),
            ),
            variantsOf(schema)
        )
    }

    fun testJavaAbstractClassWithSubTypesUsesTheDeclaredNamesAndProperty() = runBlocking<Unit> {
        addJavaType("Card", "public class Card extends Payment { public String number; }")
        addJavaType("Cash", "public class Cash extends Payment { public long amount; }")
        addJavaController(
            "Payment", "new Card()", """
            import com.fasterxml.jackson.annotation.JsonSubTypes;
            import com.fasterxml.jackson.annotation.JsonTypeInfo;

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
            @JsonSubTypes({
                @JsonSubTypes.Type(value = Card.class, name = "card"),
                @JsonSubTypes.Type(value = Cash.class, name = "cash")
            })
            public abstract class Payment {}
            """
        )
        assertAnnotated("$PACKAGE.Payment", JSON_TYPE_INFO)
        assertAnnotated("$PACKAGE.Payment", JSON_SUB_TYPES)

        val schema = responseSchema("/api/payment")

        assertEquals("$PACKAGE.Payment", schema["className"].asText())
        assertPolymorphic(schema)
        assertEquals(json("""{"use":"NAME","include":"PROPERTY","property":"kind"}"""), schema["discriminator"])
        assertEquals(
            mapOf(
                "$PACKAGE.Card" to VariantView("card", listOf("number")),
                "$PACKAGE.Cash" to VariantView("cash", listOf("amount")),
            ),
            variantsOf(schema)
        )
    }

    fun testKotlinAbstractClassWithSubTypesUsesTheDeclaredNamesAndProperty() = runBlocking<Unit> {
        addKotlinController(
            "Payment", "Payment", "Card(\"4242\")", """
            import com.fasterxml.jackson.annotation.JsonSubTypes
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
            @JsonSubTypes(
                JsonSubTypes.Type(value = Card::class, name = "card"),
                JsonSubTypes.Type(value = Cash::class, name = "cash"),
            )
            abstract class Payment

            class Card(val number: String) : Payment()

            class Cash(val amount: Long) : Payment()
            """
        )
        assertAnnotated("$PACKAGE.Payment", JSON_SUB_TYPES)

        val schema = responseSchema("/api/payment")

        assertPolymorphic(schema)
        assertEquals(json("""{"use":"NAME","include":"PROPERTY","property":"kind"}"""), schema["discriminator"])
        assertEquals(
            mapOf(
                "$PACKAGE.Card" to VariantView("card", listOf("number")),
                "$PACKAGE.Cash" to VariantView("cash", listOf("amount")),
            ),
            variantsOf(schema)
        )
    }

    fun testJavaAbstractClassWithoutSubTypesIsPolymorphicWithoutVariants() = runBlocking<Unit> {
        addJavaType("Card", "public class Card extends Payment { public String number; }")
        addJavaController("Payment", "new Card()", "public abstract class Payment {}")
        assertNotNull("Precondition: the subclass exists in the project", findProjectClass("$PACKAGE.Card"))

        val schema = responseSchema("/api/payment")

        assertEquals("$PACKAGE.Payment", schema["className"].asText())
        assertPolymorphic(schema)
        assertNull("Subtypes Jackson does not know are not enumerated, got $schema", schema["variants"])
        assertNull("No @JsonTypeInfo, so no discriminator, got $schema", schema["discriminator"])
    }

    fun testKotlinInterfaceWithoutSubTypesIsPolymorphicWithoutVariants() = runBlocking<Unit> {
        addKotlinController(
            "Payment", "Payment", "Card(\"4242\")", """
            interface Payment

            data class Card(val number: String) : Payment
            """
        )

        val schema = responseSchema("/api/payment")

        assertEquals("$PACKAGE.Payment", schema["className"].asText())
        assertPolymorphic(schema)
        assertNull("Subtypes Jackson does not know are not enumerated, got $schema", schema["variants"])
        assertNull("No @JsonTypeInfo, so no discriminator, got $schema", schema["discriminator"])
    }

    fun testKotlinSealedWithoutTypeInfoListsVariantsWithoutIds() = runBlocking<Unit> {
        addKotlinController(
            "Payment", "Payment", "Card(\"4242\")", """
            sealed interface Payment

            data class Card(val number: String) : Payment

            data class Cash(val amount: Long) : Payment
            """
        )

        val schema = responseSchema("/api/payment")

        assertPolymorphic(schema)
        assertNull("Written by its runtime class, with no type id property, got $schema", schema["discriminator"])
        assertEquals(
            mapOf(
                "$PACKAGE.Card" to VariantView(null, listOf("number")),
                "$PACKAGE.Cash" to VariantView(null, listOf("amount")),
            ),
            variantsOf(schema)
        )
        schema["variants"].forEach { variant ->
            assertEquals(setOf("className", "fields"), variant.fieldNames().asSequence().toSet())
        }
    }

    fun testListElementCarriesThePolymorphicDescription() = runBlocking<Unit> {
        addKotlinController(
            "Payment", "List<Payment>", "listOf(Card(\"4242\"))", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            sealed interface Payment

            data class Card(val number: String) : Payment

            data class Cash(val amount: Long) : Payment
            """
        )

        val schema = responseSchema("/api/payment")

        assertEquals("The list is described by its element", "$PACKAGE.Payment", schema["className"].asText())
        assertPolymorphic(schema)
        assertEquals(json("""{"use":"NAME","include":"PROPERTY","property":"@type"}"""), schema["discriminator"])
        assertEquals(setOf("$PACKAGE.Card", "$PACKAGE.Cash"), variantsOf(schema).keys)
    }

    fun testVariantReferencingTheRootStopsAtTheDepthBudget() = runBlocking<Unit> {
        addKotlinController(
            "Expr", "Expr", "Neg(Lit(1))", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            sealed interface Expr

            data class Lit(val value: Int) : Expr

            data class Neg(val operand: Expr) : Expr
            """
        )

        val schema = responseSchema("/api/expr")

        assertPolymorphic(schema)
        assertEquals(setOf("$PACKAGE.Lit", "$PACKAGE.Neg"), variantsOf(schema).keys)
        val levels = schemaLevels(schema)
        assertTrue("The self-reference is cut within the depth budget of 3, got $levels levels: $schema", levels in 2..3)
    }

    fun testPlainDtoIsUnchanged() = runBlocking<Unit> {
        addJavaController("Item", "new Item()", "public class Item { public long id; }")

        assertEquals(
            json("""{"className":"$PACKAGE.Item","fields":[{"name":"id","type":"long","nullable":false,"nested":null}]}"""),
            responseSchema("/api/item")
        )
    }

    fun testAbstractLibraryTypeIsStillMarkedOmitted() = runBlocking<Unit> {
        addJavaType(
            "LibraryController", """
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;
            import org.springframework.web.context.request.WebRequest;

            @RestController
            public class LibraryController {
                @GetMapping("/api/request")
                public WebRequest get(WebRequest request) { return request; }
            }
            """
        )

        assertEquals(
            json("""{"className":"org.springframework.web.context.request.WebRequest","schemaOmitted":"ABSTRACT_TYPE"}"""),
            responseSchema("/api/request")
        )
    }

    private data class VariantView(val typeId: String?, val fields: List<String>)

    private fun variantsOf(schema: JsonNode): Map<String, VariantView> {
        val variants = schema["variants"]
        assertNotNull("Expected 'variants' in $schema", variants)
        return variants.associate { variant ->
            variant["className"].asText() to VariantView(
                variant["typeId"]?.asText(),
                variant["fields"].map { it["name"].asText() },
            )
        }
    }

    private fun assertPolymorphic(schema: JsonNode) {
        assertTrue("Expected 'polymorphic: true' in $schema", schema["polymorphic"]?.asBoolean() == true)
    }

    private fun schemaLevels(schema: JsonNode?): Int {
        if (schema == null || schema.isNull) return 0
        val variantLevels = schema["variants"].orEmpty().maxOfOrNull(::schemaLevels) ?: 0
        val nestedLevels = schema["fields"].orEmpty().maxOfOrNull { schemaLevels(it["nested"]) } ?: 0
        return maxOf(1, variantLevels, nestedLevels + 1)
    }

    private fun JsonNode?.orEmpty(): List<JsonNode> = this?.toList().orEmpty()

    private fun assertAnnotated(className: String, annotation: String) {
        val psiClass = findProjectClass(className)
        assertNotNull("Precondition: $className resolves", psiClass)
        assertTrue("Precondition: @$annotation on $className resolves to the library class", psiClass!!.hasAnnotation(annotation))
    }

    private fun findProjectClass(className: String) =
        JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.projectScope(project))

    private fun addJavaType(typeName: String, declaration: String) {
        myFixture.addFileToProject("com/example/schema/$typeName.java", "package $PACKAGE;\n\n" + declaration.trimIndent())
    }

    private fun addJavaController(typeName: String, instance: String, declaration: String) {
        addJavaType(typeName, declaration)
        addJavaType(
            "${typeName}Controller", """
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class ${typeName}Controller {
                @GetMapping("/api/${typeName.lowercase()}")
                public $typeName get() { return $instance; }
            }
            """
        )
    }

    private fun addKotlinController(name: String, returnType: String, instance: String, declarations: String) {
        val imports = declarations.trimIndent().lines().filter { it.startsWith("import ") }
        val body = declarations.trimIndent().lines().filterNot { it.startsWith("import ") }
        myFixture.addFileToProject(
            "com/example/schema/${name}Controller.kt",
            (listOf("package $PACKAGE", "") + imports + listOf(
                "import org.springframework.web.bind.annotation.GetMapping",
                "import org.springframework.web.bind.annotation.RestController",
            ) + body + listOf(
                "",
                "@RestController",
                "class ${name}Controller {",
                "    @GetMapping(\"/api/${name.lowercase()}\")",
                "    fun get(): $returnType = $instance",
                "}",
            )).joinToString("\n")
        )
    }

    private suspend fun responseSchema(url: String): JsonNode {
        val endpoints = mapper.readTree(
            toolset.getEndpointContract(urlPattern = url, projectPath = project.basePath, httpMethod = "GET")
        )["endpoints"]
        assertEquals("Exactly one contract for $url", 1, endpoints.size())
        return endpoints[0]["responseSchema"]
    }

    private fun json(text: String): JsonNode = mapper.readTree(text)

    private companion object {
        const val PACKAGE = "com.example.schema"
        const val JSON_TYPE_INFO = "com.fasterxml.jackson.annotation.JsonTypeInfo"
        const val JSON_SUB_TYPES = "com.fasterxml.jackson.annotation.JsonSubTypes"
    }
}
