package com.esprito.jpa.ql.reference.search

import com.esprito.jpa.model.impl.JpaEntityPsi
import com.esprito.jpa.service.JpaService
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