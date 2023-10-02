package com.esprito.spring.core.service

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.util.SpringCoreUtil.getBeanName
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanName
import com.esprito.spring.core.util.SpringCoreUtil.resolvePsiClass
import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isEqualOrInheritor
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner

@Service(Service.Level.PROJECT)
class SpringBeanService {

    fun getBeanCandidates(
        resolvedPsiClass: PsiClass,
        module: Module
    ): Set<PsiBean> {
        val searchService = SpringSearchService.getInstance(module.project)
        val classInheritors = searchService.searchClassInheritors(resolvedPsiClass)
        val methodsPsiBeans = searchService.getComponentBeanPsiMethods(module)
            .filter { it.returnPsiClass?.isEqualOrInheritor(resolvedPsiClass) == true }.toSet()

        val beanCandidates = getBeanCandidatesInPsiModifierListOwner(classInheritors, SpringCoreClasses.COMPONENT)
        beanCandidates += getBeanCandidatesInPsiModifierListOwner(methodsPsiBeans, SpringCoreClasses.BEAN)
        if (resolvedPsiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)) {
            beanCandidates += getBeanCandidatesInPsiModifierListOwner(setOf(resolvedPsiClass), SpringCoreClasses.COMPONENT)
        }

        return beanCandidates
    }

    private fun getBeanCandidatesInPsiModifierListOwner(
        owners: Set<PsiModifierListOwner>,
        annotationName: String
    ): MutableSet<PsiBean> {
        val beanCandidates = mutableSetOf<PsiBean>()
        owners.forEach {
            val psiClass = it.resolvePsiClass
            if (psiClass != null) {
                val values = SpringProperties.stringQualifiers
                    .asSequence()
                    .mapNotNull { annotation -> getAnnotationValue(it, annotation) }
                    .map { value -> PsiBean(value, psiClass, null) }
                    .toMutableSet()
                beanCandidates += values

                val beanNameAnnotationValue = getAnnotationValue(it, annotationName)
                if (values.isEmpty() && !beanNameAnnotationValue.isNullOrBlank()) {
                    beanCandidates += PsiBean(beanNameAnnotationValue, psiClass, null)
                }
                if (values.isEmpty() && beanNameAnnotationValue.isNullOrBlank()) {

                    val beanName = if (it is PsiClass) it.getBeanName() else if (it is PsiMethod) it.resolveBeanName else null
                    if (beanName != null) {
                        beanCandidates += PsiBean(beanName, psiClass, null)
                    }
                }
            }
        }
        return beanCandidates
    }

    private fun getAnnotationValue(psiClass: PsiModifierListOwner, annotationName: String): String? {
        if (psiClass.isMetaAnnotatedBy(annotationName)) {
            val annotation = psiClass.getMetaAnnotation(annotationName)
            if (annotation != null) {
                val value = AnnotationUtil.getStringAttributeValue(annotation, "value")
                if (!value.isNullOrBlank()) {
                    return value
                }
            }
        }
        return null
    }

    companion object {
        fun getInstance(project: Project): SpringBeanService = project.service()
    }
}