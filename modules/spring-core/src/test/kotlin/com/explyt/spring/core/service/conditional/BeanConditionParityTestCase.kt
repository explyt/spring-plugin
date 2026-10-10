/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

abstract class BeanConditionParityTestCase : BeanConditionTestCase() {

    fun testUnconditionalComponentIsActive() {
        fixtures.addPlainComponent()

        assertBeanSets(active = setOf(APPLICATION, "com.app.PlainService"), excluded = emptySet())
    }

    fun testPropertyNotMatchingHavingValueIsExcluded() {
        addProperties("feature.sync=false")
        fixtures.addPropertyGatedConfiguration("SyncConfig")
        assertPropertyDefined("feature.sync")

        assertBeanSets(active = setOf(APPLICATION), excluded = setOf("com.app.SyncConfig"))
    }

    fun testUnresolvablePlaceholderKeepsBeanActive() {
        addProperties("feature.sync=\${SYNC_ENABLED}")
        fixtures.addPropertyGatedConfiguration("SyncConfig")
        assertPropertyDefined("feature.sync")
        assertPropertyUndefined("SYNC_ENABLED")

        assertBeanSets(active = setOf(APPLICATION, "com.app.SyncConfig"), excluded = emptySet())
    }

    fun testCustomConditionKeepsBeanActive() {
        fixtures.addCustomConditionConfiguration()
        assertAnnotatedBy(beanClass("com.app.CustomConditionConfig"), CONDITIONAL)

        assertBeanSets(active = setOf(APPLICATION, "com.app.CustomConditionConfig"), excluded = emptySet())
    }

    fun testUnresolvableProfileKeepsBeanActive() {
        fixtures.addUnknownProfileConfiguration()
        assertProfileValueUnresolved(beanClass("com.app.UnknownProfileConfig"))

        assertBeanSets(active = setOf(APPLICATION, "com.app.UnknownProfileConfig"), excluded = emptySet())
    }

    fun testConditionOnMissingBeanTypeIsExcluded() {
        fixtures.addMissingBeanConfiguration()
        assertAnnotatedBy(beanClass("com.app.MissingBeanConfig"), CONDITIONAL_ON_BEAN)

        assertBeanSets(active = setOf(APPLICATION), excluded = setOf("com.app.MissingBeanConfig"))
    }

    fun testInactiveOuterConfigurationExcludesItsBeanMethod() {
        addProperties("outer.enabled=false")
        fixtures.addConditionalOuterConfiguration()
        assertPropertyDefined("outer.enabled")

        assertBeanSets(
            active = setOf(APPLICATION),
            excluded = setOf("com.app.OuterConfig", "com.app.OuterConfig#outerService")
        )
    }

    fun testInactivePropertyWinsOverCustomCondition() {
        addProperties("mixed.enabled=false")
        fixtures.addMixedConditionsConfiguration()
        assertPropertyDefined("mixed.enabled")

        assertBeanSets(active = setOf(APPLICATION), excluded = setOf("com.app.MixedConditionsConfig"))
    }

    fun testPropertyGatedControllerWithoutPropertyIsExcluded() {
        addProperties("admin.marker=present")
        fixtures.addSyncAdminController()
        assertPropertyDefined("admin.marker")
        assertPropertyUndefined("admin.sync.enabled")

        assertBeanSets(active = setOf(APPLICATION), excluded = setOf("com.app.SyncAdminController"))
    }

    private fun assertBeanSets(active: Set<String>, excluded: Set<String>) {
        assertEquals("Active beans", active.toSortedSet(), activeBeans())
        assertEquals("Excluded beans", excluded.toSortedSet(), excludedBeans())
    }
}
