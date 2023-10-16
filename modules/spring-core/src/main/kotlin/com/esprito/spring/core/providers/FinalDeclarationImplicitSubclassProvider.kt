package com.esprito.spring.core.providers

import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringCoreClasses.ASYNC
import com.esprito.spring.core.SpringCoreClasses.BEAN
import com.esprito.spring.core.SpringCoreClasses.CACHEABLE
import com.esprito.spring.core.SpringCoreClasses.CACHECONFIG
import com.esprito.spring.core.SpringCoreClasses.CACHEEVICT
import com.esprito.spring.core.SpringCoreClasses.CACHEPUT
import com.esprito.spring.core.SpringCoreClasses.CACHING
import com.esprito.spring.core.SpringCoreClasses.CONFIGURATION
import com.esprito.spring.core.util.SpringCoreUtil.isSpringBeanCandidateClass
import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isAbstract
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isStatic
import com.intellij.codeInspection.inheritance.ImplicitSubclassProvider
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.psi.*

@Suppress("UnstableApiUsage")
class FinalDeclarationImplicitSubclassProvider : ImplicitSubclassProvider() {

    private val transactionalAnnotations = listOf(SpringCoreClasses.TRANSACTIONAL) + JavaEeClasses.TRANSACTIONAL.allFqns
    private val cacheableAnnotations = listOf(CACHEABLE, CACHING, CACHEEVICT, CACHEPUT, CACHECONFIG)
    private val overrideMethods = listOf(ASYNC) + cacheableAnnotations
    private val overrideClasses = listOf(ASYNC, CONFIGURATION) + transactionalAnnotations + cacheableAnnotations

    override fun getSubclassingInfo(psiClass: PsiClass): SubclassingInfo? {
        val methods = psiClass.methods

        val methodsInfo = mutableMapOf<PsiMethod, OverridingInfo>()
        for (method in methods) {
            val overridingInfo = getOverridingInfoForMethod(method)
            if (overridingInfo != null) {
                methodsInfo[method] = overridingInfo
            }
        }

        val existAnnotation = psiClass.modifierList?.annotations?.isNotEmpty() ?: false
        if (existAnnotation) {
            val annotation = overrideClasses.asSequence()
                .map { psiClass.getMetaAnnotation(it) }
                .filterNotNull()
                .firstOrNull()
            if ( annotation != null ) {
                val shotName = annotation.qualifiedName?.split(".")?.last() ?: ""
                return SubclassingInfo(
                    SpringCoreBundle.message("esprito.implicit.inspection.forClass.annotated", shotName),
                    methodsInfo.ifEmpty { null },
                    psiClass.isAbstract)
            }

        }

        if (methodsInfo.isNotEmpty()) {
            val className = psiClass.name ?: return null
            return SubclassingInfo(
                SpringCoreBundle.message("esprito.implicit.inspection.subclass.display.forClass", className),
                methodsInfo,
                psiClass.isAbstract
            )
        }
        return null
    }

    override fun isApplicableTo(psiClass: PsiClass): Boolean {
        return isSpringBeanCandidateClass(psiClass)
    }

    private fun getOverridingInfoForMethod(method: PsiMethod): OverridingInfo? {
        val annotations = method.modifierList.annotations
        if (annotations.isEmpty()) {
            return null
        }

        val annotation = overrideMethods.asSequence()
            .map { method.getMetaAnnotation(it) }
            .filterNotNull()
            .firstOrNull()
        return if (annotation != null) {
            val shotName = annotation.qualifiedName?.split(".")?.last() ?: ""
            val message =
                SpringCoreBundle.message("esprito.implicit.inspection.forMethod.annotated", shotName)
            OverridingInfo(message)
        } else if (isBeanInConfiguration(method)) {
            OverridingInfo(SpringCoreBundle.message("esprito.implicit.inspection.bean.in.configuration"))
        } else {
            getOverridingInfoForTransactional(method)
        }
    }

    private fun isBeanInConfiguration(method: PsiMethod): Boolean {
        if (method.isStatic) {
            return false
        }
        val psiClass = method.containingClass ?: return false
        return psiClass.isMetaAnnotatedBy(CONFIGURATION) && method.isMetaAnnotatedBy(BEAN)
    }

    private fun isTestFiles(psiElement: PsiElement): Boolean {
        val virtualFile = psiElement.containingFile?.virtualFile ?: return false
        return TestSourcesFilter.isTestSources(virtualFile, psiElement.project)
    }

    private fun getModifiersForTransactional(method: PsiMethod): Array<JvmModifier> {
        return if (isTestFiles(method)) {
            arrayOf(JvmModifier.PUBLIC, JvmModifier.PROTECTED, JvmModifier.PACKAGE_LOCAL)
        } else {
            arrayOf(JvmModifier.PUBLIC)
        }
    }

    private fun getOverridingInfoForTransactional(method: PsiMethod): OverridingInfo {
        val acceptedModifiers: Array<JvmModifier> = getModifiersForTransactional(method)
        val annotation = transactionalAnnotations.asSequence()
            .map { method.getMetaAnnotation(it) }
            .filterNotNull()
            .firstOrNull()

        val shotName = annotation?.qualifiedName?.split(".")?.last() ?: ""
        return OverridingInfo(SpringCoreBundle.message("esprito.implicit.inspection.forMethod.annotated", shotName), acceptedModifiers)
    }

}