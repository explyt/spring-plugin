package com.esprito.spring.core.service.conditional

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.service.PsiBean
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.spring.core.util.SpringCoreUtil.resolvePsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.util.childrenOfType

class ConditionalOnBeanStrategy(module: Module) : ExclusionStrategy {
    private val annotationHolder = MetaAnnotationsHolder.of(module, SpringCoreClasses.CONDITIONAL_ON_BEAN)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        if (dependant.annotations.none { annotationHolder.contains(it) }) {
            return false
        }

        val names = annotationHolder.getAnnotationMemberValues(dependant, setOf("name"))
            .map { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
        if (names.isNotEmpty() && foundBeans.none { names.contains(it.name) }) {
            return true
        }

        val types = annotationHolder.getAnnotationMemberValues(dependant, setOf("type"))
            .map { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
        if (types.isNotEmpty() && foundBeans.none { types.contains(it.psiClass.qualifiedName) }) {
            return true
        }

        val classAttributes = annotationHolder.getAnnotationMemberValues(dependant, setOf("value"))
        val classesQn = if (names.isEmpty() && types.isEmpty() && classAttributes.isEmpty()) {
            setOfNotNull(dependant.resolvePsiClass?.qualifiedName)
        } else {
            classAttributes.asSequence()
                .flatMap { it.childrenOfType<PsiTypeElement>() }
                .map { it.type }
                .mapNotNull { it.resolveBeanPsiClass }
                .map { it.qualifiedName }
                .toSet()
        }
        return classesQn.isNotEmpty() && foundBeans.none { classesQn.contains(it.psiClass.qualifiedName) }
    }

}