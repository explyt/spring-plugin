/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

interface ConditionFixtures {
    fun addApplication()
    fun addPlainComponent()
    fun addPropertyGatedConfiguration(className: String)
    fun addCustomConditionConfiguration()
    fun addUnknownProfileConfiguration()
    fun addMissingBeanConfiguration()
    fun addConditionalOuterConfiguration()
    fun addMixedConditionsConfiguration()
    fun addSyncAdminController()
}
