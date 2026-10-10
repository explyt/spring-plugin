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

    fun testSupportedConditionsDoNotBecomeUnsupported() {
        addProperties("feature.sync=true", "spring.profiles.active=dev")
        val config = addGapConfiguration(
            "SupportedConfig",
            "@ConditionalOnProperty(name = \"feature.sync\") @ConditionalOnClass(String.class) @Profile(\"dev\")",
            "@ConditionalOnProperty(name = [\"feature.sync\"]) @ConditionalOnClass(String::class) @Profile(\"dev\")"
        )
        assertPropertyDefined("feature.sync")
        assertPropertyDefined("spring.profiles.active")
        assertAnnotatedBy(config, CONDITIONAL_ON_PROPERTY)
        assertAnnotatedBy(config, "org.springframework.boot.autoconfigure.condition.ConditionalOnClass")
        assertAnnotatedBy(config, SpringCoreClasses.PROFILE)

        assertEquals(ConditionVerdict.Active, verdictOf(config))
    }

    fun testExpressionIsUnsupportedAndStaysActive() {
        val config = addGapConfiguration(
            "ExpressionConfig", "@ConditionalOnExpression(\"\${x}\")",
            "@ConditionalOnExpression(\"\${'$'}{x}\")"
        )
        val annotation = "org.springframework.boot.autoconfigure.condition.ConditionalOnExpression"
        assertAnnotatedBy(config, annotation)
        assertPropertyUndefined("x")

        val evidence = undecidedEvidence(verdictOf(config)).single()
        assertEquals(annotation, evidence.annotationFqn)
        assertEquals(ConditionReason.UNSUPPORTED_CONDITION, evidence.reason)
        assertTrue("com.app.ExpressionConfig" in activeBeans())
    }

    fun testPropertyMetaAnnotationNamesTheProjectAnnotation() {
        addProperties("feature.sync=false")
        addGapSource(
            "SyncEnabled",
            "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) @ConditionalOnProperty(name = \"feature.sync\", havingValue = \"true\") public @interface SyncEnabled {}",
            "@Retention(AnnotationRetention.RUNTIME) @ConditionalOnProperty(name = [\"feature.sync\"], havingValue = \"true\") annotation class SyncEnabled"
        )
        val config = addGapConfiguration("MetaConfig", "@SyncEnabled", "@SyncEnabled")
        assertPropertyDefined("feature.sync")
        assertAnnotatedBy(beanClass("com.app.SyncEnabled"), CONDITIONAL_ON_PROPERTY)
        assertAnnotatedBy(config, "com.app.SyncEnabled")

        val evidence = inactiveEvidence(verdictOf(config))
        assertEquals("com.app.SyncEnabled", evidence.annotationFqn)
        assertEquals(ConditionReason.NOT_MATCHED, evidence.reason)
        assertDetailMentions(evidence, "feature.sync", "false")
    }

    fun testMissingBeanSatisfiedIsActive() {
        val config = addGapConfiguration(
            "MissingSatisfiedConfig", "@ConditionalOnMissingBean(name = \"plainService\")",
            "@ConditionalOnMissingBean(name = [\"plainService\"])"
        )
        assertAnnotatedBy(config, "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean")
        assertFalse("Precondition: PlainService is absent", "com.app.PlainService" in activeBeans())

        assertEquals(ConditionVerdict.Active, verdictOf(config))
    }

    fun testMissingBeanViolatedHasStaticModelAssumption() {
        fixtures.addPlainComponent()
        val config = addGapConfiguration(
            "MissingViolatedConfig", "@ConditionalOnMissingBean(name = \"plainService\")",
            "@ConditionalOnMissingBean(name = [\"plainService\"])"
        )
        val annotation = "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean"
        assertAnnotatedBy(config, annotation)
        assertTrue("Precondition: PlainService is active", "com.app.PlainService" in activeBeans())

        val evidence = inactiveEvidence(verdictOf(config))
        assertEquals(annotation, evidence.annotationFqn)
        assertEquals(ConditionReason.NOT_MATCHED, evidence.reason)
        assertEquals(setOf(ConditionAssumption.STATIC_BEAN_MODEL_COMPLETE), evidence.assumptions)
    }


    fun testNestedBeanInheritsInactiveOuterCarrier() {
        addProperties("outer.enabled=false")
        addGapSource(
            "NestedOuterConfig",
            "@Configuration @ConditionalOnProperty(name = \"outer.enabled\", havingValue = \"true\") public class NestedOuterConfig { @Configuration public static class NestedConfig {} }",
            "@Configuration @ConditionalOnProperty(name = [\"outer.enabled\"], havingValue = \"true\") class NestedOuterConfig { @Configuration class NestedConfig }"
        )
        val outer = beanClass("com.app.NestedOuterConfig")
        val nested = outer.innerClasses.single()
        assertAnnotatedBy(outer, CONDITIONAL_ON_PROPERTY)
        assertAnnotatedBy(nested, "org.springframework.context.annotation.Configuration")
        assertPropertyDefined("outer.enabled")

        val evidence = inactiveEvidence(verdictOf(nested))
        assertEquals("com.app.NestedOuterConfig", evidence.carrierFqn)
        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
    }

    fun testTwoUndecidedConditionsKeepBothEvidencesInStrategyOrder() {
        addProperties("feature.sync=\${SYNC_ENABLED}")
        val config = addGapConfiguration(
            "TwoUndecidedConfig",
            "@ConditionalOnExpression(\"\${x}\") @ConditionalOnProperty(name = \"feature.sync\")",
            "@ConditionalOnExpression(\"\${'$'}{x}\") @ConditionalOnProperty(name = [\"feature.sync\"])"
        )
        val expression = "org.springframework.boot.autoconfigure.condition.ConditionalOnExpression"
        assertAnnotatedBy(config, expression)
        assertAnnotatedBy(config, CONDITIONAL_ON_PROPERTY)
        assertPropertyDefined("feature.sync")
        assertPropertyUndefined("SYNC_ENABLED")

        val evidences = undecidedEvidence(verdictOf(config))
        assertEquals(listOf(CONDITIONAL_ON_PROPERTY, expression), evidences.map { it.annotationFqn })
        assertEquals(listOf(ConditionReason.PROPERTY_UNRESOLVABLE, ConditionReason.UNSUPPORTED_CONDITION), evidences.map { it.reason })
        assertTrue("com.app.TwoUndecidedConfig" in activeBeans())
    }

    fun testNonBeanMemberHasNoVerdict() {
        addGapSource("NonBean", "public class NonBean {}", "class NonBean")
        val member = beanClass("com.app.NonBean")
        assertEquals(emptyList<String>(), member.annotations.mapNotNull { it.qualifiedName }.filterNot { it == "kotlin.Metadata" })
        assertFalse("com.app.NonBean" in activeBeans())

        assertNull(verdictOf(member))
    }

    fun testInactiveProfileMemberHasNoVerdict() {
        addProperties("spring.profiles.active=dev")
        val config = addGapConfiguration("InactiveProfileConfig", "@Profile(\"prod\")", "@Profile(\"prod\")")
        assertAnnotatedBy(config, SpringCoreClasses.PROFILE)
        assertPropertyDefined("spring.profiles.active")
        assertEquals(com.explyt.spring.core.service.ProfileActivation.INACTIVE, SpringSearchService.getInstance(project).profileActivation(config))

        assertNull(verdictOf(config))
        assertFalse("com.app.InactiveProfileConfig" in activeBeans())
        assertFalse("com.app.InactiveProfileConfig" in excludedBeans())
    }

    private fun addGapConfiguration(name: String, javaAnnotations: String, kotlinAnnotations: String): com.intellij.psi.PsiClass {
        addGapSource(name, "@Configuration $javaAnnotations public class $name {}", "@Configuration $kotlinAnnotations class $name")
        return beanClass("com.app.$name")
    }

    private fun addGapSource(name: String, javaDeclaration: String, kotlinDeclaration: String) {
        val kotlin = fixtures is com.explyt.spring.core.service.conditional.kotlin.KotlinConditionFixtures
        val source = if (kotlin) {
            "package com.app\nimport org.springframework.boot.autoconfigure.condition.*\nimport org.springframework.context.annotation.*\n$kotlinDeclaration"
        } else {
            "package com.app;\nimport org.springframework.boot.autoconfigure.condition.*;\nimport org.springframework.context.annotation.*;\n$javaDeclaration"
        }
        myFixture.addFileToProject("com/app/$name.${if (kotlin) "kt" else "java"}", source)
        ModificationTrackerManager.getInstance(project).invalidateAll()
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
