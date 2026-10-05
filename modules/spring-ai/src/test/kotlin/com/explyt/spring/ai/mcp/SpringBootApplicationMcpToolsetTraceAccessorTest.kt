/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.addFromMaven
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * A call to a trivial accessor of a project class is listed with `accessor: true` and neither followed nor counted
 * against the method limit of `explyt_trace_spring_call_chain`.
 *
 * On a Java project a service reading a DTO field by field used up the whole limit on getters, and the repository
 * call that the trace was made for was left as `node: null` behind `chainLimitReached`. Uses the heavy
 * [JavaCodeInsightFixtureTestCase] because `traceCallChain` resolves its file through `LocalFileSystem`.
 */
class SpringBootApplicationMcpToolsetTraceAccessorTest : JavaCodeInsightFixtureTestCase() {

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun tuneFixture(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        moduleBuilder.addJdkVersion(LanguageLevel.JDK_21)
    }

    override fun setUp() {
        super.setUp()
        IdeaTestUtil.setModuleLanguageLevel(myFixture.module, LanguageLevel.JDK_21, testRootDisposable)
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            LIBRARIES.forEach { addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies) }
        }
        generateMethodsOfGeneratedDto()
        addSource("lombok/Getter.java", LOMBOK_GETTER_SOURCE)
        addSource("lombok/experimental/Delegate.java", LOMBOK_DELEGATE_SOURCE)
        addSource("com/example/orders/OrderDto.java", ORDER_DTO_SOURCE)
        addSource("com/example/orders/CustomerDto.kt", CUSTOMER_DTO_SOURCE)
        addSource("com/example/orders/Priced.java", PRICED_SOURCE)
        addSource("com/example/orders/GeneratedDto.java", GENERATED_DTO_SOURCE)
        addSource("com/example/orders/LineDto.java", LINE_DTO_SOURCE)
        addSource("com/example/orders/OrderStore.java", ORDER_STORE_SOURCE)
        addSource("com/example/orders/OrderService.java", ORDER_SERVICE_SOURCE)
        addSource("com/example/orders/OrderController.java", ORDER_CONTROLLER_SOURCE)
    }

    fun testTrivialGettersDoNotSpendTheMethodLimit() = runBlocking {
        val trace = traceFromHandler()

        assertFalse("Sixty trivial getters must not reach the cap", trace.root["chainLimitReached"].asBoolean())
        assertFalse("The whole chain fits one page", trace.root["truncated"].asBoolean())
        trace.assertTraced(trace.call("OrderStore.save"), "OrderStore.save")

        val getterCalls = trace.service["callsInto"].filter { it["target"].asText().matches(FIELD_GETTER) }
        assertEquals(FIELD_COUNT, getterCalls.size)
        getterCalls.forEach { call ->
            trace.assertAccessor(call)
            assertEquals("PROJECT", call["kind"].asText())
        }
    }

    fun testComputedGetterIsTracedAsAnyProjectMethod() = runBlocking {
        val trace = traceFromHandler()

        trace.assertTraced(trace.call("OrderDto.getTotal"), "OrderDto.getTotal")
    }

    fun testTrivialSetterIsAnAccessor() = runBlocking {
        val trace = traceFromHandler()

        trace.assertAccessor(trace.call("OrderDto.setF0"))
    }

    fun testSameClassAccessorIsListedAndNotExpanded() = runBlocking {
        val trace = traceFromHandler()

        val baseCall = trace.call("OrderService.getBase")
        assertEquals("INTERNAL", baseCall["kind"].asText())
        trace.assertAccessor(baseCall)
        assertFalse(trace.chain.any { nameOf(it) == "OrderService.getBase" })
    }

    fun testKotlinPropertyAccessorsCalledFromJava() = runBlocking {
        val trace = traceFromHandler()

        listOf("CustomerDto.getName", "CustomerDto.setEmail", "CustomerDto.getLabel").forEach { target ->
            trace.assertAccessor(trace.call(target))
        }
        trace.assertTraced(trace.call("CustomerDto.getInitials"), "CustomerDto.getInitials")
        trace.assertTraced(trace.call("CustomerDto.getCode"), "CustomerDto.getCode")
    }

    fun testKotlinPropertyDelegatedToAnotherObjectIsNotAnAccessor() = runBlocking {
        val trace = traceFromHandler()

        trace.assertTraced(trace.call("NamedWrapper.getName"), "NamedWrapper.getName")
    }

    fun testAccessorGeneratedFromAFieldIsAnAccessor() = runBlocking {
        val trace = traceFromHandler()

        listOf("GeneratedDto.getCity", "GeneratedDto.setCity", "GeneratedDto.setCountry", "GeneratedDto.getPlain")
            .forEach { target -> trace.assertAccessor(trace.call(target)) }
    }

    fun testGeneratedMethodThatDoesNotMirrorItsFieldIsNotAnAccessor() = runBlocking {
        val trace = traceFromHandler()

        listOf("GeneratedDto.fetchCity", "GeneratedDto.getStreet", "GeneratedDto.getZip", "GeneratedDto.setZip")
            .forEach { target -> trace.assertTraced(trace.call(target), target) }
    }

    fun testGeneratedAccessorOfAFieldThatComputesItsValueIsNotAnAccessor() = runBlocking {
        val trace = traceFromHandler()

        listOf("GeneratedDto.getRegion", "GeneratedDto.getNamed")
            .forEach { target -> trace.assertTraced(trace.call(target), target) }
    }

    fun testRecordComponentAccessorIsAnAccessor() = runBlocking {
        val trace = traceFromHandler()

        trace.assertAccessor(trace.call("LineDto.sku"))
    }

    fun testAbstractAccessorIsNeverTrivial() {
        listOf("com.example.orders.Priced" to "getPrice", "com.example.orders.Titled" to "getTitle")
            .forEach { (className, methodName) ->
                val method = JavaPsiFacade.getInstance(project)
                    .findClass(className, GlobalSearchScope.projectScope(project))!!
                    .findMethodsByName(methodName, false).single()
                assertTrue("Precondition: $className.$methodName is abstract", method.isAbstract())
                assertFalse("$className.$methodName has no body to prove anything", TrivialAccessors.isTrivial(method))
            }
    }

    fun testOrdinaryCallRecordKeepsItsKeys() = runBlocking {
        val trace = traceFromHandler()

        val serviceCall = trace.chain[0]["callsInto"].single { it["target"].asText() == "OrderService.total" }
        assertEquals(listOf("target", "kind", "line", "node", "via"), serviceCall.fieldNames().asSequence().toList())
        assertEquals(
            listOf("target", "kind", "line", "node", "via", "accessor"),
            trace.call("OrderDto.getF1").fieldNames().asSequence().toList()
        )
    }

    private fun PsiMethod.isAbstract(): Boolean = hasModifierProperty(PsiModifier.ABSTRACT)

    private inner class Trace(val root: JsonNode) {
        val chain: JsonNode get() = root["chain"]
        val service: JsonNode = chain.single { nameOf(it) == "OrderService.total" }

        fun call(target: String): JsonNode = service["callsInto"].single { it["target"].asText() == target }

        fun assertAccessor(call: JsonNode) {
            assertTrue("${call["target"]} must be an accessor", call["accessor"]?.asBoolean() == true)
            assertTrue("${call["target"]} must not be traced", call["node"].isNull)
        }

        fun assertTraced(call: JsonNode, name: String) {
            assertNull("${call["target"]} is not an accessor", call["accessor"])
            assertFalse("${call["target"]} must be traced", call["node"].isNull)
            val node = chain.singleOrNull { it["id"].asInt() == call["node"].asInt() }
            assertNotNull("Node ${call["node"]} of ${call["target"]} must be on the page", node)
            assertEquals(name, nameOf(node!!))
        }
    }

    private fun generateMethodsOfGeneratedDto() {
        PsiAugmentProvider.EP_NAME.point.registerExtension(object : PsiAugmentProvider() {
            override fun <Psi : PsiElement> getAugments(
                element: PsiElement,
                type: Class<Psi>,
                nameHint: String?,
            ): List<Psi> {
                if (type != PsiMethod::class.java || element !is PsiClass || element.qualifiedName != GENERATED_DTO) {
                    return emptyList()
                }
                val generated = generatedMethodsOf(element).filter { nameHint == null || it.name == nameHint }
                @Suppress("UNCHECKED_CAST")
                return generated as List<Psi>
            }
        }, testRootDisposable)
    }

    private fun generatedMethodsOf(dto: PsiClass): List<PsiMethod> {
        val string = PsiType.getJavaLangString(dto.manager, dto.resolveScope)
        val self = JavaPsiFacade.getElementFactory(project).createType(dto)
        val street = dto.findInnerClassByName("Holder", false)!!.findFieldByName("street", false)!!
        fun field(name: String): PsiField = dto.findFieldByName(name, false)!!
        return listOf(
            generated(dto, "getCity", string, field("city")),
            generated(dto, "setCity", self, field("city"), parameter = string),
            generated(dto, "fetchCity", string, field("city")),
            generated(dto, "setCountry", PsiTypes.voidType(), field("country"), parameter = string),
            generated(dto, "getZip", PsiTypes.intType(), field("zip")),
            generated(dto, "setZip", PsiTypes.voidType(), field("zip"), parameter = PsiTypes.intType()),
            generated(dto, "getStreet", string, street),
            generated(dto, "getRegion", string, field("region")),
            generated(dto, "getNamed", field("named").type, field("named")),
            generated(dto, "getPlain", string, field("plain")),
        )
    }

    private fun generated(
        owner: PsiClass,
        name: String,
        returnType: PsiType,
        field: PsiField,
        parameter: PsiType? = null,
    ): PsiMethod = LightMethodBuilder(owner.manager, JavaLanguage.INSTANCE, name)
        .setMethodReturnType(returnType)
        .setContainingClass(owner)
        .addModifier(PsiModifier.PUBLIC)
        .apply { parameter?.let { addParameter("value", it) } }
        .also { it.navigationElement = field }

    private fun nameOf(node: JsonNode): String =
        "${node["className"].asText().substringAfterLast('.')}.${node["methodName"].asText()}"

    private suspend fun traceFromHandler(): Trace = Trace(
        mapper.readTree(
            toolset.traceCallChain(
                filePath = "$MAIN_ROOT/com/example/orders/OrderController.java",
                line = handlerLine(),
                projectPath = project.basePath!!,
                includeTests = false,
                limit = 50,
                maxChars = 16000,
            )
        )
    )

    private fun handlerLine(): Int {
        val index = ORDER_CONTROLLER_SOURCE.lines().indexOfFirst { it.contains("public int create(") }
        assertTrue("The handler declaration is absent from the fixture", index >= 0)
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
        const val MAIN_ROOT = "accessorMain"
        const val FIELD_COUNT = 60
        const val GENERATED_DTO = "com.example.orders.GeneratedDto"
        val FIELD_GETTER = Regex("OrderDto\\.getF\\d+")
        const val SERVICE_PARAMETERS =
            "OrderDto dto, CustomerDto customer, GeneratedDto generated, LineDto line, NamedWrapper wrapper"

        val LIBRARIES = listOf(
            TestLibrary.springWebMvc_6_0_7,
            TestLibrary.springContext_6_0_7,
            TestLibrary.kotlin_1_9_22,
        )

        val LOMBOK_GETTER_SOURCE = """
            package lombok;

            public @interface Getter {
                boolean lazy() default false;
            }
        """.trimIndent()

        val LOMBOK_DELEGATE_SOURCE = """
            package lombok.experimental;

            public @interface Delegate {
            }
        """.trimIndent()

        val ORDER_DTO_SOURCE = buildString {
            appendLine("package com.example.orders;")
            appendLine()
            appendLine("public class OrderDto {")
            for (i in 0 until FIELD_COUNT) appendLine("    private String f$i;")
            appendLine("    private int quantity;")
            appendLine("    private int unitPrice;")
            appendLine()
            appendLine("    public String getF0() { return this.f0; }")
            for (i in 1 until FIELD_COUNT) appendLine("    public String getF$i() { return f$i; }")
            appendLine("    public void setF0(String f0) { this.f0 = f0; }")
            appendLine("    public int getQuantity() { return quantity; }")
            appendLine("    public int getTotal() { return quantity * unitPrice; }")
            appendLine("}")
        }

        val ORDER_SERVICE_SOURCE = buildString {
            appendLine("package com.example.orders;")
            appendLine()
            appendLine("import org.springframework.stereotype.Service;")
            appendLine()
            appendLine("@Service")
            appendLine("public class OrderService {")
            appendLine("    private final OrderStore store;")
            appendLine("    private int base;")
            appendLine()
            appendLine("    public OrderService(OrderStore store) { this.store = store; }")
            appendLine()
            appendLine("    private int getBase() { return base; }")
            appendLine()
            appendLine("    public int total($SERVICE_PARAMETERS) {")
            appendLine("        int n = getBase();")
            for (i in 0 until FIELD_COUNT) appendLine("        n += dto.getF$i().hashCode();")
            appendLine("        dto.setF0(\"x\");")
            appendLine("        n += dto.getTotal();")
            appendLine("        n += customer.getName().hashCode();")
            appendLine("        customer.setEmail(\"x\");")
            appendLine("        n += customer.getLabel().hashCode();")
            appendLine("        n += customer.getInitials().hashCode();")
            appendLine("        n += customer.getCode().hashCode();")
            appendLine("        n += wrapper.getName().hashCode();")
            appendLine("        n += generated.getCity().hashCode();")
            appendLine("        generated.setCity(\"x\");")
            appendLine("        n += generated.fetchCity().hashCode();")
            appendLine("        generated.setCountry(\"x\");")
            appendLine("        n += generated.getZip();")
            appendLine("        generated.setZip(1);")
            appendLine("        n += generated.getStreet().hashCode();")
            appendLine("        n += generated.getRegion().hashCode();")
            appendLine("        n += generated.getNamed().hashCode();")
            appendLine("        n += generated.getPlain().hashCode();")
            appendLine("        n += line.sku().hashCode();")
            appendLine("        return n + store.save(dto);")
            appendLine("    }")
            appendLine("}")
        }

        val CUSTOMER_DTO_SOURCE = """
            package com.example.orders

            class CustomerDto(val name: String, var email: String) {
                val label: String = name
                val initials: String
                    get() = name.take(1)
                val code: String by lazy { name.uppercase() }
            }

            interface Named {
                val name: String
            }

            interface Titled {
                val title: String
            }

            class NamedWrapper(delegate: Named) : Named by delegate
        """.trimIndent()

        val PRICED_SOURCE = """
            package com.example.orders;

            public interface Priced {
                int getPrice();
            }
        """.trimIndent()

        val GENERATED_DTO_SOURCE = """
            package com.example.orders;

            public class GeneratedDto {
                private String city;
                private String country;
                private String zip;
                @lombok.Getter(lazy = true)
                private final String region = "r";
                @lombok.experimental.Delegate
                private final Named named = null;
                @lombok.Getter
                private final String plain = "p";

                public static class Holder {
                    private String street;
                }
            }
        """.trimIndent()

        val LINE_DTO_SOURCE = """
            package com.example.orders;

            public record LineDto(String sku) {
            }
        """.trimIndent()

        val ORDER_STORE_SOURCE = """
            package com.example.orders;

            import org.springframework.stereotype.Repository;

            @Repository
            public class OrderStore {
                public int save(OrderDto dto) { return dto.getTotal(); }
            }
        """.trimIndent()

        val ORDER_CONTROLLER_SOURCE = """
            package com.example.orders;

            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class OrderController {
                private final OrderService service;

                public OrderController(OrderService service) { this.service = service; }

                @PostMapping("/orders")
                public int create(@RequestBody $SERVICE_PARAMETERS) {
                    return service.total(dto, customer, generated, line, wrapper);
                }
            }
        """.trimIndent()
    }
}
