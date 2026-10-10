/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.psi.PsiMember

abstract class BeanConditionVerdictTestCase : BeanConditionTestCase() {

    fun testUnconditionalComponentIsActive() {
        fixtures.addPlainComponent()
        assertTrue("Precondition: PlainService is an active bean", "com.app.PlainService" in activeBeans())

        assertEquals(ConditionVerdict.Active, verdictOf(beanClass("com.app.PlainService")))
    }

    fun testPropertyNotMatchingHavingValueIsInactiveWithNamedEvidence() {
        addProperties("feature.sync=false")
        fixtures.addPropertyGatedConfiguration("SyncConfig")
        val config = beanClass("com.app.SyncConfig")
        assertPropertyDefined("feature.sync")
        assertAnnotatedBy(config, CONDITIONAL_ON_PROPERTY)

        val evidence = inactiveEvidence(verdictOf(config))

        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals("com.app.SyncConfig", evidence.carrierFqn)
        assertEquals(ConditionReason.NOT_MATCHED, evidence.reason)
        assertDetailMentions(evidence, "feature.sync", "false")
        assertTrue("com.app.SyncConfig" in excludedBeans())
    }

    fun testUnresolvablePlaceholderIsUndecidedAndStaysActive() {
        addProperties("feature.sync=\${SYNC_ENABLED}")
        fixtures.addPropertyGatedConfiguration("SyncConfig")
        val config = beanClass("com.app.SyncConfig")
        assertPropertyDefined("feature.sync")
        assertPropertyUndefined("SYNC_ENABLED")
        assertAnnotatedBy(config, CONDITIONAL_ON_PROPERTY)

        val evidence = undecidedEvidence(verdictOf(config)).single()

        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals(ConditionReason.PROPERTY_UNRESOLVABLE, evidence.reason)
        assertDetailMentions(evidence, "feature.sync")
        assertTrue("com.app.SyncConfig" in activeBeans())
    }

    fun testCustomConditionIsUndecidedAndStaysActive() {
        fixtures.addCustomConditionConfiguration()
        val config = beanClass("com.app.CustomConditionConfig")
        assertAnnotatedBy(config, CONDITIONAL)

        val evidence = undecidedEvidence(verdictOf(config)).single()

        assertEquals(CONDITIONAL, evidence.annotationFqn)
        assertEquals(ConditionReason.UNSUPPORTED_CONDITION, evidence.reason)
        assertTrue("com.app.CustomConditionConfig" in activeBeans())
    }

    fun testUnresolvableProfileIsUndecidedAndStaysActive() {
        fixtures.addUnknownProfileConfiguration()
        val config = beanClass("com.app.UnknownProfileConfig")
        assertProfileValueUnresolved(config)

        val evidence = undecidedEvidence(verdictOf(config)).single()

        assertEquals(SpringCoreClasses.PROFILE, evidence.annotationFqn)
        assertEquals(ConditionReason.PROFILE_NOT_DECIDABLE, evidence.reason)
        assertTrue("com.app.UnknownProfileConfig" in activeBeans())
    }

    fun testMissingBeanConditionIsInactiveUnderStaticModelAssumption() {
        fixtures.addMissingBeanConfiguration()
        val config = beanClass("com.app.MissingBeanConfig")
        assertAnnotatedBy(config, CONDITIONAL_ON_BEAN)

        val evidence = inactiveEvidence(verdictOf(config))

        assertEquals(CONDITIONAL_ON_BEAN, evidence.annotationFqn)
        assertEquals(ConditionReason.NOT_MATCHED, evidence.reason)
        assertEquals(setOf(ConditionAssumption.STATIC_BEAN_MODEL_COMPLETE), evidence.assumptions)
        assertTrue("com.app.MissingBeanConfig" in excludedBeans())
    }

    fun testInactiveOuterConfigurationDecidesItsBeanMethod() {
        addProperties("outer.enabled=false")
        fixtures.addConditionalOuterConfiguration()
        val method = beanMethod("com.app.OuterConfig", "outerService")
        assertPropertyDefined("outer.enabled")
        assertAnnotatedBy(beanClass("com.app.OuterConfig"), CONDITIONAL_ON_PROPERTY)
        assertFalse(
            "Precondition: the bean method carries no condition of its own",
            method.modifierList.hasAnnotation(CONDITIONAL_ON_PROPERTY)
        )

        val evidence = inactiveEvidence(verdictOf(method))

        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals("com.app.OuterConfig", evidence.carrierFqn)
        assertDetailMentions(evidence, "outer.enabled", "false")
    }

    fun testInactiveConditionWinsOverUndecidedOne() {
        addProperties("mixed.enabled=false")
        fixtures.addMixedConditionsConfiguration()
        val config = beanClass("com.app.MixedConditionsConfig")
        assertPropertyDefined("mixed.enabled")
        assertAnnotatedBy(config, CONDITIONAL)
        assertAnnotatedBy(config, CONDITIONAL_ON_PROPERTY)

        val evidence = inactiveEvidence(verdictOf(config))

        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertDetailMentions(evidence, "mixed.enabled")
    }

    fun testPropertyGatedControllerWithoutPropertyIsInactiveWithEvidence() {
        addProperties("admin.marker=present")
        fixtures.addSyncAdminController()
        val controller = beanClass("com.app.SyncAdminController")
        assertPropertyDefined("admin.marker")
        assertPropertyUndefined("admin.sync.enabled")
        assertAnnotatedBy(controller, CONDITIONAL_ON_PROPERTY)

        val evidence = inactiveEvidence(verdictOf(controller))

        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals("com.app.SyncAdminController", evidence.carrierFqn)
        assertEquals(ConditionReason.NOT_MATCHED, evidence.reason)
        assertDetailMentions(evidence, "admin.sync.enabled")
        assertTrue("com.app.SyncAdminController" in excludedBeans())
    }

    private fun verdictOf(member: PsiMember): ConditionVerdict? {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return SpringSearchService.getInstance(project).conditionVerdictOf(member, module)
    }

    private fun inactiveEvidence(verdict: ConditionVerdict?): ConditionEvidence {
        assertInstanceOf(verdict, ConditionVerdict.Inactive::class.java)
        return (verdict as ConditionVerdict.Inactive).condition
    }

    private fun undecidedEvidence(verdict: ConditionVerdict?): List<ConditionEvidence> {
        assertInstanceOf(verdict, ConditionVerdict.Undecided::class.java)
        return (verdict as ConditionVerdict.Undecided).conditions
    }

    private fun assertDetailMentions(evidence: ConditionEvidence, vararg fragments: String) {
        val detail = evidence.detail
        assertNotNull("Evidence must carry a detail", detail)
        for (fragment in fragments) {
            assertTrue("Detail '$detail' must mention '$fragment'", fragment in detail!!)
        }
    }
}
