package com.esprito.spring.data.util

import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.util.PsiAnnotationUtils
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.spring.data.SpringDataClasses
import com.esprito.spring.data.SpringDataClasses.SPRING_RESOURCE
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiUtil

object SpringDataRepositoryUtil {

    fun substituteRepositoryTypes(repositoryClass: PsiClass): RepositoryTypes? {
        if (AnnotationUtil.isAnnotated(
                repositoryClass, SpringDataClasses.REPOSITORY_ANNOTATION,
                AnnotationUtil.CHECK_HIERARCHY
            )
        ) {
            return substituteForRepositoryDefinition(repositoryClass)
        }
        val psiClassType = JavaPsiFacade.getInstance(repositoryClass.project)
            .elementFactory.createType(repositoryClass)

        val psiType = PsiUtil.substituteTypeParameter(psiClassType, SPRING_RESOURCE, 0, false) ?: return null
        val idPsiType = PsiUtil.substituteTypeParameter(psiClassType, SPRING_RESOURCE, 1, false) ?: return null
        val psiClass = (psiType as? PsiClassType)?.resolve() ?: return null
        return RepositoryTypes(psiClass, idPsiType)
    }

    private fun substituteForRepositoryDefinition(repositoryClass: PsiClass): RepositoryTypes? {
        val module = ModuleUtilCore.findModuleForPsiElement(repositoryClass) ?: return null
        val metaAnnotationsHolder = MetaAnnotationsHolder.of(module, SpringDataClasses.REPOSITORY_ANNOTATION)
        val annotation = repositoryClass.annotations.find { metaAnnotationsHolder.contains(it) } ?: return null
        val domainValues = metaAnnotationsHolder.getAnnotationMemberValues(annotation, setOf("domainClass"))
        val idValues = metaAnnotationsHolder.getAnnotationMemberValues(annotation, setOf("idClass"))
        val classDomain = PsiAnnotationUtils.getPsiTypes(domainValues).map { it.resolveBeanPsiClass }.firstOrNull()
        val typeId = PsiAnnotationUtils.getPsiTypes(idValues).firstOrNull()
        return if (classDomain != null && typeId != null) RepositoryTypes(classDomain, typeId) else null
    }
}

data class RepositoryTypes(val psiClass: PsiClass, val psiType: PsiType)