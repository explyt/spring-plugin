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
        TestLibrary("com.fasterxml.jackson.core:jackson-annotations:2.21", false),
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

        assertAnnotated("$PACKAGE.Expr", JSON_TYPE_INFO)
        assertNotNull("Precondition: Lit resolves", findProjectClass("$PACKAGE.Lit"))
        assertNotNull("Precondition: Neg resolves", findProjectClass("$PACKAGE.Neg"))
        val schema = responseSchema("/api/expr")

        assertPolymorphic(schema)
        assertEquals(setOf("$PACKAGE.Lit", "$PACKAGE.Neg"), variantsOf(schema).keys)
        assertEquals("Exactly three expanded schema levels", 3, schemaLevels(schema))
        val neg = schema["variants"].single { it["className"].asText() == "$PACKAGE.Neg" }
        val operand = neg["fields"].single { it["name"].asText() == "operand" }["nested"]
        assertDepthLimitedVariants(operand, mapOf("$PACKAGE.Lit" to "Lit", "$PACKAGE.Neg" to "Neg"))
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

    fun testSimpleNameUsesTheNestedClassSimpleName() = runBlocking<Unit> {
        addKotlinController(
            "NestedPayment", "NestedPayment", "Outer.Card()", """
            import com.fasterxml.jackson.annotation.JsonSubTypes
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
            @JsonSubTypes(JsonSubTypes.Type(value = Outer.Card::class))
            abstract class NestedPayment

            class Outer {
                class Card : NestedPayment()
            }
            """
        )

        assertAnnotated("$PACKAGE.NestedPayment", JSON_TYPE_INFO)
        assertAnnotated("$PACKAGE.NestedPayment", JSON_SUB_TYPES)
        val id = JavaPsiFacade.getInstance(project).findClass("$JSON_TYPE_INFO.Id", module.moduleWithLibrariesScope)
        assertNotNull("Precondition: SIMPLE_NAME resolves in jackson-annotations", id?.findFieldByName("SIMPLE_NAME", false))
        val schema = responseSchema("/api/nestedpayment")
        assertEquals("@type", schema["discriminator"]["property"].asText())
        assertEquals("Card", variantsOf(schema).values.single().typeId)
    }

    fun testClassUsesTheBinaryFqnAndDefaultClassProperty() = runBlocking<Unit> {
        addKotlinController(
            "ClassPayment", "ClassPayment", "Outer.Card()", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
            sealed interface ClassPayment

            class Outer {
                class Card : ClassPayment
            }
            """
        )

        val schema = responseSchema("/api/classpayment")
        assertEquals("@class", schema["discriminator"]["property"].asText())
        assertEquals("$PACKAGE.Outer.Card", schema["variants"].single()["className"].asText())
        assertEquals("$PACKAGE.Outer\$Card", schema["variants"].single()["typeId"].asText())
    }

    fun testMinimalClassUsesALeadingDotRelativeToTheBasePackage() = runBlocking<Unit> {
        addKotlinController(
            "MinimalPayment", "MinimalPayment", "Card()", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
            sealed interface MinimalPayment

            class Card : MinimalPayment
            """
        )

        val schema = responseSchema("/api/minimalpayment")
        assertEquals("@c", schema["discriminator"]["property"].asText())
        assertEquals(".Card", variantsOf(schema).values.single().typeId)
    }

    fun testDeductionHasNoPropertyOrVariantTypeIds() = runBlocking<Unit> {
        addKotlinController(
            "DeductionPayment", "DeductionPayment", "Card()", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
            sealed interface DeductionPayment

            class Card : DeductionPayment
            """
        )

        val schema = responseSchema("/api/deductionpayment")
        assertEquals(setOf("use", "include"), schema["discriminator"].fieldNames().asSequence().toSet())
        assertEquals("DEDUCTION", schema["discriminator"]["use"].asText())
        assertEquals("PROPERTY", schema["discriminator"]["include"].asText())
        assertNull(variantsOf(schema).values.single().typeId)
    }

    fun testJsonTypeNameIsUsedWhenSubtypeNameIsAbsentAndExplicitNameWins() = runBlocking<Unit> {
        addJavaController("NamedPayment", "new Visa()", """
            import com.fasterxml.jackson.annotation.JsonSubTypes;
            import com.fasterxml.jackson.annotation.JsonTypeInfo;
            import com.fasterxml.jackson.annotation.JsonTypeName;

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            @JsonSubTypes({
                @JsonSubTypes.Type(value = Visa.class),
                @JsonSubTypes.Type(value = Cash.class, name = "cash")
            })
            public abstract class NamedPayment {}

            @JsonTypeName("visa")
            class Visa extends NamedPayment {}

            @JsonTypeName("ignored")
            class Cash extends NamedPayment {}
            """)

        val variants = variantsOf(responseSchema("/api/namedpayment"))
        assertEquals("visa", variants.getValue("$PACKAGE.Visa").typeId)
        assertEquals("cash", variants.getValue("$PACKAGE.Cash").typeId)
    }

    fun testTypeInfoDeclaredOnAnImplementedInterfaceIsInherited() = runBlocking<Unit> {
        addKotlinController(
            "InheritedPayment", "InheritedPayment", "Card()", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            interface PaymentContract

            abstract class InheritedPayment : PaymentContract

            class Card : InheritedPayment()
            """
        )

        val schema = responseSchema("/api/inheritedpayment")
        assertEquals("@type", schema["discriminator"]["property"].asText())
        assertNull("A non-sealed abstract type without registered subtypes is not enumerated", schema["variants"])
    }

    fun testExistingPropertyIsReported() = runBlocking<Unit> {
        addKotlinController(
            "ExistingPayment", "ExistingPayment", "Card()", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
            sealed interface ExistingPayment

            class Card : ExistingPayment
            """
        )
        val existing = responseSchema("/api/existingpayment")
        assertEquals("EXISTING_PROPERTY", existing["discriminator"]["include"].asText())
        assertEquals("kind", existing["discriminator"]["property"].asText())
    }

    fun testKotlinSealedObjectIsAKnownVariant() = runBlocking<Unit> {
        addKotlinController(
            "ObjectPayment", "ObjectPayment", "Cash", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            sealed interface ObjectPayment

            object Cash : ObjectPayment
            """
        )

        val schema = responseSchema("/api/objectpayment")
        assertEquals("Cash", variantsOf(schema).values.single().typeId)
    }

    fun testWrapperObjectHasNoDiscriminatorProperty() = runBlocking<Unit> {
        addKotlinController(
            "WrapperPayment", "WrapperPayment", "Card()", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
            sealed interface WrapperPayment

            class Card : WrapperPayment
            """
        )
        assertAnnotated("$PACKAGE.WrapperPayment", JSON_TYPE_INFO)
        assertNotNull("Precondition: Card resolves", findProjectClass("$PACKAGE.Card"))
        val wrapper = responseSchema("/api/wrapperpayment")
        assertEquals("WRAPPER_OBJECT", wrapper["discriminator"]["include"].asText())
        assertNull(wrapper["discriminator"]["property"])
    }

    fun testJavaSingleSubtypeAnnotationWithoutBraces() = runBlocking<Unit> {
        addJavaType("Card", "public class Card extends SinglePayment { public long amount; }")
        addJavaController("SinglePayment", "new Card()", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo;
            import com.fasterxml.jackson.annotation.JsonSubTypes;
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            @JsonSubTypes(@JsonSubTypes.Type(value = Card.class, name = "card"))
            public abstract class SinglePayment {}
            """)
        assertAnnotated("$PACKAGE.SinglePayment", JSON_TYPE_INFO)
        assertAnnotated("$PACKAGE.SinglePayment", JSON_SUB_TYPES)
        assertEquals(mapOf("$PACKAGE.Card" to VariantView("card", listOf("amount"))), variantsOf(responseSchema("/api/singlepayment")))
    }

    fun testWrapperArrayHasNoDiscriminatorProperty() = runBlocking<Unit> {
        addKotlinController(
            "ArrayPayment", "ArrayPayment", "Card()", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
            sealed interface ArrayPayment

            class Card : ArrayPayment
            """
        )
        assertAnnotated("$PACKAGE.ArrayPayment", JSON_TYPE_INFO)
        assertNotNull("Precondition: Card resolves", findProjectClass("$PACKAGE.Card"))
        val schema = responseSchema("/api/arraypayment")
        assertEquals("WRAPPER_ARRAY", schema["discriminator"]["include"].asText())
        assertNull("A wrapper array has no type-id property", schema["discriminator"]["property"])
    }

    fun testPaymentAtTheDepthBudgetRetainsVariantStubs() = runBlocking<Unit> {
        addKotlinController(
            "Order", "Order", "Order(Shipping(Card(\"4242\")))", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            data class Order(val shipping: Shipping)
            data class Shipping(val payment: Payment)

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            sealed interface Payment

            data class Card(val number: String) : Payment
            data class Cash(val amount: Long) : Payment
            """
        )
        assertAnnotated("$PACKAGE.Payment", JSON_TYPE_INFO)
        listOf("Order", "Shipping", "Card", "Cash").forEach {
            assertNotNull("Precondition: $it resolves", findProjectClass("$PACKAGE.$it"))
        }
        val schema = responseSchema("/api/order")
        val shipping = schema["fields"].single { it["name"].asText() == "shipping" }["nested"]
        val payment = shipping["fields"].single { it["name"].asText() == "payment" }["nested"]
        assertEquals("Exactly three expanded schema levels", 3, schemaLevels(schema))
        assertDepthLimitedVariants(payment, mapOf("$PACKAGE.Card" to "Card", "$PACKAGE.Cash" to "Cash"))
    }

    fun testNoneHasNoDiscriminatorOrVariantIds() = runBlocking<Unit> {
        addKotlinController(
            "NonePayment", "NonePayment", "Card()", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
            sealed interface NonePayment

            class Card : NonePayment
            class Cash : NonePayment
            """
        )
        assertAnnotated("$PACKAGE.NonePayment", JSON_TYPE_INFO)
        assertNotNull("Precondition: Card resolves", findProjectClass("$PACKAGE.Card"))
        assertNotNull("Precondition: Cash resolves", findProjectClass("$PACKAGE.Cash"))
        val schema = responseSchema("/api/nonepayment")
        assertEquals(setOf("$PACKAGE.Card", "$PACKAGE.Cash"), variantsOf(schema).keys)
        assertTrue("NONE variants have no typeId", schema["variants"].all { !it.has("typeId") })
        assertNull("NONE disables type metadata", schema["discriminator"])
    }

    fun testSubtypeNoneCancelsInheritedNameTypeInfo() = runBlocking<Unit> {
        addKotlinController(
            "ChildPayment", "ChildPayment", "Card()", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            interface ParentPayment

            @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
            sealed interface ChildPayment : ParentPayment

            class Card : ChildPayment
            """
        )
        assertAnnotated("$PACKAGE.ParentPayment", JSON_TYPE_INFO)
        assertAnnotated("$PACKAGE.ChildPayment", JSON_TYPE_INFO)
        assertNotNull("Precondition: Card resolves", findProjectClass("$PACKAGE.Card"))
        val schema = responseSchema("/api/childpayment")
        assertEquals(setOf("$PACKAGE.Card"), variantsOf(schema).keys)
        assertTrue("NONE variants have no typeId", schema["variants"].all { !it.has("typeId") })
        assertNull("Subtype NONE overrides inherited NAME", schema["discriminator"])
    }

    fun testJavaInterfaceTypeInfoPrecedesSuperclassTypeInfo() = runBlocking<Unit> {
        addJavaType("BasePayment", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo;
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "base")
            public abstract class BasePayment {}
            """)
        addJavaType("Tagged", """
            import com.fasterxml.jackson.annotation.JsonTypeInfo;
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "iface")
            public interface Tagged {}
            """)
        addJavaType("Card", "public class Card extends Payment { public long amount; }")
        addJavaController("Payment", "new Card()", """
            import com.fasterxml.jackson.annotation.JsonSubTypes;
            @JsonSubTypes({@JsonSubTypes.Type(value = Card.class, name = "card")})
            public abstract class Payment extends BasePayment implements Tagged {}
            """)
        assertAnnotated("$PACKAGE.BasePayment", JSON_TYPE_INFO)
        assertAnnotated("$PACKAGE.Tagged", JSON_TYPE_INFO)
        assertAnnotated("$PACKAGE.Payment", JSON_SUB_TYPES)
        assertNotNull("Precondition: Card resolves", findProjectClass("$PACKAGE.Card"))
        val schema = responseSchema("/api/payment")
        assertEquals(mapOf("$PACKAGE.Card" to VariantView("card", listOf("amount"))), variantsOf(schema))
        assertEquals("Jackson visits implemented interfaces before the superclass", "iface", schema["discriminator"]["property"].asText())
    }

    private fun assertDepthLimitedVariants(schema: JsonNode, expected: Map<String, String>) {
        assertNotNull("A depth-limited polymorphic type keeps its known variants: $schema", schema["variants"])
        val variants = schema["variants"].toList()
        assertEquals(expected.size, variants.size)
        assertEquals(expected, variants.associate { it["className"].asText() to it["typeId"].asText() })
        variants.forEach {
            assertEquals("DEPTH_LIMIT", it["schemaOmitted"]?.asText())
            assertFalse("An omitted variant has no fields key: $it", it.has("fields"))
        }
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
        if (schema == null || schema.isNull || schema["schemaOmitted"]?.asText() == "DEPTH_LIMIT") return 0
        val variantLevels = schema["variants"].orEmpty().maxOfOrNull(::schemaLevels) ?: 0
        val nestedLevels = schema["fields"].orEmpty().maxOfOrNull { schemaLevels(it["nested"]) } ?: 0
        return 1 + maxOf(variantLevels, nestedLevels)
    }

    private fun JsonNode?.orEmpty(): List<JsonNode> = this?.toList().orEmpty()

    private fun assertAnnotated(className: String, annotation: String) {
        val psiClass = findProjectClass(className)
        assertNotNull("Precondition: $className resolves", psiClass)
        val resolved = psiClass!!.getAnnotation(annotation)?.resolveAnnotationType()
        assertNotNull("Precondition: @$annotation on $className resolves to the library class", resolved)
        assertFalse("Precondition: the annotation is not a project stub", GlobalSearchScope.projectScope(project).contains(resolved!!.containingFile.virtualFile))
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
        val returnType = endpoints[0]["returnType"].asText()
        val fqn = endpoints[0]["responseSchema"]["className"].asText()
        assertTrue("Precondition: handler declares the schema response type: $returnType", returnType.contains(fqn))
        assertNotNull("Precondition: handler response type $fqn resolves", JavaPsiFacade.getInstance(project).findClass(fqn, module.moduleWithLibrariesScope))
        return endpoints[0]["responseSchema"]
    }

    private fun json(text: String): JsonNode = mapper.readTree(text)

    private companion object {
        const val PACKAGE = "com.example.schema"
        const val JSON_TYPE_INFO = "com.fasterxml.jackson.annotation.JsonTypeInfo"
        const val JSON_SUB_TYPES = "com.fasterxml.jackson.annotation.JsonSubTypes"
    }
}
