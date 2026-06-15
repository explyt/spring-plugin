/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem

import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy

class DebugProjectResolverPolicy(val rawBeanData: String): ProjectResolverPolicy {
    override fun isPartialDataResolveAllowed() = false
}