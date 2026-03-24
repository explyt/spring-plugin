/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.search

import com.explyt.spring.core.util.SpringCoreUtil
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
        return project != null && SpringCoreUtil.isSpringBootProject(project)
    }

}