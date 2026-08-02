/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.search

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class BeanSearchEverywhereContributorFactory : SearchEverywhereContributorFactory<BeanNavigationItem> {

    override fun createContributor(initEvent: AnActionEvent): SearchEverywhereContributor<BeanNavigationItem> {
        val project = initEvent.project ?: throw RuntimeException("no project found")
        return BeanSearchEverywhereContributor(project)
    }

    override fun isAvailable(project: Project?): Boolean {
        // Search Everywhere asks availability on the EDT, so this must stay cheap: read the cached Spring Boot
        // flag, which the service computes in a background read action instead of hitting libraries/indices here.
        project ?: return false
        return BeanSearchAvailabilityService.getInstance(project).isSpringBootProject()
    }

}