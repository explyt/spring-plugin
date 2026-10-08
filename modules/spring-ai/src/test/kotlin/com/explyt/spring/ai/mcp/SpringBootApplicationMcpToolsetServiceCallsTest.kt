/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.toUElementOfType
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

/**
 * Which calls on injected beans `explyt_get_spring_endpoint_contract` lists for a handler.
 *
 * A handler commonly resolves a tenant or checks access through one bean before it calls the service that answers
 * the request, so the first such call alone pointed callers at the guard. Every call is listed in source order.
 */
class SpringBootApplicationMcpToolsetServiceCallsTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springDataJpa_3_1_0,
        TestLibrary.kotlin_1_9_22,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("springBootApp", "")
        myFixture.addFileToProject("com/example/app/web/OrdersController.kt", SOURCE)
        myFixture.addFileToProject("explyt/web/SourceNames.kt", SOURCE_NAMES)
    }

    /** The guard is called first, the service second: both are listed, and `serviceCall` stays the first. */
    fun testGuardBeforeServiceListsBoth() = runBlocking<Unit> {
        val contract = contractOf("/api/stores/{id}/orders")

        assertEquals(
            listOf("com.example.app.web.TenantResolver.resolve", "com.example.app.web.OrdersService.list"),
            targets(contract)
        )
        assertEquals("com.example.app.web.TenantResolver.resolve", contract["serviceCall"]["target"].asText())
        assertEquals(lineOf("tenantResolver.resolve(request)"), contract["serviceCalls"][0]["callLine"].asInt())
        assertEquals(lineOf("ordersService.list(id, it)"), contract["serviceCalls"][1]["callLine"].asInt())
    }

    fun testExpressionBodiedHandlerHasOneCall() = runBlocking<Unit> {
        assertEquals(listOf("com.example.app.web.OrdersService.count"), targets(contractOf("/api/stores/{id}/count")))
    }

    fun testInjectedCompanionDependencyUsesItsSourceOwner() = runBlocking<Unit> {
        val callee = sourceNameCallee("handle")
        assertTrue((callee.containingClass?.navigationElement as? KtObjectDeclaration)?.isCompanion() == true)
        assertEquals("com.example.app.web.SourceHandler.Companion", callee.containingClass?.qualifiedName)
        assertTrue(callee.containingClass!!.interfaces.any { it.qualifiedName == "com.example.app.web.HandlerApi" })
        val contract = contractOf("/source-names/companion")
        assertEquals("com.example.app.web.SourceHandler.handle", contract["serviceCalls"].single()["target"].asText())
        assertFalse(contract["serviceCalls"].single()["target"].asText().contains(".Companion."))
    }

    fun testInternalServiceMethodUsesItsSourceName() = runBlocking<Unit> {
        val callee = sourceNameCallee("calculate")
        val source = callee.navigationElement as KtNamedFunction
        assertTrue(source.hasModifier(KtTokens.INTERNAL_KEYWORD))
        assertEquals("calculate", source.name)
        val target = targets(contractOf("/source-names/internal")).single()
        assertEquals("com.example.app.web.InternalService.calculate", target)
        assertFalse(target.contains('\u0024'))
    }

    fun testClassNestedInCompanionKeepsItsQualifiedSourceName() = runBlocking<Unit> {
        val callee = sourceNameCallee("parse")
        assertEquals("com.example.app.web.Owner.Companion.Nested", callee.containingClass?.qualifiedName)
        assertFalse((callee.containingClass?.navigationElement as? KtObjectDeclaration)?.isCompanion() == true)
        assertEquals(
            "com.example.app.web.Owner.Companion.Nested.parse",
            targets(contractOf("/source-names/nested")).single()
        )
    }

    fun testNamedCompanionUsesItsOuterSourceOwner() = runBlocking<Unit> {
        val callee = sourceNameCallee("create")
        val owner = callee.containingClass?.navigationElement as KtObjectDeclaration
        assertTrue(owner.isCompanion())
        assertEquals("Factory", owner.name)
        assertEquals(
            "com.example.app.web.FactoryOwner.create",
            targets(contractOf("/source-names/factory")).single()
        )
    }

    /** Two calls of one bean method on different lines are two places a change has to be made. */
    fun testSameMethodOnTwoLinesIsListedTwice() = runBlocking<Unit> {
        val contract = contractOf("/api/stores/{id}/compare")

        assertEquals(
            listOf("com.example.app.web.OrdersService.count", "com.example.app.web.OrdersService.count"),
            targets(contract)
        )
        assertEquals(
            listOf(lineOf("val before = ordersService.count(id)"), lineOf("val after = ordersService.count(id + 1)")),
            contract["serviceCalls"].map { it["callLine"].asInt() }
        )
    }

    fun testFrameworkAndStdlibCallsAreNeverListed() = runBlocking<Unit> {
        val contract = contractOf("/api/stores/{id}/summary")

        assertEquals(listOf("com.example.app.web.OrdersService.count"), targets(contract))
    }

    /**
     * Kotlin copies a nullable bean into a local `val` to smart-cast it; the local is the bean, so its calls are the
     * handler's service calls. The UAST visit lists an outer call before the calls in its arguments.
     */
    fun testCallsOnLocalCopiesOfNullableBeansAreListed() = runBlocking<Unit> {
        val contract = contractOf("/api/items/{id}/activity")

        assertEquals(
            listOf(
                "com.example.app.web.ItemGuard.requireVisible",
                "com.example.app.web.TenantResolver.resolve",
                "com.example.app.web.ItemStatsService.activity",
                "com.example.app.web.TenantResolver.resolveAdmin",
            ),
            targets(contract)
        )
        assertEquals(lineOf("guard.requireVisible(id"), contract["serviceCalls"][0]["callLine"].asInt())
        assertEquals(lineOf("stats.activity(id"), contract["serviceCalls"][2]["callLine"].asInt())
    }

    fun testElvisThrowAndNonNullAssertionKeepTheBean() = runBlocking<Unit> {
        assertEquals(
            listOf("com.example.app.web.ImageStore.put", "com.example.app.web.ItemStatsService.count"),
            targets(contractOf("/api/items/{id}/image"))
        )
    }

    /** A cast, in parentheses or not, changes the static type only: the local still holds the bean. */
    fun testCastKeepsTheBean() = runBlocking<Unit> {
        assertEquals(listOf("com.example.app.web.ImageStore.put"), targets(contractOf("/api/items/{id}/cast")))
    }

    /** A `var` can be pointed elsewhere and a local built from scratch is not the bean: neither is a service call. */
    fun testReassignableOrUnrelatedLocalsAreNotTheBean() = runBlocking<Unit> {
        assertEquals(emptyList<String>(), targets(contractOf("/api/items/{id}/other")))
    }

    /**
     * An elvis whose right side is another value can hold either side, so the local is not a copy of the left one;
     * only a right side that leaves the scope - `throw`, `return`, `error()` - makes the result the left side.
     */
    fun testElvisWithAFallbackValueIsNotACopy() = runBlocking<Unit> {
        assertEquals(emptyList<String>(), targets(contractOf("/api/items/{id}/fallback")))
    }

    fun testFinalJavaLocalCopyIsTheBean() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/web/JavaItemsController.java", JAVA_SOURCE)

        assertEquals(
            listOf("com.example.app.web.ItemStatsService.count", "com.example.app.web.ItemStatsService.count"),
            targets(contractOf("/api/java-items/{id}/copied"))
        )
    }

    fun testReassignedJavaLocalIsNotTheBean() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/web/JavaItemsController.java", JAVA_SOURCE)

        assertEquals(emptyList<String>(), targets(contractOf("/api/java-items/{id}/reassigned")))
    }

    /**
     * A bean method passed as a callable reference is invoked by the function it is passed to, so it is a call of the
     * handler on that bean - here through a local copy, after an ordinary call on another bean.
     */
    fun testBeanMethodPassedAsAReferenceIsListed() = runBlocking<Unit> {
        val contract = contractOf("/api/logos/{id}/upload")

        assertEquals(
            listOf("com.example.app.web.LogoStore.exists", "com.example.app.web.LogoValidator.validate"),
            targets(contract)
        )
        assertEquals(lineOf("input.use(validator::validate)"), contract["serviceCalls"][1]["callLine"].asInt())
    }

    fun testReferenceOnAConstructorInjectedFieldIsListed() = runBlocking<Unit> {
        assertEquals(listOf("com.example.app.web.LogoStore.save"), targets(contractOf("/api/logos/batch")))
    }

    /**
     * None of these is a call on an injected bean:
     * - `String::trim` names a JDK method;
     * - `this::normalize` names the controller's own helper;
     * - `LogoStore::size` qualifies the method by a type, not by an injected instance;
     * - `logoStore::capacity` reads a property of the bean, although it resolves to the getter.
     */
    fun testReferencesThatAreNotOnAnInjectedBeanAreNotListed() = runBlocking<Unit> {
        assertEquals(emptyList<String>(), targets(contractOf("/api/logos/names")))
    }

    fun testJavaMethodReferenceOnAnInjectedFieldIsListed() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/web/JavaItemsController.java", JAVA_SOURCE)

        assertEquals(
            listOf("com.example.app.web.ItemStatsService.count", "com.example.app.web.ItemStatsService.count"),
            targets(contractOf("/api/java-items/{id}/referenced"))
        )
    }

    fun testHandlerWithoutBeanCallsHasAnEmptyList() = runBlocking<Unit> {
        val contract = contractOf("/api/stores/ping")

        assertTrue("serviceCalls is always present", contract.has("serviceCalls"))
        assertEquals(0, contract["serviceCalls"].size())
        assertTrue(contract["serviceCall"].isNull)
    }

    fun testInheritedRepositoryMethodIsListedWithItsLibrary() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/owners/OwnersController.kt", OWNERS_SOURCE)
        val repository = myFixture.findClass("com.example.app.owners.OwnerRepository")
        assertEquals(
            "org.springframework.data.repository.CrudRepository",
            repository.findMethodsByName("save", true).single().containingClass?.qualifiedName
        )

        val contract = contractOf("/owners/{id}/edit")

        assertEquals(
            listOf("com.example.app.owners.OwnerRepository.findByName", "com.example.app.owners.OwnerRepository.save"),
            targets(contract)
        )
        val save = contract["serviceCalls"][1]
        assertTrue(save["filePath"].isNull)
        assertTrue("library of save: $save", save["library"].asText().isNotBlank())
        assertEquals(ownersLineOf("return owners.save(owner)"), save["callLine"].asInt())
        assertEquals(setOf("target", "filePath", "library", "line", "callLine"), save.fieldNames().asSequence().toSet())
        val declared = contract["serviceCalls"][0]
        assertEquals("com/example/app/owners/OwnersController.kt", declared["filePath"].asText())
        assertEquals(setOf("target", "filePath", "line", "callLine"), declared.fieldNames().asSequence().toSet())
    }

    /** A call in the handler's own helper is made where the helper calls it, in place of the call of the helper. */
    fun testCallsMadeThroughOwnHelpersAreListed() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/owners/OwnersController.kt", OWNERS_SOURCE)

        val contract = contractOf("/owners/list")

        assertEquals(
            listOf(
                "com.example.app.owners.OwnerAudit.record",
                "com.example.app.owners.OwnerRepository.findAll",
                "com.example.app.owners.OwnerAudit.loop",
            ),
            targets(contract)
        )
        assertEquals(ownersLineOf("owners.findAll(PageRequest"), contract["serviceCalls"][1]["callLine"].asInt())
        assertEquals(contract["serviceCall"], contract["serviceCalls"][0])
    }

    fun testRecursiveHelperIsFollowedOnce() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/owners/OwnersController.kt", OWNERS_SOURCE)

        assertEquals(listOf("com.example.app.owners.OwnerAudit.loop"), targets(contractOf("/owners/recursive")))
    }

    /**
     * A helper of another class is not the handler's code, a repository passed to it is no longer the injected field,
     * and a locally created object is not the bean.
     */
    fun testHelpersOfOtherClassesAndLocalObjectsAreNotListed() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/owners/OwnersController.kt", OWNERS_SOURCE)
        myFixture.addFileToProject(
            "com/example/app/owners/OwnerAuditing.kt",
            "package com.example.app.owners\n\nfun recordElsewhere(controller: OwnersController) = controller.audit.record(9)\n"
        )

        assertEquals(emptyList<String>(), targets(contractOf("/owners/elsewhere")))
    }

    fun testUnresolvedMethodOnAnInjectedBeanIsListedAsUnresolved() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/owners/OwnersController.kt", OWNERS_SOURCE)

        val calls = contractOf("/owners/missing")["serviceCalls"]

        assertEquals(listOf("OwnerRepository.missingMethod", "com.example.app.owners.OwnerAudit.record"), calls.map { it["target"].asText() })
        val unresolved = calls[0]
        assertFalse(unresolved["resolved"].asBoolean(true))
        assertTrue(unresolved["filePath"].isNull)
        assertTrue(unresolved["line"].isNull)
        assertEquals(ownersLineOf("owners.missingMethod(1)"), unresolved["callLine"].asInt())
        assertFalse(calls[1].has("resolved"))
    }

    private fun sourceNameCallee(name: String): PsiMethod {
        val file = myFixture.findFileInTempDir("explyt/web/SourceNames.kt")
        val psi = com.intellij.psi.PsiManager.getInstance(project).findFile(file)!!
        val call = PsiTreeUtil.findChildrenOfType(psi, KtCallExpression::class.java)
            .single { it.calleeExpression?.text == name }
        val callee = call.toUElementOfType<UCallExpression>()?.resolve()
        assertNotNull("The fixture call to $name must resolve", callee)
        return callee!!
    }

    private fun ownersLineOf(anchor: String): Int {
        val index = OWNERS_SOURCE.lines().indexOfFirst { anchor in it }
        assertTrue("Anchor '$anchor' is absent from the fixture", index >= 0)
        return index + 1
    }

    private suspend fun contractOf(url: String): JsonNode {
        val endpoints = mapper.readTree(
            toolset.getEndpointContract(urlPattern = url, projectPath = project.basePath, httpMethod = "GET")
        )["endpoints"]
        assertEquals("Exactly one contract for $url", 1, endpoints.size())
        return endpoints[0]
    }

    private fun targets(contract: JsonNode): List<String> = contract["serviceCalls"].map { it["target"].asText() }

    private fun lineOf(anchor: String): Int {
        val index = SOURCE.lines().indexOfFirst { anchor in it }
        assertTrue("Anchor '$anchor' is absent from the fixture", index >= 0)
        return index + 1
    }

    private companion object {
        val SOURCE = """
            package com.example.app.web

            import org.springframework.http.ResponseEntity
            import org.springframework.stereotype.Service
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RequestParam
            import org.springframework.web.bind.annotation.RestController

            @Service
            class TenantResolver {
                fun resolve(request: String): String = request
                fun resolveAdmin(request: String): String = request
            }

            @Service
            class OrdersService {
                fun list(id: Long, tenant: String): List<String> = listOf(tenant)
                fun count(id: Long): Long = id
            }

            @RestController
            @RequestMapping("/api/stores")
            class OrdersController(private val tenantResolver: TenantResolver, private val ordersService: OrdersService) {
                @GetMapping("/{id}/orders")
                fun orders(@PathVariable id: Long, @RequestParam request: String) =
                    tenantResolver.resolve(request).let { ordersService.list(id, it) }

                @GetMapping("/{id}/count")
                fun count(@PathVariable id: Long) = ordersService.count(id)

                @GetMapping("/{id}/compare")
                fun compare(@PathVariable id: Long): Long {
                    val before = ordersService.count(id)
                    val after = ordersService.count(id + 1)
                    return after - before
                }

                @GetMapping("/{id}/summary")
                fun summary(@PathVariable id: Long): ResponseEntity<String> {
                    val label = "store-${'$'}id".trim().uppercase()
                    return ResponseEntity.ok(label + ordersService.count(id))
                }

                @GetMapping("/ping")
                fun ping() = ResponseEntity.ok("pong".trim())
            }

            @Service
            class ItemGuard {
                fun requireVisible(id: Long, tenant: String) = Unit
            }

            @Service
            class ItemStatsService {
                fun activity(id: Long, tenant: String): List<String> = listOf(tenant)
                fun count(id: Long): Long = id
            }

            @Service
            class ImageStore {
                fun put(id: Long): Long = id
            }

            class Unrelated {
                fun count(id: Long): Long = id
            }

            @RestController
            @RequestMapping("/api/items")
            class ItemsController(
                private val tenantResolver: TenantResolver,
                private val statsService: ItemStatsService?,
                private val itemGuard: ItemGuard?,
                private val imageStore: ImageStore?,
            ) {
                @GetMapping("/{id}/activity")
                fun activity(@PathVariable id: Long, @RequestParam request: String): List<String> {
                    val stats = statsService; val guard = itemGuard
                    if (stats == null || guard == null) throw IllegalStateException("disabled")
                    guard.requireVisible(id, tenantResolver.resolve(request))
                    return stats.activity(id, tenantResolver.resolveAdmin(request))
                }

                @GetMapping("/{id}/image")
                fun image(@PathVariable id: Long): Long {
                    val store = imageStore ?: throw IllegalStateException("no store")
                    val stats = statsService!!
                    return store.put(id) + stats.count(id)
                }

                @GetMapping("/{id}/cast")
                fun cast(@PathVariable id: Long): Long {
                    val store = (imageStore as ImageStore)
                    return store.put(id)
                }

                @GetMapping("/{id}/fallback")
                fun fallback(@PathVariable id: Long): Long {
                    val store = imageStore ?: ImageStore()
                    return store.put(id)
                }

                @GetMapping("/{id}/other")
                fun other(@PathVariable id: Long): Long {
                    var stats: ItemStatsService? = statsService
                    stats = ItemStatsService()
                    val fresh = Unrelated()
                    val replaced = stats.count(id)
                    return replaced + fresh.count(id)
                }
            }

            @Service
            class LogoValidator {
                fun validate(input: java.io.InputStream): ByteArray = input.readBytes()
            }

            @Service
            class LogoStore {
                fun exists(id: Long): Boolean = id > 0
                fun save(name: String) = Unit
                fun size(): Int = 0
                val capacity: Int get() = 10
            }

            @RestController
            @RequestMapping("/api/logos")
            class LogosController(private val logoValidator: LogoValidator?, private val logoStore: LogoStore) {
                @GetMapping("/{id}/upload")
                fun upload(@PathVariable id: Long, @RequestParam input: java.io.InputStream): Int {
                    val validator = logoValidator ?: throw IllegalStateException("no validator")
                    if (!logoStore.exists(id)) return 0
                    val image = input.use(validator::validate)
                    return image.size
                }

                @GetMapping("/batch")
                fun batch(@RequestParam names: List<String>) {
                    names.forEach(logoStore::save)
                }

                @GetMapping("/names")
                fun names(@RequestParam names: List<String>, @RequestParam stores: List<LogoStore>): List<String> {
                    val sizes = stores.map(LogoStore::size)
                    val capacity = logoStore::capacity
                    return names.map(String::trim).map(this::normalize) + sizes.map { it.toString() } +
                        capacity.get().toString()
                }

                private fun normalize(name: String): String = name.lowercase()
            }
        """.trimIndent()

        val OWNERS_SOURCE = """
            package com.example.app.owners

            import org.springframework.data.domain.PageRequest
            import org.springframework.data.jpa.repository.JpaRepository
            import org.springframework.stereotype.Service
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RequestParam
            import org.springframework.web.bind.annotation.RestController

            class Owner(val id: Int)

            @org.springframework.stereotype.Repository
            interface OwnerRepository : JpaRepository<Owner, Int> {
                fun findByName(name: String): Owner?
            }

            @Service
            class OwnerAudit {
                fun record(id: Int) = Unit
                fun loop(id: Int): Int = id
            }

            object OwnerHelpers {
                fun countAll(repository: OwnerRepository): Long = repository.count()
            }

            @RestController
            @RequestMapping("/owners")
            class OwnersController(private val owners: OwnerRepository, val audit: OwnerAudit) {
                @GetMapping("/{id}/edit")
                fun edit(@PathVariable id: Int): Owner {
                    val owner = Owner(id)
                    owners.findByName("x")
                    return owners.save(owner)
                }

                @GetMapping("/list")
                fun list(@RequestParam page: Int): List<Owner> {
                    audit.record(page)
                    return findPaginated(page)
                }

                @GetMapping("/recursive")
                fun recursive(@RequestParam n: Int): Int = countDown(n)

                @GetMapping("/elsewhere")
                fun elsewhere(): Long {
                    val fresh = OwnerAudit()
                    fresh.record(1)
                    recordElsewhere(this)
                    OwnersController(owners, audit).recursive(0)
                    return OwnerHelpers.countAll(owners) + java.util.Collections.emptyList<Int>().size
                }

                @GetMapping("/missing")
                fun missing() {
                    owners.missingMethod(1)
                    audit.record(2)
                }

                private fun findPaginated(page: Int): List<Owner> {
                    val result = owners.findAll(PageRequest.of(page, 5)).content
                    countDown(page)
                    return result
                }

                private fun countDown(n: Int): Int = if (n <= 0) audit.loop(n) else countDown(n - 1)
            }
        """.trimIndent()

        val SOURCE_NAMES = """
            package com.example.app.web

            import org.springframework.stereotype.Service
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            interface HandlerApi {
                fun handle(): String
            }

            @Service
            class SourceHandler {
                @Service
                companion object : HandlerApi {
                    override fun handle(): String = "handled"
                }
            }

            @Service
            class InternalService {
                internal fun calculate(): String = "calculated"
            }

            class Owner {
                companion object {
                    @Service
                    class Nested {
                        fun parse(): String = "nested"
                    }
                }
            }

            class FactoryOwner {
                @Service
                companion object Factory {
                    fun create(): String = "created"
                }
            }

            @RestController
            @RequestMapping("/source-names")
            class SourceNamesController(
                private val handler: SourceHandler.Companion,
                private val internalService: InternalService,
                private val nestedParser: Owner.Companion.Nested,
                private val factory: FactoryOwner.Factory,
            ) {
                @GetMapping("/companion")
                fun companion() = handler.handle()

                @GetMapping("/internal")
                fun internal() = internalService.calculate()

                @GetMapping("/nested")
                fun nested() = nestedParser.parse()

                @GetMapping("/factory")
                fun factory() = factory.create()
            }
        """.trimIndent()

        val JAVA_SOURCE = """
            package com.example.app.web;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class JavaItemsController {
                private final ItemStatsService statsService;

                public JavaItemsController(ItemStatsService statsService) {
                    this.statsService = statsService;
                }

                @GetMapping("/api/java-items/{id}/copied")
                public long item(@PathVariable long id) {
                    final ItemStatsService stats = statsService;
                    ItemStatsService effectivelyFinal = statsService;
                    long declared = stats.count(id);
                    return declared + effectivelyFinal.count(id);
                }

                @GetMapping("/api/java-items/{id}/reassigned")
                public long reassigned(@PathVariable long id) {
                    ItemStatsService stats = statsService;
                    stats = new ItemStatsService();
                    return stats.count(id);
                }

                @GetMapping("/api/java-items/{id}/referenced")
                public long referenced(@PathVariable long id) {
                    java.util.function.LongUnaryOperator viaField = this.statsService::count;
                    java.util.function.LongUnaryOperator viaName = statsService::count;
                    return viaField.applyAsLong(id) + viaName.applyAsLong(id + 1);
                }
            }
        """.trimIndent()
    }
}
