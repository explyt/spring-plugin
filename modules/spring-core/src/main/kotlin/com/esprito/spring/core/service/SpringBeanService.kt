package com.esprito.spring.core.service

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.util.SpringCoreUtil.getBeanName
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanName
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.spring.core.util.SpringCoreUtil.resolvePsiClass
import com.esprito.util.EspritoPsiUtil.isGeneric
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*

@Service(Service.Level.PROJECT)
class SpringBeanService {

    fun getBeanCandidates(
        module: Module,
        psiType: PsiType,
        beanName: String
    ): Set<PsiBean> {
        val resolvedPsiBeanClass: PsiClass = psiType.resolveBeanPsiClass ?: return emptySet()
        val searchService = SpringSearchService.getInstance(module.project)

        val excludedBeans = searchService.getExcludedBeansClasses(module)
        val classInheritors = searchService.searchClassInheritors(resolvedPsiBeanClass)
            .filter { inheritor -> excludedBeans.none { it.psiMember == inheritor } }.toSet()
        val allBeansPsiMethods = searchService.getComponentBeanPsiMethods(module)
            .filter { psiMethod -> excludedBeans.none { it.psiMember == psiMethod } }.toSet()
        val beansPsiMethods = searchService.getBeansPsiMethods(psiType, allBeansPsiMethods, resolvedPsiBeanClass).toSet()

        val beanCandidatesByComponent = getBeanCandidatesInPsiModifierListOwner(module, classInheritors, SpringCoreClasses.COMPONENT)
        val beanCandidatesByMethod = getBeanCandidatesInPsiModifierListOwner(module, beansPsiMethods, SpringCoreClasses.BEAN)

        var beanCandidatesByResolveClass: Set<PsiBean> = setOf()
        if (resolvedPsiBeanClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
            && (!resolvedPsiBeanClass.isGeneric(psiType) || beansPsiMethods.isEmpty())) {
            beanCandidatesByResolveClass = getBeanCandidatesInPsiModifierListOwner(module, setOf(resolvedPsiBeanClass), SpringCoreClasses.COMPONENT)
        }
        return filterBeanCandidates(module, beanName,
            beanCandidatesByComponent + beanCandidatesByResolveClass, beanCandidatesByMethod)
    }

    private fun getBeanCandidatesInPsiModifierListOwner(
        module: Module,
        owners: Set<PsiModifierListOwner>,
        annotationName: String
    ): Set<PsiBean> {
        val beanCandidates = mutableSetOf<PsiBean>()
        owners.forEach { owner ->
            val psiClass = if (owner is PsiMethod) {
                owner.returnType?.resolveBeanPsiClass
            } else {
                owner.resolvePsiClass
            }
            if (psiClass != null) {
                val isPrimary = owner.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY)
                val beanQualifier = SpringCoreClasses.STRING_QUALIFIERS
                    .asSequence()
                    .mapNotNull { owner.getAnnotationValue(module, it) }
                    .map { value -> PsiBean(value, psiClass, null, owner, isPrimary) }
                    .toMutableSet()
                beanCandidates += beanQualifier

                val beanNameAnnotationValue = owner.getAnnotationValue(module, annotationName)
                if (beanQualifier.isEmpty() && !beanNameAnnotationValue.isNullOrBlank()) {
                    beanCandidates += PsiBean(beanNameAnnotationValue, psiClass, null, owner, isPrimary)
                }

                if (beanQualifier.isEmpty() && beanNameAnnotationValue.isNullOrBlank()) {
                    val beanName = if (owner is PsiClass) owner.getBeanName() else if (owner is PsiMethod) owner.resolveBeanName else null
                    if (beanName != null) {
                        beanCandidates += PsiBean(beanName, psiClass, null, owner, isPrimary)
                    }
                }
            }
        }
        return beanCandidates
    }

    private fun filterBeanCandidates(
        module: Module,
        beanName: String,
        beanCandidatesByComponent: Set<PsiBean>,
        beanCandidatesByMethod: Set<PsiBean>
    ): Set<PsiBean> {
        val metaHolder = SpringSearchService.getInstance(module.project).getMetaAnnotations(module, SpringCoreClasses.COMPONENT)

        val isComponentPrimary = beanCandidatesByComponent.any { it.isPrimary }
        val isMethodPrimary = beanCandidatesByMethod.any { it.isPrimary }
        val isExistComponentName = beanCandidatesByComponent.any {
            if (it.psiMember is PsiMember) {
                metaHolder.getAnnotationMemberValues(it.psiMember, setOf("value")).isNotEmpty()
            } else {
                false
            }
        }

        if (isComponentPrimary) {
            return if (isExistComponentName) {
                if (isMethodPrimary) {
                    // return component primary with name and bean primary
                    (beanCandidatesByComponent.asSequence().filter { it.isPrimary } + beanCandidatesByMethod.asSequence().filter { it.isPrimary }).toSet()
                } else {
                    // return only component primary with name (bean not primary)
                    beanCandidatesByComponent.asSequence().filter { it.isPrimary }.toSet()
                }
            } else {
                if (isMethodPrimary) {
                    // return only method primary (component primary (without name))
                    beanCandidatesByMethod.asSequence().filter { it.isPrimary }.toSet()
                } else {
                    // return component primary and method
                    beanCandidatesByComponent.asSequence().filter { it.isPrimary }.toSet() + beanCandidatesByMethod
                }
            }
        } else if (isMethodPrimary) {
            // return method primary
            return beanCandidatesByMethod.asSequence().filter { it.isPrimary }.toSet()
        }
        val byNameBeanCandidateMethods = beanCandidatesByMethod.asSequence().filter { it.name == beanName }.toSet()
        val byNameBeanCandidateComponents = beanCandidatesByComponent.asSequence().filter { it.name == beanName }.toSet()
        if (byNameBeanCandidateMethods.size == 1 && byNameBeanCandidateComponents.size == 1) {
            // return only method by name
            return byNameBeanCandidateMethods
        }
        // return all components and methods
        return beanCandidatesByComponent + beanCandidatesByMethod
    }

    private fun PsiModifierListOwner.getAnnotationValue( module: Module, annotationName: String): String? {
        if (this is PsiMember) {
            val metaHolder = SpringSearchService.getInstance(module.project).getMetaAnnotations(module, annotationName)
            val annotationValue = metaHolder.getAnnotationMemberValues(this, setOf("value")).firstOrNull()
            return annotationValue?.let { AnnotationUtil.getStringAttributeValue(it) }
        }
        return null
    }

    companion object {
        fun getInstance(project: Project): SpringBeanService = project.service()
    }
}