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