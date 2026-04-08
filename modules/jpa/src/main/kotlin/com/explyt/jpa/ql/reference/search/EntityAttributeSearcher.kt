/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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