package com.explyt.spring.core.search

import com.explyt.spring.core.util.SpringCoreUtil
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
        return project != null && SpringCoreUtil.isSpringBootProject(project)
    }

}