/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.ai.mcp.BoundedPageWriter
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.service.ProfileActivation
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.beans.NativeBeanSnapshotReader
import com.explyt.spring.core.service.beans.ScopedBeanInjectionResolver
import com.explyt.spring.core.service.conditional.ConditionAssumption
import com.explyt.spring.core.service.conditional.ConditionReason
import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.mcpserver.McpToolset
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.IndexingTestUtil

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

    fun testPropertyGatedControllerWithoutPropertyIsReportedAsInactiveCandidate() = runBlockingReleasingWriteIntent {
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

    fun testPropertyGatedControllerWithPropertyIsAnUnconditionedCandidate() = runBlockingReleasingWriteIntent {
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

    fun testUnresolvablePlaceholderMakesTheCandidateUndecided() = runBlockingReleasingWriteIntent {
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

    fun testCustomConditionMakesTheCandidateUndecided() = runBlockingReleasingWriteIntent {
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

    fun testUnresolvableProfileMakesTheCandidateUndecidedAndNamesTheProfileLimitation() = runBlockingReleasingWriteIntent {
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

    fun testTypeLookupKeepsTheActiveBeanAndListsTheInactiveOneSeparately() = runBlockingReleasingWriteIntent {
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

    fun testAnUndecidedBeanOfAnotherTypeDoesNotMakeTheQueryPartial() = runBlockingReleasingWriteIntent {
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

    fun testInjectionPointWhoseOnlyCandidateIsInactiveListsIt() = runBlockingReleasingWriteIntent {
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

    fun testDisabledConditionFilterIsNamedAsALimitation() = runBlockingReleasingWriteIntent {
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

    fun testTogglingConditionEvaluationChangesTheRevision() = runBlockingReleasingWriteIntent {
        addWorkers(active = 2, inactive = 1)
        val settings = SpringToolRunConfigurationsSettingsState.getInstance()
        val enabled = settings.isBeanFilterEnabled
        assertTrue("Precondition: condition evaluation starts enabled", enabled)
        val first = call(typeFqn = WORKER, limit = 1)
        assertEquals("Precondition: the first page is served, got $first", "OK", first["status"].asText())
        assertTrue("Precondition: the first page is truncated, got $first", first["truncated"].asBoolean())
        assertFalse("Precondition: page 1 is read with conditions evaluated, got $first", CONDITIONS_NOT_EVALUATED in limitationsOf(first))

        settings.isBeanFilterEnabled = false
        try {
            val fresh = call(typeFqn = WORKER, limit = 1)
            assertTrue("Precondition: the toggle changes the answer, got $fresh", CONDITIONS_NOT_EVALUATED in limitationsOf(fresh))

            val stale = call(
                typeFqn = WORKER,
                limit = 1,
                offset = first["nextOffset"].asInt(),
                expectedRevision = first["revision"].asText()
            )

            assertEquals("a continuation across the toggle must restart, got $stale", "ERROR", stale["status"].asText())
            assertEquals("RESULT_CHANGED", stale["error"]["code"].asText())
        } finally {
            settings.isBeanFilterEnabled = enabled
            ModificationTrackerManager.getInstance(project).invalidateAll()
        }
    }

    fun testTheDefaultAnswerWithAnInactiveCandidateFitsTheClientBudget() = runBlockingReleasingWriteIntent {
        addProperties("admin.marker=present")
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Inactive>(SYNC_ADMIN_CONTROLLER)

        val json = toolset().findSpringBean(projectPath = project.basePath!!, beanName = "syncAdminController")
        val root = mapper.readTree(json)

        assertEquals("OK", root["status"].asText())
        assertEquals("Precondition: the inactive candidate must be in the answer", 1, inactiveCandidatesOf(root).size)
        assertTrue("payload was ${json.length} chars", json.length <= MAX_DEFAULT_PAYLOAD)
    }

    fun testTwoUndecidedConditionsOnOneBeanAreBothReported() = runBlockingReleasingWriteIntent {
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

    fun testDetailsAddIdAndAssumptionsToAnInactiveCandidate() = runBlockingReleasingWriteIntent {
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

    fun testWithoutDetailsAnInactiveCandidateHasIdButNoAssumptions() = runBlockingReleasingWriteIntent {
        addMissingBeanService()
        assertVerdict<ConditionVerdict.Inactive>(NEEDS_MISSING)

        val inactive = singleInactiveCandidateOf(call(beanName = "needsMissing"))

        assertTrue("an inactive candidate carries its id like an active one, got $inactive", inactive["id"]?.asText().orEmpty().isNotBlank())
        assertFalse("compact answer carries no assumptions, got $inactive", conditionOf(inactive).has("assumptions"))
    }

    fun testInjectionWithAMatchingQualifierListsTheInactiveBean() = runBlockingReleasingWriteIntent {
        val root = qualifiedInjection("fast")

        assertEquals("NO_CANDIDATE", root["outcome"].asText())
        val inactive = singleInactiveCandidateOf(root)
        assertEquals("fastEngine", inactive["name"].asText())
        assertEquals("INACTIVE", conditionOf(inactive)["state"].asText())
    }

    fun testInjectionWithAnotherQualifierDoesNotListTheInactiveBean() = runBlockingReleasingWriteIntent {
        val root = qualifiedInjection("slow")

        assertEquals("NO_CANDIDATE", root["outcome"].asText())
        assertTrue("no inactive candidate expected, got $root", inactiveCandidatesOf(root).isEmpty())
    }

    fun testLookupByAnAliasOfAnInactiveBeanNamesTheMatchedAlias() = runBlockingReleasingWriteIntent {
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

    fun testFactoryMethodOfAnInactiveConfigurationNamesTheConfigurationAsCarrier() = runBlockingReleasingWriteIntent {
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

    fun testProfileInactiveBeanIsAnInactiveCandidateNamingTheProfile() = runBlockingReleasingWriteIntent {
        addProperties("spring.profiles.active=prod")
        addDevOnlyService()

        val root = call(beanName = "devOnlyService")

        assertEquals("NONE", root["outcome"].asText())
        assertEquals(0, root["totalCount"].asInt())
        assertProfileInactiveCandidate(singleInactiveCandidateOf(root))
    }

    fun testTypeLookupListsTheProfileInactiveBeanAsAnInactiveCandidate() = runBlockingReleasingWriteIntent {
        addProperties("spring.profiles.active=prod")
        addDevOnlyService()

        val root = call(typeFqn = DEV_ONLY_SERVICE)

        assertEquals("NONE", root["outcome"].asText())
        assertEquals(0, root["totalCount"].asInt())
        assertProfileInactiveCandidate(singleInactiveCandidateOf(root))
    }

    private fun addDevOnlyService() {
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
        val service = myFixture.findClass(DEV_ONLY_SERVICE)
        assertNotNull("Precondition: @Profile resolves to the Spring annotation", service.getAnnotation(PROFILE))
        assertEquals(
            "Precondition: DevOnlyService is inactive by its profile",
            ProfileActivation.INACTIVE,
            SpringSearchService.getInstance(project).profileActivation(service)
        )
    }

    private fun assertProfileInactiveCandidate(inactive: JsonNode) {
        assertEquals("devOnlyService", inactive["name"].asText())
        assertEquals(DEV_ONLY_SERVICE, inactive["type"].asText())
        val condition = conditionOf(inactive)
        assertEquals("INACTIVE", condition["state"].asText())
        assertEquals(PROFILE, condition["annotation"].asText())
        assertEquals(DEV_ONLY_SERVICE, condition["carrier"].asText())
        assertTrue("detail must name the profile, got $condition", "dev" in condition["detail"].asText())
    }

    fun testManyInactiveCandidatesAreSampledWithinTheDefaultBudget() = runBlockingReleasingWriteIntent {
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

    fun testASingleInactiveCandidateIsCountedAndNotFlaggedTruncated() = runBlockingReleasingWriteIntent {
        addProperties("admin.marker=present")
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Inactive>(SYNC_ADMIN_CONTROLLER)

        val root = call(beanName = "syncAdminController")

        assertEquals("inactiveCount expected, got $root", 1, root["inactiveCount"]?.asInt())
        assertEquals("syncAdminController", singleInactiveCandidateOf(root)["name"].asText())
        assertFalse("a complete sample carries no truncation key, got $root", root.has("inactiveCandidatesTruncated"))
    }

    fun testAContinuationPageCountsInactiveCandidatesWithoutRepeatingThem() = runBlockingReleasingWriteIntent {
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

    fun testManyInactiveCandidatesDoNotCrowdOutActiveOnes() = runBlockingReleasingWriteIntent {
        addWorkers(active = 3, inactive = INACTIVE_WORKERS)

        val json = toolset().findSpringBean(projectPath = project.basePath!!, typeFqn = WORKER)
        val root = mapper.readTree(json)

        assertEquals("answer was $json", "OK", root["status"].asText())
        assertTrue("payload was ${json.length} chars", json.length <= MAX_DEFAULT_PAYLOAD)
        assertEquals("MULTIPLE", root["outcome"].asText())
        assertFalse("active candidates must be served, got $json", root["candidates"].isEmpty)
        assertEquals(INACTIVE_WORKERS, root["inactiveCount"]?.asInt())
    }

    fun testASmallBudgetServesAnActiveCandidateBeforeTheInactiveSample() = runBlockingReleasingWriteIntent {
        addWorkers(active = 2, inactive = MAX_INACTIVE_SAMPLE + 1)
        val roomy = call(typeFqn = WORKER, limit = 1, maxChars = BoundedPageWriter.MAX_CHARS) as ObjectNode
        assertEquals("Precondition: the roomy page is served, got $roomy", "OK", roomy["status"].asText())
        assertEquals("Precondition: the roomy page holds one active candidate, got $roomy", 1, roomy["candidates"].size())
        val sample = inactiveCandidatesOf(roomy)
        assertEquals("Precondition: the roomy page samples inactive candidates, got $roomy", MAX_INACTIVE_SAMPLE, sample.size)
        assertEquals("Precondition: the roomy sample is flagged as cut, got $roomy", true, roomy["inactiveCandidatesTruncated"]?.asBoolean())
        val sampleFree = roomy.deepCopy().apply { remove("inactiveCandidates") }
        val sampleFreeLength = mapper.writeValueAsString(sampleFree).length
        val firstSampledLength = mapper.writeValueAsString(sample.first()).length
        val budget = maxOf(sampleFreeLength, 2 * (firstSampledLength + 2), BoundedPageWriter.MIN_CHARS)
        val oneSampled = sampleFree.deepCopy().apply { remove("inactiveCandidatesTruncated"); putArray("inactiveCandidates").add(sample.first()) }
        assertTrue(
            "Precondition: no inactive candidate fits beside the first active one within $budget chars",
            mapper.writeValueAsString(oneSampled).length > budget
        )

        val json = callJson(typeFqn = WORKER, maxChars = budget)

        val root = mapper.readTree(json)
        assertEquals("answer was $json", "OK", root["status"].asText())
        assertTrue("payload was ${json.length} chars within $budget", json.length <= budget)
        assertFalse("an active candidate must be served, got $json", root["candidates"].isEmpty)
        assertEquals(MAX_INACTIVE_SAMPLE + 1, root["inactiveCount"]?.asInt())
        assertEquals("a cut sample is flagged, got $json", true, root["inactiveCandidatesTruncated"]?.asBoolean())
    }

    fun testTheDefaultBudgetSampleDisplacesOnlyTheActiveCandidatesItsOwnLengthAccountsFor() = runBlockingReleasingWriteIntent {
        addWorkers(active = DEFAULT_LIMIT + 1, inactive = MAX_INACTIVE_SAMPLE)
        addWorkers(active = DEFAULT_LIMIT + 1, inactive = 0, role = HELPER_ROLE)
        val unsampled = call(typeFqn = HELPER)
        assertEquals("Precondition: the baseline is served, got $unsampled", "OK", unsampled["status"].asText())
        assertFalse("Precondition: the baseline has no inactive candidates, got $unsampled", unsampled.has("inactiveCount"))
        assertTrue("Precondition: the baseline page is cut, got $unsampled", unsampled["truncated"].asBoolean())

        val json = callJson(typeFqn = WORKER)

        val sampled = mapper.readTree(json)
        assertEquals("answer was $json", "OK", sampled["status"].asText())
        assertEquals("Precondition: every inactive worker is counted, got $json", MAX_INACTIVE_SAMPLE, sampled["inactiveCount"]?.asInt())
        val sample = sampled["inactiveCandidates"]
        assertNotNull("Precondition: the default page samples inactive candidates, got $json", sample)
        val sampleCost = envelopeLengthOf(sampled) - envelopeLengthOf(unsampled)
        val baseline = unsampled["candidates"].size()
        val served = sampled["candidates"].size()
        val displaced = baseline - served
        val shortest = unsampled["candidates"].minOf { mapper.writeValueAsString(it).length + 1 }
        val sampleLength = mapper.writeValueAsString(sample).length
        val measurement = "a sample of ${sample.size()} ($sampleLength chars) costing $sampleCost chars moved $displaced of " +
                "$baseline active candidates to later pages (shortest candidate $shortest chars, page ${json.length} " +
                "chars, budget $MAX_DEFAULT_PAYLOAD)"
        println("#634 measurement: $measurement")
        assertTrue(measurement, served >= 1)
        assertTrue(measurement, sample.size() <= MAX_INACTIVE_SAMPLE)
        assertTrue(measurement, sampleLength <= MAX_DEFAULT_PAYLOAD / 2)
    }

    private fun envelopeLengthOf(page: JsonNode): Int =
        mapper.writeValueAsString((page as ObjectNode).deepCopy().apply { remove("candidates") }).length

    fun testANonConstantQualifierEmitsNoUnfilteredInactiveCandidates() = runBlockingReleasingWriteIntent {
        val root = qualifiedInjection("EngineNames.FAST", nonConstantNames = true)

        assertTrue(
            "Precondition: the qualifier is reported as non-constant, got $root",
            ScopedBeanInjectionResolver.QUALIFIER_NOT_CONSTANT in limitationsOf(root)
        )
        assertEquals("INDETERMINATE", root["outcome"].asText())
        assertFalse("no unfiltered inactive sample, got $root", root.has("inactiveCandidates"))
        assertFalse("no unfiltered inactive count, got $root", root.has("inactiveCount"))
    }

    fun testANativeAnswerWithTheFilterDisabledClaimsNoConditionEvaluation() = runBlockingReleasingWriteIntent {
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

    private fun addWorkers(active: Int, inactive: Int, role: String = WORKER_ROLE) {
        addJava(
            role,
            """
            package com.explyt.demo;

            public interface $role {}
            """
        )
        val activeWorkers = (1..active).map { "Active$role$it" }
        activeWorkers.forEach { name ->
            addJava(
                name,
                """
                package com.explyt.demo;

                import org.springframework.stereotype.Component;

                @Component
                public class $name implements $role {}
                """
            )
        }
        val inactiveWorkers = (1..inactive).map { "Conditional$role$it" }
        inactiveWorkers.forEach { name ->
            addJava(
                name,
                """
                package com.explyt.demo;

                import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
                import org.springframework.stereotype.Component;

                @Component
                @ConditionalOnProperty(name = "${role.lowercase()}.$name.enabled", havingValue = "true")
                public class $name implements $role {}
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
        expectedRevision: String? = null,
        limit: Int = DEFAULT_LIMIT,
        maxChars: Int = MAX_DEFAULT_PAYLOAD
    ): JsonNode = mapper.readTree(
        callJson(typeFqn, beanName, filePath, line, column, includeDetails, offset, expectedRevision, limit, maxChars)
    )

    private suspend fun callJson(
        typeFqn: String? = null,
        beanName: String? = null,
        filePath: String? = null,
        line: Int? = null,
        column: Int? = null,
        includeDetails: Boolean = false,
        offset: Int = 0,
        expectedRevision: String? = null,
        limit: Int = DEFAULT_LIMIT,
        maxChars: Int = MAX_DEFAULT_PAYLOAD
    ): String = toolset().findSpringBean(
        projectPath = project.basePath!!,
        applicationClassName = APPLICATION,
        source = "STATIC",
        typeFqn = typeFqn,
        beanName = beanName,
        filePath = filePath,
        line = line,
        column = column,
        offset = offset,
        limit = limit,
        maxChars = maxChars,
        expectedRevision = expectedRevision,
        includeDetails = includeDetails
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
        const val DEV_ONLY_SERVICE = "com.explyt.demo.DevOnlyService"
        const val PROFILE = "org.springframework.context.annotation.Profile"
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
        const val WORKER_ROLE = "Worker"
        const val WORKER = "com.explyt.demo.Worker"
        const val HELPER_ROLE = "Helper"
        const val HELPER = "com.explyt.demo.Helper"
        const val CONDITIONS_UNDECIDED = "CONDITIONS_UNDECIDED"
        const val PROFILE_NOT_DECIDABLE = "PROFILE_NOT_DECIDABLE"
        const val CONDITIONS_NOT_EVALUATED = "CONDITIONS_NOT_EVALUATED"
        const val MAX_DEFAULT_PAYLOAD = 1800
    }
}
