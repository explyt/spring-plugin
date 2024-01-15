package com.esprito.spring.core.service.conditional

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.PsiBean
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.PsiAnnotationUtils
import com.esprito.spring.core.util.SpringCoreUtil.resolvePsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember

class ConditionalOnMissingBeanStrategy(module: Module) : ExclusionStrategy {
    private val annotationHolder = SpringSearchService.getInstance(module.project)
        .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_MISSING_BEAN)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        if (dependant.annotations.none { annotationHolder.contains(it) }) {
            return false
        }

        val names = annotationHolder.getAnnotationMemberValues(dependant, setOf("name"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
        if (names.isNotEmpty() && foundBeans.any { names.contains(it.name) }) {
            return true
        }

        val types = annotationHolder.getAnnotationMemberValues(dependant, setOf("type"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
        if (types.isNotEmpty() && foundBeans.any { types.contains(it.psiClass.qualifiedName) }) {
            return true
        }

        val classAttributes = annotationHolder.getAnnotationMemberValues(dependant, setOf("value"))
        val classesQn = if (names.isEmpty() && types.isEmpty() && classAttributes.isEmpty()) {
            setOfNotNull(dependant.resolvePsiClass?.qualifiedName)
        } else {
            PsiAnnotationUtils.getTypeNames(classAttributes)

        }
        return classesQn.isNotEmpty() && foundBeans.any { classesQn.contains(it.psiClass.qualifiedName) }
    }

}