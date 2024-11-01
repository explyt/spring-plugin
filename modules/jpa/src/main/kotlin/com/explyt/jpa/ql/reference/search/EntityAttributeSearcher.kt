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

import com.explyt.jpa.service.JpaService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.search.GlobalSearchScopeUtil
import com.intellij.psi.search.PsiSearchHelper
import org.jetbrains.uast.UField
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.toUElementOfType

class EntityAttributeSearcher : JpqlQuerySearcherBase() {
    override fun getSearchDetails(elementToSearch: PsiElement): SearchDetails? {
        val jpaService = JpaService.getInstance(elementToSearch.project)
        val searchHelper = PsiSearchHelper.getInstance(elementToSearch.project)

        val uField = elementToSearch.toUElementOfType<UField>() ?: return null


        val containingClass = uField.getContainingUClass() ?: return null

        val psiField = uField.javaPsi as? PsiField
            ?: return null

        val isJpaAttribute = jpaService.isJpaEntityAttribute(psiField)

        if (!isJpaAttribute)
            return null

        val searchScope = GlobalSearchScopeUtil.toGlobalSearchScope(
            searchHelper.getUseScope(containingClass.javaPsi),
            elementToSearch.project
        )

        @Suppress("UElementAsPsi")
        return SearchDetails(uField.name, searchScope)
    }
}