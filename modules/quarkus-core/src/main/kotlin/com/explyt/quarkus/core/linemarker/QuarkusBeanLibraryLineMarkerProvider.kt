/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.quarkus.core.linemarker

import com.explyt.quarkus.core.QuarkusCoreBundle
import com.explyt.quarkus.core.QuarkusCoreClasses
import com.explyt.quarkus.core.QuarkusCoreIcons
import com.explyt.quarkus.core.QuarkusUtil
import com.explyt.quarkus.core.QuarkusUtil.isCandidateQuarkus
import com.explyt.quarkus.core.service.QuarkusSearchService
import com.explyt.spring.core.JavaEeClasses
import com.explyt.spring.core.providers.SpringBeanLineMarkerProvider
import com.explyt.spring.core.providers.SpringBeanLineMarkerProvider.Companion.isLombokAnnotatedClassFieldExpression
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.explyt.spring.core.util.SpringCoreUtil.isCandidate
import com.explyt.util.ExplytPsiUtil.allSupers
import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.*
import org.jetbrains.uast.*

class QuarkusBeanLibraryLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectSlowLineMarkers(
        elements: List<PsiElement?>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        val element = elements.firstOrNull() ?: return
        if (!QuarkusUtil.isQuarkusProject(element)) return
        val containingFile = element.containingFile
        val virtualFile = containingFile?.virtualFile
        //virtualFile is always null for library classes
        if (virtualFile != null) return

        super.collectSlowLineMarkers(elements, result)
    }

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uClass = getUParentForIdentifier(element) as? UClass ?: return

        val psiClass = uClass.javaPsi
        if (!QuarkusUtil.isBeanCandidateClass(psiClass)) return
        val libraryBeans = QuarkusSearchService.Companion.getInstance(element.project).getLibraryBeans()
        val targetQualifiedName = psiClass.qualifiedName
        val findPsiBean = libraryBeans.find { it.psiClass.qualifiedName == targetQualifiedName }
        if (findPsiBean != null || psiClass.isMetaAnnotatedBy(QuarkusCoreClasses.COMPONENTS_ANNO)) {
            addContextBean(uClass, result)
        }
        if (findPsiBean?.isMember() == true) {
            addProducesBeanDeclaration(uClass, result)
        }

        addContextBean(uClass, result)
        uClass.methods.forEach { processMethod(it, result) }
        uClass.fields.forEach { processField(it, result) }
    }

    private fun isContextClass(it: PsiBean, targetQualifiedName: String?): Boolean {
        return if (it.psiMember is PsiClass) {
            (it.psiMember as PsiClass).qualifiedName == targetQualifiedName
        } else {
            it.psiMember.containingClass?.qualifiedName == targetQualifiedName
        }
    }

    private fun addProducesBeanDeclaration(
        uClass: UClass,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val sourcePsi = uClass.uastAnchor?.sourcePsi ?: return
        val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.BeanDependencies)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { findBeanDeclarations(uClass) })
            .setTooltipText(QuarkusCoreBundle.message("explyt.quarkus.gutter.tooltip.title.choose.bean.candidate"))
            .setPopupTitle(QuarkusCoreBundle.message("explyt.quarkus.gutter.popup.title.choose.bean.candidate"))
            .setEmptyPopupText(QuarkusCoreBundle.message("explyt.quarkus.gutter.notfound.title.choose.bean.candidate"))
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }

    private fun addContextBean(
        uClass: UClass,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (uClass.javaPsi.isMetaAnnotatedBy(QuarkusCoreClasses.INTERCEPTOR.allFqns)) return

        val sourcePsi = uClass.uastAnchor?.sourcePsi ?: return
        val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.Bean)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithInject(null, uClass) })
            .setTooltipText(QuarkusCoreBundle.message("explyt.quarkus.gutter.tooltip.title.choose.autowired.candidate"))
            .setPopupTitle(QuarkusCoreBundle.message("explyt.quarkus.gutter.popup.title.choose.autowired.candidate"))
            .setEmptyPopupText(QuarkusCoreBundle.message("explyt.quarkus.gutter.notfound.title"))
            .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }

    private fun findBeans(module: Module, uElement: UElement): List<PsiBean> {
        val beanClass = QuarkusUtil.getBeanClass(uElement) ?: return emptyList()
        return QuarkusSearchService.Companion.getInstance(module.project).allBeanSequence(module)
            .filter { it.psiClass == beanClass }.toList()
    }

    private fun processField(
        uField: UField,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val psiField = uField.javaPsi as? PsiField ?: return
        if (psiField.isAnnotatedBy(QuarkusCoreClasses.PRODUCES.allFqns)) {
            val sourcePsi = uField.uastAnchor?.sourcePsi ?: return
            val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.Bean)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithInject(uField.type, uField) })
                .setTooltipText(QuarkusCoreBundle.message("explyt.quarkus.gutter.tooltip.title.choose.autowired.candidate"))
                .setPopupTitle(QuarkusCoreBundle.message("explyt.quarkus.gutter.popup.title.choose.autowired.candidate"))
                .setEmptyPopupText(QuarkusCoreBundle.message("explyt.quarkus.gutter.notfound.title.choose.autowired.candidate"))
                .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
            result.add(builder.createLineMarkerInfo(sourcePsi))
            return
        }
        if (!isInjectExpression(psiField) && !isLombokAnnotatedClassFieldExpression(psiField)) return

        val sourcePsi = uField.uastAnchor?.sourcePsi ?: return
        val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.BeanDependencies)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { getBeanDeclarations(uField) })
            .setTooltipText(QuarkusCoreBundle.message("explyt.quarkus.gutter.tooltip.title.choose.bean.candidate"))
            .setPopupTitle(QuarkusCoreBundle.message("explyt.quarkus.gutter.popup.title.choose.bean.candidate"))
            .setEmptyPopupText(QuarkusCoreBundle.message("explyt.quarkus.gutter.notfound.title.choose.bean.candidate"))
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }

    private fun processMethod(
        method: UMethod,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val psiElement = method.uastAnchor?.sourcePsi ?: return
        val project = psiElement.project
        if (isInjectExpression(method.javaPsi)) {
            checkMethodParameters(method, result)
            return
        } else if (method.isConstructor) {
            val beanClass = method.getContainingUClass()?.javaPsi ?: return
            val allBeanSequence = QuarkusSearchService.Companion.getInstance(project).allBeanSequence()
            if (allBeanSequence.none { it.psiClass == beanClass }) return
            checkMethodParameters(method, result)
            return
        }

        if (method.javaPsi.isMetaAnnotatedBy(QuarkusCoreClasses.PRODUCES.allFqns)) {
            val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.Bean)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithInject(method.returnType, method) })
                .setTooltipText(QuarkusCoreBundle.message("explyt.quarkus.gutter.tooltip.title.choose.autowired.candidate"))
                .setPopupTitle(QuarkusCoreBundle.message("explyt.quarkus.gutter.popup.title.choose.autowired.candidate"))
                .setEmptyPopupText(QuarkusCoreBundle.message("explyt.quarkus.gutter.notfound.title.choose.autowired.candidate"))
                .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
            result.add(builder.createLineMarkerInfo(psiElement))

            checkMethodParameters(method, result)
        }
    }

    private fun checkMethodParameters(
        method: UMethod,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        for (uParameter in method.uastParameters) {
            val sourcePsi = uParameter.uastAnchor?.sourcePsi ?: continue
            val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.BeanDependencies)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { getBeanDeclarations(uParameter) })
                .setTooltipText(QuarkusCoreBundle.message("explyt.quarkus.gutter.tooltip.title.choose.bean.candidate"))
                .setPopupTitle(QuarkusCoreBundle.message("explyt.quarkus.gutter.popup.title.choose.bean.candidate"))
                .setEmptyPopupText(QuarkusCoreBundle.message("explyt.quarkus.gutter.notfound.title.choose.bean.candidate"))
            result.add(builder.createLineMarkerInfo(sourcePsi))
        }
    }

    fun isInjectExpression(javaPsi: PsiModifierListOwner): Boolean {
        return javaPsi.isAnnotatedBy(JavaEeClasses.INJECT.allFqns)

    }

    private fun findBeanDeclarations(uClass: UClass): List<PsiElement> {
        val targetClass = QuarkusUtil.getBeanClass(uClass) ?: return emptyList()
        return QuarkusSearchService.Companion.getInstance(uClass.javaPsi.project).allBeanSequence()
            .filter { it.psiClass == targetClass && it.psiClass != it.psiMember }
            .map { it.psiMember }
            .toList()
    }

    private fun getBeanDeclarations(uVariable: UVariable): Collection<PsiElement> {
        val sourcePsi = uVariable.sourcePsi ?: return emptyList()
        val project = sourcePsi.project
        //todo to Quarkus Qualifier
        val qualifierAnnotation = (sourcePsi as? PsiModifierListOwner)?.getQualifierAnnotation()
        val allActiveBeans = QuarkusSearchService.Companion.getInstance(project).allBeanSequence().toList()
        val activeBean = QuarkusSearchService.Companion.getInstance(project)
            .findActiveBeanDeclarations(allActiveBeans, uVariable, null)
        if ((sourcePsi as? PsiModifierListOwner)?.isMetaAnnotatedBy(QuarkusCoreClasses.DELEGATE.allFqns) == true) {
            return activeBean.filter {
                if (it is PsiClass) !it.isMetaAnnotatedBy(QuarkusCoreClasses.DECORATOR.allFqns) else true
            }
        }
        return activeBean
    }

    private fun findFieldsAndMethodsWithInject(
        targetType: PsiType?, uElement: UElement
    ): Collection<PsiElement> {
        val project = uElement.javaPsi?.project ?: return emptyList()
        val targetClass = QuarkusUtil.getBeanClass(uElement) ?: return emptyList()
        val targetClasses = targetClass.allSupers()
        val searchService = QuarkusSearchService.Companion.getInstance(project)
        val componentBeans = searchService.allBeanSequence().filter { !it.isMember() }.toSet()

        val allFieldsWithAutowired = searchService.allBeanSequence()
            .mapNotNull { bean -> bean.psiClass.toUElementOfType<UClass>()?.fields }
            .flatMap { field ->
                field.asSequence()
                    .filter { it.isAnnotatedBy(QuarkusCoreClasses.INJECT.allFqns) }
                    .filter { it.isCandidateQuarkus(targetType, targetClasses, targetClass) }
                    .mapNotNull { it.navigationElement.toUElement() as? UVariable }
            }.toSet()


        val allParametersWithAutowired = mutableSetOf<UVariable>()
        searchService.allBeanSequence().forEach { bean ->
            val methods = bean.psiClass.toUElementOfType<UClass>()?.methods ?: return@forEach
            allParametersWithAutowired.addAll(
                methods.asSequence()
                    .filter {
                        it.isAnnotatedBy(QuarkusCoreClasses.INJECT.allFqns)
                                || it.isAnnotatedBy(QuarkusCoreClasses.PRODUCES.allFqns)
                                || it.isConstructor
                                && bean in componentBeans
                    }
                    .flatMap { it.parameterList.parameters.asSequence() }
                    .filter { it.isCandidate(targetType, targetClass, targetClasses) }
                    .map { it.navigationElement.toUElement() as? UVariable }
                    .filterNotNull().toSet())
        }

        val result = allFieldsWithAutowired + allParametersWithAutowired
        if (uElement is UClass && uElement.javaPsi.isMetaAnnotatedBy(QuarkusCoreClasses.DECORATOR.allFqns)) {
            return result.filter { !it.isMetaAnnotatedBy(QuarkusCoreClasses.DELEGATE.allFqns) }
        }
        return result
    }
}