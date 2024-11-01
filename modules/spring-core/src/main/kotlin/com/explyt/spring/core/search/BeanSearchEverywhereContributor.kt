/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.search

import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor
import com.intellij.ide.actions.searcheverywhere.WeightedSearchEverywhereContributor
import com.intellij.ide.util.NavigationItemListCellRenderer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
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
        selected.navigate(true)
        return true
    }

    override fun isDumbAware(): Boolean {
        return false
    }

    override fun isShownInSeparateTab() = true

    override fun fetchWeightedElements(
        pattern: String,
        progressIndicator: ProgressIndicator,
        consumer: Processor<in FoundItemDescriptor<BeanNavigationItem>>
    ) {
        val matcher = NameUtil.buildMatcher("*$pattern*", NameUtil.MatchingCaseSensitivity.NONE)
        val searchService = SpringSearchServiceFacade.getInstance(project)

        val navItemList = ApplicationManager.getApplication().runReadAction<List<BeanNavigationItem>, Throwable?> {
            val (active, excluded) = searchService.getAllBeansClassesConsideringContext(project)
            active.map { BeanNavigationItem(it, true) } +
                    excluded.map { BeanNavigationItem(it, false) }
        }
        if (navItemList != null) {
            for (item in navItemList) {
                if (matcher.matches(item.name)) {
                    consumer.process(FoundItemDescriptor(item, Int.MAX_VALUE))
                }
            }
        }
    }

}