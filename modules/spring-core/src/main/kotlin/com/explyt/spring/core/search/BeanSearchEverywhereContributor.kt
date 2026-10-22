/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.search

import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor
import com.intellij.ide.actions.searcheverywhere.WeightedSearchEverywhereContributor
import com.intellij.ide.util.EditSourceUtil
import com.intellij.ide.util.NavigationItemListCellRenderer
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.MinusculeMatcher
import com.intellij.psi.codeStyle.NameUtil
import com.intellij.util.Processor

class BeanSearchEverywhereContributor(private val project: Project) :
    WeightedSearchEverywhereContributor<BeanNavigationItem> {

    override fun getSearchProviderId(): String {
        return BeanSearchEverywhereContributor::class.java.simpleName
    }

    override fun getGroupName() = "Beans"

    override fun getSortWeight() = 800

    override fun showInFindResults() = true

    override fun getElementsRenderer() = NavigationItemListCellRenderer()

    override fun getDataForItem(element: BeanNavigationItem, dataId: String) = null

    override fun processSelectedItem(selected: BeanNavigationItem, modifiers: Int, searchText: String): Boolean {
        if (!selected.canNavigate()) return false
        selected.navigate(true)
        return true
    }

    override fun isDumbAware(): Boolean {
        return false
    }

    override fun isShownInSeparateTab() = true

    /**
     * Search Everywhere calls this off the EDT, but a plain blocking read action still starves the EDT indirectly:
     * the bean model behind [SpringSearchServiceFacade.getAllBeansClassesConsideringContext] takes tens of seconds to
     * build on a large project, and any write action the user triggers meanwhile — typing included — waits for this
     * read action to finish. A cancellable read action lets the platform abort and restart the search instead.
     */
    override fun fetchWeightedElements(
        pattern: String,
        progressIndicator: ProgressIndicator,
        consumer: Processor<in FoundItemDescriptor<BeanNavigationItem>>
    ) {
        val matcher = NameUtil.buildMatcher("*$pattern*", NameUtil.MatchingCaseSensitivity.NONE)
        val searchService = SpringSearchServiceFacade.getInstance(project)

        // Match by name first and only then resolve navigation: building a descriptor per bean is expensive
        // (it reads the member's text offset and creates a range marker) and would run on every keystroke.
        val navItemList = ReadAction.nonBlocking<List<BeanNavigationItem>> {
            if (!SpringCoreUtil.isSpringBootProject(project)) return@nonBlocking emptyList()

            val (active, excluded) = searchService.getAllBeansClassesConsideringContext(project)
            toNavigationItems(active, true, matcher) + toNavigationItems(excluded, false, matcher)
        }
            .wrapProgress(progressIndicator)
            .executeSynchronously()

        for (item in navItemList) {
            progressIndicator.checkCanceled()
            if (!consumer.process(FoundItemDescriptor(item, Int.MAX_VALUE))) return
        }
    }

    private fun toNavigationItems(
        beans: Collection<PsiBean>,
        isActive: Boolean,
        matcher: MinusculeMatcher
    ): List<BeanNavigationItem> {
        return beans.mapNotNull { bean ->
            ProgressManager.checkCanceled()
            if (!matcher.matches(bean.name)) return@mapNotNull null
            BeanNavigationItem(bean, isActive, EditSourceUtil.getDescriptor(bean.psiMember))
        }
    }

}
