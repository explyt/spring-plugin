/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.JavaPsiFacade

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType

/**
 * Reads the beans the IDE's own model believes are active in one module.
 *
 * This is an estimate of a module, not of a running application: the static model cannot prove which component
 * scan and which profiles the application would actually use, and the snapshot says so rather than implying the
 * precision of a loaded context.
 */
class StaticBeanSnapshotReader(private val project: Project) {

    data class Records(val active: List<ScopedBeanRecord>, val inactive: List<ScopedBeanRecord>)

    @Suppress("DEPRECATION")
    fun read(module: Module, injectionFile: PsiFile?): Records {
        val searchService = SpringSearchService.getInstance(project)
        // The active model, not `getProjectBeans`: the latter enumerates stereotypes without the conditional
        // filtering that decides whether a bean is in the context at all.
        val found = searchService.foundBeans(module)
        val active = found.active + searchService.getStaticBeans(module)
        val excluded = found.excluded
        val verdicts = found.verdicts
        val fromTestSource = injectionFile?.virtualFile
            ?.let { ProjectRootManager.getInstance(project).fileIndex.isInTestSourceContent(it) } ?: false

        fun recordsOf(beans: Collection<PsiBean>, conditionOf: (PsiBean) -> BeanConditionRecord?) =
            beans.asSequence()
                .onEach { ProgressManager.checkCanceled() }
                .filter { it.psiClass.isValid && it.psiMember.isValid }
                // A production query must not see test-only beans. The decision follows the file the query is
                // about, never the selected editor, so the same request answers the same way.
                .filter { fromTestSource || !it.isFromTestSource() }
                .map { DeclaredBean(it, declaredNamesOf(it, module)) }
                .toList()
                .groupBy { it.identity }
                .map { (_, declarations) ->
                    val canonical = canonicalOf(declarations)
                    toRecord(canonical, module, conditionOf(canonical.bean))
                }

        val activeRecords = recordsOf(active) { bean ->
            BeanConditionRecord.of(verdicts[bean])?.takeIf { it.undecided }
        }
        val inactiveRecords = recordsOf(excluded.filter { verdicts[it] is ConditionVerdict.Inactive }) { bean ->
            BeanConditionRecord.of(verdicts[bean])
        }
        return Records(activeRecords, inactiveRecords)
    }

    /**
     * `@Bean({"a", "b"})` reaches the model as one [PsiBean] per name, while Spring registers one bean with aliases.
     * Only names the member itself declares are folded together: two beans that merely share a class stay two.
     */
    private class DeclaredBean(val bean: PsiBean, val declaredNames: Set<String>) {
        val identity: Any = if (bean.name in declaredNames) bean.psiMember else bean
    }

    private fun canonicalOf(declarations: List<DeclaredBean>): DeclaredBean {
        val canonicalName = declarations.first().declaredNames.firstOrNull()
        return declarations.firstOrNull { it.bean.name == canonicalName } ?: declarations.first()
    }

    private fun toRecord(declared: DeclaredBean, module: Module, condition: BeanConditionRecord?): ScopedBeanRecord {
        val bean = declared.bean
        val factory = bean.psiMember as? PsiMethod
        val knownNames = (listOf(bean.name) + declared.declaredNames).filterTo(LinkedHashSet()) { it.isNotBlank() }
        // The active model builds its beans without reading `@Primary`, so the flag is taken from the declaration.
        val primary = bean.isPrimary || bean.psiMember.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY)

        return ScopedBeanRecord(
            id = BeanSnapshotIdentity.hash(
                listOf(module.name, bean.psiClass.qualifiedName ?: "", factory?.name ?: "", declarationKey(bean))
            ),
            name = bean.name,
            knownNames = knownNames,
            typeName = declaredTypeOf(bean, factory)?.canonicalText ?: bean.psiClass.qualifiedName,
            kind = if (factory == null) BeanKind.COMPONENT else BeanKind.BEAN_METHOD,
            declaration = bean.psiMember,
            declaredType = declaredTypeOf(bean, factory),
            // The declaring module comes from the member, not from the type: a `@Bean Clock` is declared in the
            // project even though `java.time.Clock` belongs to the JDK.
            declarationModule = ModuleUtilCore.findModuleForPsiElement(bean.psiMember)?.name,
            primary = primary,
            priority = null,
            details = BeanDetailsEvidence(aliases = knownNames.toList(), primary = primary),
            limitations = emptySet(),
            condition = condition
        )
    }

    private fun declaredNamesOf(bean: PsiBean, module: Module): Set<String> =
        bean.psiMember.resolveBeanName(module)
            .filterTo(LinkedHashSet()) { it.isNotBlank() }

    private fun declaredTypeOf(bean: PsiBean, factory: PsiMethod?): PsiType? = when (factory) {
        null -> JavaPsiFacade.getElementFactory(project).createType(bean.psiClass)
        else -> factory.returnType
    }

    private fun declarationKey(bean: PsiBean): String =
        bean.psiMember.containingFile?.virtualFile?.path ?: bean.psiClass.qualifiedName ?: ""

    private fun PsiBean.isFromTestSource(): Boolean {
        val file = psiMember.containingFile?.virtualFile ?: return false
        return ProjectRootManager.getInstance(project).fileIndex.isInTestSourceContent(file)
    }
}
