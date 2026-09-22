/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.util.ExplytAnnotationUtil.getStringMemberValues
import com.explyt.util.ExplytPsiUtil.getMetaAnnotation
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.JavaPsiFacade

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiType

/**
 * Reads the beans the IDE's own model believes are active in one module.
 *
 * This is an estimate of a module, not of a running application: the static model cannot prove which component
 * scan and which profiles the application would actually use, and the snapshot says so rather than implying the
 * precision of a loaded context.
 */
class StaticBeanSnapshotReader(private val project: Project) {

    @Suppress("DEPRECATION")
    fun read(module: Module, injectionFile: PsiFile?): List<ScopedBeanRecord> {
        val searchService = SpringSearchService.getInstance(project)
        // The active model, not `getProjectBeans`: the latter enumerates stereotypes without the conditional
        // filtering that decides whether a bean is in the context at all.
        val active = searchService.getActiveBeansClasses(module) + searchService.getStaticBeans(module)
        val fromTestSource = injectionFile?.virtualFile
            ?.let { ProjectRootManager.getInstance(project).fileIndex.isInTestSourceContent(it) } ?: false

        return active.asSequence()
            .onEach { ProgressManager.checkCanceled() }
            .filter { it.psiClass.isValid && it.psiMember.isValid }
            // A production query must not see test-only beans. The decision follows the file the query is about,
            // never the selected editor, so the same request answers the same way.
            .filter { fromTestSource || !it.isFromTestSource() }
            .map { toRecord(it, module) }
            .toList()
    }

    private fun toRecord(bean: PsiBean, module: Module): ScopedBeanRecord {
        val factory = bean.psiMember as? PsiMethod
        val knownNames = knownNamesOf(bean, module)

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
            primary = bean.isPrimary,
            priority = null,
            details = BeanDetailsEvidence(aliases = knownNames.toList(), primary = bean.isPrimary),
            limitations = emptySet()
        )
    }

    /**
     * Every name the declaration is known to answer to, in declaration order.
     *
     * Reading them here is what lets an exact-name query match an alias without a second lookup; the canonical
     * name stays the one the model already chose.
     */
    private fun knownNamesOf(bean: PsiBean, module: Module): Set<String> {
        val declared: Set<String> = bean.psiMember.resolveBeanName(module)
        return (listOf(bean.name) + declared + declaredBeanAliases(bean))
            .filterTo(LinkedHashSet()) { it.isNotBlank() }
    }

    /**
     * `@Bean` declares its names under either `value` or `name` - they are aliases of one attribute in Spring.
     * The shared `resolveBeanName` reads only `value`, so a bean declared as `@Bean(name = {"a", "b"})` would
     * answer to `b` at runtime while an exact-name query for it found nothing.
     */
    private fun declaredBeanAliases(bean: PsiBean): List<String> {
        val annotation = (bean.psiMember as? PsiModifierListOwner)?.getMetaAnnotation(SpringCoreClasses.BEAN)
            ?: return emptyList()
        return (annotation.getStringMemberValues("value") + annotation.getStringMemberValues("name")).toList()
    }

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
