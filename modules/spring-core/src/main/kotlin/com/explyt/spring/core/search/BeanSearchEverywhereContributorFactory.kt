/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.search

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project

class BeanSearchEverywhereContributorFactory : SearchEverywhereContributorFactory<BeanNavigationItem> {

    override fun createContributor(initEvent: AnActionEvent): SearchEverywhereContributor<BeanNavigationItem> {
        return BeanSearchEverywhereContributor(initEvent.getRequiredData(CommonDataKeys.PROJECT))
    }

    override fun isAvailable(project: Project?): Boolean {
        // Search Everywhere asks availability on the EDT, so keep this check cheap.
        // Spring Boot detection touches libraries/indices and is performed later in
        // the contributor's background read action instead.
        return project != null
    }

}