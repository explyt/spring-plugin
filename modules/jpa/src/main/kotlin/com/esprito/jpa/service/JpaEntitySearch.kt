package com.esprito.jpa.service

import com.esprito.jpa.JpaClasses
import com.esprito.jpa.model.JpaEntity
import com.esprito.jpa.model.impl.JpaEntityPsi
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.util.CommonProcessors.CollectProcessor
import com.intellij.util.MergeQuery
import com.intellij.util.Processor
import com.intellij.util.UniqueResultsQuery
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType


@Service(Service.Level.PROJECT)
class JpaEntitySearch(
    private val project: Project
) {
    private val javaPsiFacade by lazy {
        project.service<JavaPsiFacade>()
    }

    fun loadEntities(module: Module): List<JpaEntity> {
        val processor = CollectProcessor<JpaEntity>()

        processEntities(
            GlobalSearchScope.moduleWithDependenciesScope(module),
            processor
        )

        return processor.results.toList()
    }

    fun processEntities(
        searchScope: GlobalSearchScope,
        processor: Processor<JpaEntity>
    ) {
        val allScope = GlobalSearchScope.allScope(project)

        val psiClassQuery = listOf(
            JpaClasses.entity,
            JpaClasses.mappedSuperclass,
            JpaClasses.embeddable
        ).flatMap {
            it.allFqns
        }.mapNotNull {
            javaPsiFacade.findClass(it, allScope)
        }.map {
            AnnotatedElementsSearch.searchPsiClasses(it, searchScope)
        }.reduce { acc, query ->
            MergeQuery(acc, query)
        }

        val myProcessor = Processor<PsiClass> {
            val uClass = it.toUElementOfType<UClass>()
                ?: return@Processor true

            val jpaEntity = JpaEntityPsi(uClass)
                ?: return@Processor true

            processor.process(jpaEntity)
        }

        UniqueResultsQuery(psiClassQuery) { psiClass ->
            psiClass.qualifiedName
        }.forEach(
            myProcessor
        )
    }
}