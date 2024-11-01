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

package com.explyt.jpa.ql.reference.search

import com.explyt.jpa.model.impl.JpaEntityPsi
import com.explyt.jpa.service.JpaService
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScopeUtil
import com.intellij.psi.search.PsiSearchHelper
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

class EntitySearcher : JpqlQuerySearcherBase() {
    override fun getSearchDetails(elementToSearch: PsiElement): SearchDetails? {
        val jpaService = JpaService.getInstance(elementToSearch.project)
        val searchHelper = PsiSearchHelper.getInstance(elementToSearch.project)

        val psiClass = elementToSearch.toUElementOfType<UClass>()
            ?.javaPsi ?: return null

        val jpaEntity = psiClass
            .takeIf(jpaService::isJpaEntity)
            ?.let(JpaEntityPsi::invoke)
            ?: return null

        val searchScope = GlobalSearchScopeUtil.toGlobalSearchScope(
            searchHelper.getUseScope(psiClass),
            elementToSearch.project
        )

        val entityName = jpaEntity.name ?: return null

        return SearchDetails(entityName, searchScope)
    }
}