package com.esprito.spring.core.service.conditional

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.service.PsiBean
import com.esprito.spring.core.util.PsiAnnotationUtils
import com.esprito.spring.core.util.SpringCoreUtil.resolvePsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember

class ConditionalOnBeanStrategy(module: Module) : ExclusionStrategy {
    private val annotationHolder = MetaAnnotationsHolder.of(module, SpringCoreClasses.CONDITIONAL_ON_BEAN)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
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
            PsiAnnotationUtils.getTypeNames(classAttributes)
        }
        return classesQn.isNotEmpty() && classesQn.any { !foundBeanClassQn.contains(it) }
    }

}