/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.beans.NativeBeanSnapshotReader
import com.explyt.spring.core.service.beans.ScopedBeanInjectionResolver
import com.explyt.spring.core.service.conditional.ConditionAssumption
import com.explyt.spring.core.service.conditional.ConditionReason
import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.McpToolset
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.IndexingTestUtil
import kotlinx.coroutines.runBlocking

class SpringBeanMcpConditionVerdictTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        addJava(
            "App",
            """
            package com.explyt.demo;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class App {}
            """
        )
    }

    fun testPropertyGatedControllerWithoutPropertyIsReportedAsInactiveCandidate() = runBlocking {
        addProperties("admin.marker=present")
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Inactive>(SYNC_ADMIN_CONTROLLER)

        val root = call(beanName = "syncAdminController")

        assertEquals("OK", root["status"].asText())
        assertEquals("NONE", root["outcome"].asText())
        assertEquals(0, root["totalCount"].asInt())
        val inactive = singleInactiveCandidateOf(root)
        assertEquals("syncAdminController", inactive["name"].asText())
        assertEquals(SYNC_ADMIN_CONTROLLER, inactive["type"].asText())
        val condition = conditionOf(inactive)
        assertEquals("INACTIVE", condition["state"].asText())
        assertEquals(CONDITIONAL_ON_PROPERTY, condition["annotation"].asText())
        assertEquals(SYNC_ADMIN_CONTROLLER, condition["carrier"].asText())
        assertTrue("detail must name the property, got $condition", "admin.sync.enabled" in condition["detail"].asText())
    }

    fun testPropertyGatedControllerWithPropertyIsAnUnconditionedCandidate() = runBlocking {
        addProperties("admin.sync.enabled=true")
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Active>(SYNC_ADMIN_CONTROLLER)

        val root = call(beanName = "syncAdminController")

        assertEquals("SINGLE", root["outcome"].asText())
        assertEquals("COMPLETE", root["matchCompleteness"].asText())
        val candidate = root["candidates"].single()
        assertEquals("syncAdminController", candidate["name"].asText())
        assertFalse("an active candidate carries no condition, got $candidate", candidate.has("condition"))
        assertTrue("no inactive candidates, got $root", inactiveCandidatesOf(root).isEmpty())
        assertFalse(CONDITIONS_UNDECIDED in limitationsOf(root))
    }

    fun testUnresolvablePlaceholderMakesTheCandidateUndecided() = runBlocking {
        addProperties("admin.sync.enabled=\${SYNC_ENABLED}")
        addSyncAdminController()
        assertUndecidedBecause(SYNC_ADMIN_CONTROLLER, ConditionReason.PROPERTY_UNRESOLVABLE)

        val root = call(beanName = "syncAdminController")

        assertEquals("INDETERMINATE", root["outcome"].asText())
        assertEquals("PARTIAL", root["matchCompleteness"].asText())
        assertTrue("limitations were ${limitationsOf(root)}", CONDITIONS_UNDECIDED in limitationsOf(root))
        val condition = conditionOf(root["candidates"].single())
        assertEquals("UNDECIDED", condition["state"].asText())
        assertEquals(listOf("PROPERTY_UNRESOLVABLE"), reasonsOf(condition))
        assertEquals(CONDITIONAL_ON_PROPERTY, condition["annotation"].asText())
        assertEquals(SYNC_ADMIN_CONTROLLER, condition["carrier"].asText())
        assertTrue("detail must name the property, got $condition", "admin.sync.enabled" in condition["detail"].asText())
    }

    fun testCustomConditionMakesTheCandidateUndecided() = runBlocking {
        addJava(
            "MyCondition",
            """
            package com.explyt.demo;

            import org.springframework.context.annotation.Condition;
            import org.springframework.context.annotation.ConditionContext;
            import org.springframework.core.type.AnnotatedTypeMetadata;

            public class MyCondition implements Condition {
                @Override
                public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
                    return false;
                }
            }
            """
        )
        addJava(
            "CustomService",
            """
            package com.explyt.demo;

            import org.springframework.context.annotation.Conditional;
            import org.springframework.stereotype.Component;

            @Component
            @Conditional(MyCondition.class)
            public class CustomService {}
            """
        )
        assertUndecidedBecause(CUSTOM_SERVICE, ConditionReason.UNSUPPORTED_CONDITION)

        val root = call(beanName = "customService")

        assertEquals("INDETERMINATE", root["outcome"].asText())
        assertEquals("PARTIAL", root["matchCompleteness"].asText())
        assertTrue("limitations were ${limitationsOf(root)}", CONDITIONS_UNDECIDED in limitationsOf(root))
        val condition = conditionOf(root["candidates"].single())
        assertEquals("UNDECIDED", condition["state"].asText())
        assertEquals(listOf("UNSUPPORTED_CONDITION"), reasonsOf(condition))
        assertEquals(CONDITIONAL, condition["annotation"].asText())
    }

    fun testUnresolvableProfileMakesTheCandidateUndecidedAndNamesTheProfileLimitation() = runBlocking {
        addJava(
            "ProfiledService",
            """
            package com.explyt.demo;

            import org.springframework.context.annotation.Profile;
            import org.springframework.stereotype.Component;

            @Component
            @Profile(UNKNOWN_PROFILE)
            public class ProfiledService {}
            """
        )
        assertUndecidedBecause(PROFILED_SERVICE, ConditionReason.PROFILE_NOT_DECIDABLE)

        val root = call(beanName = "profiledService")

        assertEquals("INDETERMINATE", root["outcome"].asText())
        assertEquals("PARTIAL", root["matchCompleteness"].asText())
        val limitations = limitationsOf(root)
        assertTrue("limitations were $limitations", CONDITIONS_UNDECIDED in limitations)
        assertTrue("limitations were $limitations", PROFILE_NOT_DECIDABLE in limitations)
        val condition = conditionOf(root["candidates"].single())
        assertEquals("UNDECIDED", condition["state"].asText())
        assertEquals(listOf("PROFILE_NOT_DECIDABLE"), reasonsOf(condition))
    }

    fun testTypeLookupKeepsTheActiveBeanAndListsTheInactiveOneSeparately() = runBlocking {
        addFooBeans()
        assertVerdict<ConditionVerdict.Active>(ACTIVE_FOO)
        assertVerdict<ConditionVerdict.Inactive>(INACTIVE_FOO)

        val root = call(typeFqn = FOO)

        assertEquals("SINGLE", root["outcome"].asText())
        assertEquals("COMPLETE", root["matchCompleteness"].asText())
        assertEquals(1, root["totalCount"].asInt())
        val candidate = root["candidates"].single()
        assertEquals("activeFoo", candidate["name"].asText())
        assertFalse(candidate.has("condition"))
        val inactive = singleInactiveCandidateOf(root)
        assertEquals("inactiveFoo", inactive["name"].asText())
        assertEquals("INACTIVE", conditionOf(inactive)["state"].asText())
    }

    fun testAnUndecidedBeanOfAnotherTypeDoesNotMakeTheQueryPartial() = runBlocking {
        addFooBeans()
        addProperties("admin.sync.enabled=\${SYNC_ENABLED}")
        addSyncAdminController()
        assertUndecidedBecause(SYNC_ADMIN_CONTROLLER, ConditionReason.PROPERTY_UNRESOLVABLE)
        assertVerdict<ConditionVerdict.Active>(ACTIVE_FOO)

        val root = call(typeFqn = FOO)

        assertEquals("SINGLE", root["outcome"].asText())
        assertEquals("COMPLETE", root["matchCompleteness"].asText())
        assertFalse("limitations were ${limitationsOf(root)}", CONDITIONS_UNDECIDED in limitationsOf(root))
        assertEquals("activeFoo", root["candidates"].single()["name"].asText())
    }

    fun testInjectionPointWhoseOnlyCandidateIsInactiveListsIt() = runBlocking {
        addProperties("admin.marker=present")
        addSyncAdminController()
        val consumer = addJava(
            "SyncClient",
            """
            package com.explyt.demo;

            import org.springframework.stereotype.Component;

            @Component
            public class SyncClient {
                private final SyncAdminController controller;

                public SyncClient(SyncAdminController controller) {
                    this.controller = controller;
                }
            }
            """
        )
        assertVerdict<ConditionVerdict.Inactive>(SYNC_ADMIN_CONTROLLER)
        val (line, column) = positionOf(consumer, "SyncClient(SyncAdminController controller)", "SyncClient(SyncAdminController ".length)

        val root = call(filePath = "com/explyt/demo/SyncClient.java", line = line, column = column)

        assertEquals("INJECTION", root["mode"].asText())
        assertEquals("NO_CANDIDATE", root["outcome"].asText())
        assertEquals(0, root["totalCount"].asInt())
        val inactive = singleInactiveCandidateOf(root)
        assertEquals("syncAdminController", inactive["name"].asText())
        assertEquals("INACTIVE", conditionOf(inactive)["state"].asText())
    }

    fun testDisabledConditionFilterIsNamedAsALimitation() = runBlocking {
        addProperties("admin.marker=present")
        addSyncAdminController()
        val settings = SpringToolRunConfigurationsSettingsState.getInstance()
        val enabled = settings.isBeanFilterEnabled
        settings.isBeanFilterEnabled = false
        try {
            ModificationTrackerManager.getInstance(project).invalidateAll()

            val root = call(beanName = "syncAdminController")

            assertEquals("OK", root["status"].asText())
            assertTrue("limitations were ${limitationsOf(root)}", CONDITIONS_NOT_EVALUATED in limitationsOf(root))
            val candidate = root["candidates"].single()
            assertEquals("syncAdminController", candidate["name"].asText())
            assertFalse("no verdict without evaluation, got $candidate", candidate.has("condition"))
            assertTrue(inactiveCandidatesOf(root).isEmpty())
        } finally {
            settings.isBeanFilterEnabled = enabled
            ModificationTrackerManager.getInstance(project).invalidateAll()
        }
    }

    fun testTheDefaultAnswerWithAnInactiveCandidateFitsTheClientBudget() = runBlocking {
        addProperties("admin.marker=present")
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Inactive>(SYNC_ADMIN_CONTROLLER)

        val json = toolset().findSpringBean(projectPath = project.basePath!!, beanName = "syncAdminController")
        val root = mapper.readTree(json)

        assertEquals("OK", root["status"].asText())
        assertEquals("Precondition: the inactive candidate must be in the answer", 1, inactiveCandidatesOf(root).size)
        assertTrue("payload was ${json.length} chars", json.length <= MAX_DEFAULT_PAYLOAD)
    }

    fun testTwoUndecidedConditionsOnOneBeanAreBothReported() = runBlocking {
        addProperties("twin.enabled=\${TWIN_ENABLED}")
        addMyCondition()
        addJava(
            "TwinService",
            """
            package com.explyt.demo;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Conditional;
            import org.springframework.stereotype.Component;

            @Component
            @Conditional(MyCondition.class)
            @ConditionalOnProperty(name = "twin.enabled", havingValue = "true")
            public class TwinService {}
            """
        )
        val verdict = verdictOf("com.explyt.demo.TwinService")
        assertTrue("Precondition: TwinService must be Undecided, got $verdict", verdict is ConditionVerdict.Undecided)
        assertEquals(
            "Precondition: both reasons are in the model",
            setOf(ConditionReason.PROPERTY_UNRESOLVABLE, ConditionReason.UNSUPPORTED_CONDITION),
            (verdict as ConditionVerdict.Undecided).conditions.map { it.reason }.toSet()
        )

        val root = call(beanName = "twinService")

        assertEquals("INDETERMINATE", root["outcome"].asText())
        val condition = conditionOf(root["candidates"].single())
        assertEquals("UNDECIDED", condition["state"].asText())
        assertEquals(setOf("PROPERTY_UNRESOLVABLE", "UNSUPPORTED_CONDITION"), reasonsOf(condition).toSet())
        assertEquals(2, reasonsOf(condition).size)
    }

    fun testDetailsAddIdAndAssumptionsToAnInactiveCandidate() = runBlocking {
        addMissingBeanService()
        val verdict = verdictOf(NEEDS_MISSING)
        assertTrue("Precondition: NeedsMissing must be Inactive, got $verdict", verdict is ConditionVerdict.Inactive)
        assertEquals(
            setOf(ConditionAssumption.STATIC_BEAN_MODEL_COMPLETE),
            (verdict as ConditionVerdict.Inactive).condition.assumptions
        )

        val root = call(beanName = "needsMissing", includeDetails = true)

        val inactive = singleInactiveCandidateOf(root)
        assertTrue("id expected with details, got $inactive", inactive["id"]?.asText().orEmpty().isNotBlank())
        val condition = conditionOf(inactive)
        assertEquals(CONDITIONAL_ON_BEAN, condition["annotation"].asText())
        assertEquals(listOf("STATIC_BEAN_MODEL_COMPLETE"), condition["assumptions"].map { it.asText() })
        assertFalse("an INACTIVE condition carries no reasons, got $condition", condition.has("reasons"))
    }

    fun testWithoutDetailsAnInactiveCandidateHasIdButNoAssumptions() = runBlocking {
        addMissingBeanService()
        assertVerdict<ConditionVerdict.Inactive>(NEEDS_MISSING)

        val inactive = singleInactiveCandidateOf(call(beanName = "needsMissing"))

        assertTrue("an inactive candidate carries its id like an active one, got $inactive", inactive["id"]?.asText().orEmpty().isNotBlank())
        assertFalse("compact answer carries no assumptions, got $inactive", conditionOf(inactive).has("assumptions"))
    }

    fun testInjectionWithAMatchingQualifierListsTheInactiveBean() = runBlocking {
        val root = qualifiedInjection("fast")

        assertEquals("NO_CANDIDATE", root["outcome"].asText())
        val inactive = singleInactiveCandidateOf(root)
        assertEquals("fastEngine", inactive["name"].asText())
        assertEquals("INACTIVE", conditionOf(inactive)["state"].asText())
    }

    fun testInjectionWithAnotherQualifierDoesNotListTheInactiveBean() = runBlocking {
        val root = qualifiedInjection("slow")

        assertEquals("NO_CANDIDATE", root["outcome"].asText())
        assertTrue("no inactive candidate expected, got $root", inactiveCandidatesOf(root).isEmpty())
    }

    fun testLookupByAnAliasOfAnInactiveBeanNamesTheMatchedAlias() = runBlocking {
        addJava(
            "Engine",
            """
            package com.explyt.demo;

            public class Engine {}
            """
        )
        addJava(
            "AliasConfig",
            """
            package com.explyt.demo;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class AliasConfig {
                @Bean(name = {"mainEngine", "spareEngine"})
                @ConditionalOnProperty(name = "engine.enabled", havingValue = "true")
                public Engine engine() {
                    return new Engine();
                }
            }
            """
        )
        assertMethodVerdictInactive("com.explyt.demo.AliasConfig", "engine", "com.explyt.demo.AliasConfig#engine")

        val root = call(beanName = "spareEngine")

        assertEquals("NONE", root["outcome"].asText())
        val inactive = singleInactiveCandidateOf(root)
        assertEquals("mainEngine", inactive["name"].asText())
        assertEquals("spareEngine", inactive["matchedName"].asText())
        assertEquals("BEAN_METHOD", inactive["kind"].asText())
    }

    fun testFactoryMethodOfAnInactiveConfigurationNamesTheConfigurationAsCarrier() = runBlocking {
        addJava(
            "EtlJob",
            """
            package com.explyt.demo;

            public class EtlJob {}
            """
        )
        addJava(
            "EtlConfig",
            """
            package com.explyt.demo;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = "etl.enabled", havingValue = "true")
            public class EtlConfig {
                @Bean
                public EtlJob etlJob() {
                    return new EtlJob();
                }
            }
            """
        )
        assertMethodVerdictInactive("com.explyt.demo.EtlConfig", "etlJob", "com.explyt.demo.EtlConfig")

        val root = call(beanName = "etlJob")

        assertEquals("NONE", root["outcome"].asText())
        val inactive = singleInactiveCandidateOf(root)
        assertEquals("etlJob", inactive["name"].asText())
        assertEquals("BEAN_METHOD", inactive["kind"].asText())
        val condition = conditionOf(inactive)
        assertEquals("INACTIVE", condition["state"].asText())
        assertEquals("com.explyt.demo.EtlConfig", condition["carrier"].asText())
        assertTrue("detail must name the property, got $condition", "etl.enabled" in condition["detail"].asText())
    }

    fun testProfileInactiveBeanIsFilteredBeforeTheVerdictAndStaysUnlisted() = runBlocking {
        addProperties("spring.profiles.active=prod")
        addJava(
            "DevOnlyService",
            """
            package com.explyt.demo;

            import org.springframework.context.annotation.Profile;
            import org.springframework.stereotype.Component;

            @Component
            @Profile("dev")
            public class DevOnlyService {}
            """
        )
        val verdict = verdictOf("com.explyt.demo.DevOnlyService")
        assertNull("Precondition: a profile-inactive bean never reaches the verdicts, got $verdict", verdict)

        val root = call(beanName = "devOnlyService")

        assertEquals("NONE", root["outcome"].asText())
        assertEquals("COMPLETE", root["matchCompleteness"].asText())
        assertTrue("profile-inactive beans are not listed yet, got $root", inactiveCandidatesOf(root).isEmpty())
    }

    fun testManyInactiveCandidatesAreSampledWithinTheDefaultBudget() = runBlocking {
        addWorkers(active = 0, inactive = INACTIVE_WORKERS)

        val json = toolset().findSpringBean(projectPath = project.basePath!!, typeFqn = WORKER)
        val root = mapper.readTree(json)

        assertEquals("answer was $json", "OK", root["status"].asText())
        assertTrue("payload was ${json.length} chars", json.length <= MAX_DEFAULT_PAYLOAD)
        assertEquals("NONE", root["outcome"].asText())
        assertEquals("inactiveCount must count every match, got $json", INACTIVE_WORKERS, root["inactiveCount"]?.asInt())
        assertTrue("sample size must be 1..3, got $json", inactiveCandidatesOf(root).size in 1..MAX_INACTIVE_SAMPLE)
        assertTrue("a short sample is flagged, got $json", root["inactiveCandidatesTruncated"]?.asBoolean() == true)
    }

    fun testASingleInactiveCandidateIsCountedAndNotFlaggedTruncated() = runBlocking {
        addProperties("admin.marker=present")
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Inactive>(SYNC_ADMIN_CONTROLLER)

        val root = call(beanName = "syncAdminController")

        assertEquals("inactiveCount expected, got $root", 1, root["inactiveCount"]?.asInt())
        assertEquals("syncAdminController", singleInactiveCandidateOf(root)["name"].asText())
        assertFalse("a complete sample carries no truncation key, got $root", root.has("inactiveCandidatesTruncated"))
    }

    fun testAContinuationPageCountsInactiveCandidatesWithoutRepeatingThem() = runBlocking {
        addWorkers(active = DEFAULT_LIMIT + 1, inactive = 1)

        val first = call(typeFqn = WORKER)
        assertEquals("Precondition: the first page is truncated, got $first", true, first["truncated"]?.asBoolean())
        assertEquals("page 1 counts the inactive bean, got $first", 1, first["inactiveCount"]?.asInt())
        assertEquals("page 1 samples the inactive bean", 1, inactiveCandidatesOf(first).size)

        val second = call(
            typeFqn = WORKER,
            offset = first["nextOffset"].asInt(),
            expectedRevision = first["revision"].asText()
        )

        assertEquals("OK", second["status"].asText())
        assertFalse("Precondition: page 2 serves active candidates, got $second", second["candidates"].isEmpty)
        assertEquals("page 2 keeps the count, got $second", 1, second["inactiveCount"]?.asInt())
        assertFalse("page 2 does not repeat the sample, got $second", second.has("inactiveCandidates"))
    }

    fun testManyInactiveCandidatesDoNotCrowdOutActiveOnes() = runBlocking {
        addWorkers(active = 3, inactive = INACTIVE_WORKERS)

        val json = toolset().findSpringBean(projectPath = project.basePath!!, typeFqn = WORKER)
        val root = mapper.readTree(json)

        assertEquals("answer was $json", "OK", root["status"].asText())
        assertTrue("payload was ${json.length} chars", json.length <= MAX_DEFAULT_PAYLOAD)
        assertEquals("MULTIPLE", root["outcome"].asText())
        assertFalse("active candidates must be served, got $json", root["candidates"].isEmpty)
        assertEquals(INACTIVE_WORKERS, root["inactiveCount"]?.asInt())
    }

    fun testANonConstantQualifierEmitsNoUnfilteredInactiveCandidates() = runBlocking {
        val root = qualifiedInjection("EngineNames.FAST", nonConstantNames = true)

        assertTrue(
            "Precondition: the qualifier is reported as non-constant, got $root",
            ScopedBeanInjectionResolver.QUALIFIER_NOT_CONSTANT in limitationsOf(root)
        )
        assertEquals("INDETERMINATE", root["outcome"].asText())
        assertFalse("no unfiltered inactive sample, got $root", root.has("inactiveCandidates"))
        assertFalse("no unfiltered inactive count, got $root", root.has("inactiveCount"))
    }

    fun testANativeAnswerWithTheFilterDisabledClaimsNoConditionEvaluation() = runBlocking {
        val nativeFixture = NativeBeanListingFixture(project)
        val settings = SpringToolRunConfigurationsSettingsState.getInstance()
        val enabled = settings.isBeanFilterEnabled
        settings.isBeanFilterEnabled = false
        try {
            nativeFixture.install(myFixture.findClass(APPLICATION), listOf("nativeClock" to "java.time.Clock"))
            assertEquals(
                "Precondition: the native root must be loaded",
                1, NativeBeanSnapshotReader(project).contexts().size
            )

            val root = mapper.readTree(
                toolset().findSpringBean(
                    projectPath = project.basePath!!,
                    applicationClassName = APPLICATION,
                    source = "NATIVE",
                    beanName = "nativeClock"
                )
            )

            assertEquals("answer was $root", "OK", root["status"].asText())
            assertEquals("Precondition: the native model answers", "NATIVE_SNAPSHOT", root["model"]["source"].asText())
            assertEquals("SINGLE", root["outcome"].asText())
            assertFalse("limitations were ${limitationsOf(root)}", CONDITIONS_NOT_EVALUATED in limitationsOf(root))
            assertFalse(root["candidates"].single().has("condition"))
        } finally {
            nativeFixture.clear()
            settings.isBeanFilterEnabled = enabled
            ModificationTrackerManager.getInstance(project).invalidateAll()
        }
    }

    private fun addWorkers(active: Int, inactive: Int) {
        addJava(
            "Worker",
            """
            package com.explyt.demo;

            public interface Worker {}
            """
        )
        val activeWorkers = (1..active).map { "ActiveWorker$it" }
        activeWorkers.forEach { name ->
            addJava(
                name,
                """
                package com.explyt.demo;

                import org.springframework.stereotype.Component;

                @Component
                public class $name implements Worker {}
                """
            )
        }
        val inactiveWorkers = (1..inactive).map { "ConditionalWorker$it" }
        inactiveWorkers.forEach { name ->
            addJava(
                name,
                """
                package com.explyt.demo;

                import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
                import org.springframework.stereotype.Component;

                @Component
                @ConditionalOnProperty(name = "worker.$name.enabled", havingValue = "true")
                public class $name implements Worker {}
                """
            )
        }
        activeWorkers.forEach { assertVerdict<ConditionVerdict.Active>("com.explyt.demo.$it") }
        inactiveWorkers.forEach { assertVerdict<ConditionVerdict.Inactive>("com.explyt.demo.$it") }
    }

    private suspend fun qualifiedInjection(qualifier: String, nonConstantNames: Boolean = false): JsonNode {
        if (nonConstantNames) {
            addJava(
                "EngineNames",
                """
                package com.explyt.demo;

                public class EngineNames {
                    public static String FAST = "fast";
                }
                """
            )
        }
        val qualifierText = if (nonConstantNames) qualifier else "\"$qualifier\""
        addJava(
            "Engine",
            """
            package com.explyt.demo;

            public interface Engine {}
            """
        )
        addJava(
            "FastEngine",
            """
            package com.explyt.demo;

            import org.springframework.beans.factory.annotation.Qualifier;
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.stereotype.Component;

            @Component
            @Qualifier("fast")
            @ConditionalOnProperty(name = "engine.fast.enabled", havingValue = "true")
            public class FastEngine implements Engine {}
            """
        )
        val client = addJava(
            "EngineClient",
            """
            package com.explyt.demo;

            import org.springframework.beans.factory.annotation.Qualifier;
            import org.springframework.stereotype.Component;

            @Component
            public class EngineClient {
                public EngineClient(@Qualifier($qualifierText) Engine engine) {}
            }
            """
        )
        assertVerdict<ConditionVerdict.Inactive>("com.explyt.demo.FastEngine")
        val marker = "Engine engine"
        val (line, column) = positionOf(client, marker, "Engine ".length)
        return call(filePath = "com/explyt/demo/EngineClient.java", line = line, column = column)
    }

    private fun addMyCondition() = addJava(
        "MyCondition",
        """
        package com.explyt.demo;

        import org.springframework.context.annotation.Condition;
        import org.springframework.context.annotation.ConditionContext;
        import org.springframework.core.type.AnnotatedTypeMetadata;

        public class MyCondition implements Condition {
            @Override
            public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
                return false;
            }
        }
        """
    )

    private fun addMissingBeanService() {
        addJava(
            "Missing",
            """
            package com.explyt.demo;

            public class Missing {}
            """
        )
        addJava(
            "NeedsMissing",
            """
            package com.explyt.demo;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
            import org.springframework.stereotype.Component;

            @Component
            @ConditionalOnBean(Missing.class)
            public class NeedsMissing {}
            """
        )
    }

    private fun assertMethodVerdictInactive(classFqn: String, methodName: String, carrier: String) {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val method = myFixture.findClass(classFqn).findMethodsByName(methodName, false).single()
        val verdict = SpringSearchService.getInstance(project).conditionVerdictOf(method, module)
        assertTrue("Precondition: $classFqn#$methodName must be Inactive, got $verdict", verdict is ConditionVerdict.Inactive)
        assertEquals("Precondition: carrier", carrier, (verdict as ConditionVerdict.Inactive).condition.carrierFqn)
    }

    private suspend fun call(
        typeFqn: String? = null,
        beanName: String? = null,
        filePath: String? = null,
        line: Int? = null,
        column: Int? = null,
        includeDetails: Boolean = false,
        offset: Int = 0,
        expectedRevision: String? = null
    ): JsonNode = mapper.readTree(
        toolset().findSpringBean(
            projectPath = project.basePath!!,
            applicationClassName = APPLICATION,
            source = "STATIC",
            typeFqn = typeFqn,
            beanName = beanName,
            filePath = filePath,
            line = line,
            column = column,
            includeDetails = includeDetails,
            offset = offset,
            expectedRevision = expectedRevision
        )
    )

    private fun toolset(): SpringBeanMcpToolset {
        val toolsets = McpToolset.EP.extensionList.filterIsInstance<SpringBeanMcpToolset>()
        assertEquals("Precondition: the toolset must be registered", 1, toolsets.size)
        return toolsets.single()
    }

    private fun addSyncAdminController() = addJava(
        "SyncAdminController",
        """
        package com.explyt.demo;

        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
        import org.springframework.stereotype.Controller;

        @Controller
        @ConditionalOnProperty(name = "admin.sync.enabled", havingValue = "true")
        public class SyncAdminController {}
        """
    )

    private fun addFooBeans() {
        addJava(
            "Foo",
            """
            package com.explyt.demo;

            public interface Foo {}
            """
        )
        addJava(
            "ActiveFoo",
            """
            package com.explyt.demo;

            import org.springframework.stereotype.Component;

            @Component
            public class ActiveFoo implements Foo {}
            """
        )
        addJava(
            "InactiveFoo",
            """
            package com.explyt.demo;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.stereotype.Component;

            @Component
            @ConditionalOnProperty(name = "foo.inactive.enabled", havingValue = "true")
            public class InactiveFoo implements Foo {}
            """
        )
    }

    private fun addJava(className: String, source: String): PsiFile {
        val file = myFixture.addFileToProject("com/explyt/demo/$className.java", source.trimIndent())
        settle()
        return file
    }

    private fun addProperties(vararg lines: String) {
        myFixture.addFileToProject("application.properties", lines.joinToString("\n"))
        settle()
    }

    private fun settle() {
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private fun verdictOf(classFqn: String): ConditionVerdict? {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return SpringSearchService.getInstance(project).conditionVerdictOf(myFixture.findClass(classFqn), module)
    }

    private inline fun <reified T : ConditionVerdict> assertVerdict(classFqn: String) {
        val verdict = verdictOf(classFqn)
        assertTrue("Precondition: $classFqn must be ${T::class.simpleName}, got $verdict", verdict is T)
    }

    private fun assertUndecidedBecause(classFqn: String, reason: ConditionReason) {
        val verdict = verdictOf(classFqn)
        assertTrue("Precondition: $classFqn must be Undecided, got $verdict", verdict is ConditionVerdict.Undecided)
        assertEquals(
            "Precondition: $classFqn undecided reason",
            listOf(reason),
            (verdict as ConditionVerdict.Undecided).conditions.map { it.reason }
        )
    }

    private fun inactiveCandidatesOf(root: JsonNode): List<JsonNode> =
        root["inactiveCandidates"]?.toList().orEmpty()

    private fun singleInactiveCandidateOf(root: JsonNode): JsonNode {
        val inactive = inactiveCandidatesOf(root)
        assertEquals("one inactive candidate expected, got $root", 1, inactive.size)
        return inactive.single()
    }

    private fun conditionOf(candidate: JsonNode): JsonNode {
        val condition = candidate["condition"]
        assertNotNull("candidate must carry a condition, got $candidate", condition)
        return condition
    }

    private fun reasonsOf(condition: JsonNode): List<String> =
        condition["reasons"]?.map { it.asText() }.orEmpty()

    private fun limitationsOf(root: JsonNode): Set<String> =
        root["model"]?.get("limitations")?.mapTo(sortedSetOf()) { it.asText() }.orEmpty()

    private fun positionOf(file: PsiFile, marker: String, offsetInMarker: Int): Pair<Int, Int> {
        val offset = file.text.indexOf(marker)
        assertTrue("Precondition: marker '$marker' must exist in the fixture", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        val target = offset + offsetInMarker
        val line = document.getLineNumber(target) + 1
        return line to (target - document.getLineStartOffset(line - 1) + 1)
    }

    private companion object {
        const val APPLICATION = "com.explyt.demo.App"
        const val SYNC_ADMIN_CONTROLLER = "com.explyt.demo.SyncAdminController"
        const val CUSTOM_SERVICE = "com.explyt.demo.CustomService"
        const val PROFILED_SERVICE = "com.explyt.demo.ProfiledService"
        const val FOO = "com.explyt.demo.Foo"
        const val ACTIVE_FOO = "com.explyt.demo.ActiveFoo"
        const val INACTIVE_FOO = "com.explyt.demo.InactiveFoo"
        const val CONDITIONAL_ON_PROPERTY = "org.springframework.boot.autoconfigure.condition.ConditionalOnProperty"
        const val CONDITIONAL = "org.springframework.context.annotation.Conditional"
        const val CONDITIONAL_ON_BEAN = "org.springframework.boot.autoconfigure.condition.ConditionalOnBean"
        const val NEEDS_MISSING = "com.explyt.demo.NeedsMissing"
        const val INACTIVE_WORKERS = 15
        const val MAX_INACTIVE_SAMPLE = 3
        const val DEFAULT_LIMIT = 5
        const val WORKER = "com.explyt.demo.Worker"
        const val CONDITIONS_UNDECIDED = "CONDITIONS_UNDECIDED"
        const val PROFILE_NOT_DECIDABLE = "PROFILE_NOT_DECIDABLE"
        const val CONDITIONS_NOT_EVALUATED = "CONDITIONS_NOT_EVALUATED"
        const val MAX_DEFAULT_PAYLOAD = 1800
    }
}
