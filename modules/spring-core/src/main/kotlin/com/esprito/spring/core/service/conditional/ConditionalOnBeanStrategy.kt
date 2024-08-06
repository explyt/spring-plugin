package com.esprito.spring.core.service.conditional

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.service.PsiBean
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.PsiAnnotationUtils
import com.esprito.spring.core.util.SpringCoreUtil.resolvePsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember

class ConditionalOnBeanStrategy(module: Module) : ExclusionStrategy {
    private val annotationBeanHolder = SpringSearchService.getInstance(module.project)
        .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_BEAN)
    private val annotationSingleHolder = SpringSearchService.getInstance(module.project)
        .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_SINGLE_BEAN)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        val exclude = shouldExclude(annotationBeanHolder, dependant, foundBeans)
        if (exclude) return true

        val excludeSingle = shouldExclude(annotationSingleHolder, dependant, foundBeans)
        if (excludeSingle) return true

        return false
    }

    private fun shouldExclude(
        annotationHolder: MetaAnnotationsHolder, dependant: PsiMember, foundBeans: Collection<PsiBean>,
    ): Boolean {
        if (dependant.annotations.none { annotationHolder.contains(it) }) {
            return false
        }
        val foundBeanNames = foundBeans.mapTo(mutableSetOf()) { it.name }

        val names = annotationHolder.getAnnotationMemberValues(dependant, setOf("name"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
        if (names.isNotEmpty() && names.any { !foundBeanNames.contains(it) }) {
            return true
        }

        val foundBeanClassQn = foundBeans.mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
        val types = annotationHolder.getAnnotationMemberValues(dependant, setOf("type"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
        if (types.isNotEmpty() && types.any { !foundBeanClassQn.contains(it) }) {
            return true
        }

        val classAttributes = annotationHolder.getAnnotationMemberValues(dependant, setOf("value"))
        val classesQn = if (names.isEmpty() && types.isEmpty() && classAttributes.isEmpty()) {
            setOfNotNull(dependant.resolvePsiClass?.qualifiedName)
        } else {
            val typeNames = PsiAnnotationUtils.getTypeNames(classAttributes)
            if (classAttributes.size != typeNames.size) {
                return true
            }
            typeNames
        }
        if (classAttributes.isNotEmpty() && classesQn.isEmpty()) {
            return true
        }
        return classesQn.isNotEmpty() && classesQn.any { !foundBeanClassQn.contains(it) }
    }

}