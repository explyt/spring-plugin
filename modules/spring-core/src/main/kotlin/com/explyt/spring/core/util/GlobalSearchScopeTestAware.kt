/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope

object GlobalSearchScopeTestAware {
    fun getScope(module: Module, scope: SearchScope): SearchScope {
        return if (ApplicationManager.getApplication().isUnitTestMode)
            GlobalSearchScope.allScope(module.project) else scope
    }
}
