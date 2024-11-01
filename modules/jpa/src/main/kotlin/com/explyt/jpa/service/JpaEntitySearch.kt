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

package com.explyt.jpa.service

import com.explyt.jpa.JpaClasses
import com.explyt.jpa.model.JpaEntity
import com.explyt.jpa.model.impl.JpaEntityPsi
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.util.CommonProcessors.CollectProcessor
import com.intellij.util.EmptyQuery
import com.intellij.util.MergeQuery
import com.intellij.util.Processor
import com.intellij.util.UniqueResultsQuery


@Service(Service.Level.PROJECT)
class JpaEntitySearch(
    private val project: Project
) {
    private val javaPsiFacade by lazy { JavaPsiFacade.getInstance(project) }

    fun loadEntities(module: Module): List<JpaEntity> {
        val processor = CollectProcessor<JpaEntity>()

        processEntities(
            GlobalSearchScope.moduleWithDependenciesScope(module),
            processor
        )

        return processor.results.toList()
    }

    private fun processEntities(
        searchScope: GlobalSearchScope,
        processor: Processor<JpaEntity>
    ) {
        val allScope = GlobalSearchScope.allScope(project)

        val queries = listOf(
            JpaClasses.entity,
            JpaClasses.mappedSuperclass,
            JpaClasses.embeddable
        ).flatMap {
            it.allFqns
        }.mapNotNull {
            javaPsiFacade.findClass(it, allScope)
        }.map {
            AnnotatedElementsSearch.searchPsiClasses(it, searchScope)
        }

        val psiClassQuery = if (queries.isEmpty()) {
            EmptyQuery()
        } else {
            queries.reduce { acc, query ->
                MergeQuery(acc, query)
            }
        }

        val myProcessor = Processor<PsiClass> {
            val jpaEntity = JpaEntityPsi(it)

            processor.process(jpaEntity)
        }

        UniqueResultsQuery(psiClassQuery) { psiClass ->
            psiClass.qualifiedName
        }.forEach(
            myProcessor
        )
    }

    companion object {
        fun getInstance(project: Project): JpaEntitySearch = project.service()
    }
}