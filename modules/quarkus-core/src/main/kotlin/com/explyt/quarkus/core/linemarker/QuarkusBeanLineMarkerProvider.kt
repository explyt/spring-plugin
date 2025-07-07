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
import com.explyt.spring.core.util.SpringCoreUtil.isCandidate
import com.explyt.util.ExplytKotlinUtil.mapToList
import com.explyt.util.ExplytPsiUtil.allSupers
import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.*
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.*

class QuarkusBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectSlowLineMarkers(
        elements: List<PsiElement?>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        val element = elements.firstOrNull() ?: return
        if (!QuarkusUtil.isQuarkusModule(element)) return

        super.collectSlowLineMarkers(elements, result)
    }

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val uElement = getUParentForIdentifier(element) ?: return

        if (uElement is UClass) {
            if (!QuarkusUtil.isBeanCandidateClass(uElement.javaPsi)) return
            val findBeans = findBeans(module, uElement)
            val inContextBean = findBeans.isNotEmpty()

            val isDirectlyMemberDeclaration = findBeans.any { it.isMember() }
            if (isDirectlyMemberDeclaration) {
                addProducesBeanDeclaration(uElement, module, result)
            }
            if (inContextBean) {
                addContextBean(uElement, module, result)
                processConstructorMethods(uElement, module, result)
            } else if (!isDirectlyMemberDeclaration && isInheritorMemberDeclaration(module, uElement)) {
                addContextBean(uElement, module, result)
                addProducesBeanDeclaration(uElement, module, result)
                processConstructorMethods(uElement, module, result)
            }

        } else if (uElement is UField) {
            processField(uElement, module, result)
        } else if (uElement is UMethod) {
            processMethod(uElement, module, result)
        }
    }

    private fun isInheritorMemberDeclaration(module: Module, uElement: UClass): Boolean {
        return QuarkusSearchService.getInstance(module.project).searchBeansByProject().asSequence()
            .filter { it.isMember() }
            .filter { it.psiClass.qualifiedName != null }
            .any { InheritanceUtil.isInheritor(uElement.javaPsi, it.psiClass.qualifiedName!!) }
    }

    private fun addProducesBeanDeclaration(
        uClass: UClass,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val sourcePsi = uClass.uastAnchor?.sourcePsi ?: return
        val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.BeanDependencies)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { findBeanDeclarations(uClass, module) })
            .setTooltipText(QuarkusCoreBundle.message("explyt.quarkus.gutter.tooltip.title.choose.bean.candidate"))
            .setPopupTitle(QuarkusCoreBundle.message("explyt.quarkus.gutter.popup.title.choose.bean.candidate"))
            .setEmptyPopupText(QuarkusCoreBundle.message("explyt.quarkus.gutter.notfound.title.choose.bean.candidate"))
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }

    private fun addContextBean(
        uClass: UClass,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (uClass.javaPsi.isMetaAnnotatedBy(QuarkusCoreClasses.INTERCEPTOR.allFqns)) return

        val sourcePsi = uClass.uastAnchor?.sourcePsi ?: return
        val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.Bean)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithInject(null, uClass, module) })
            .setTooltipText(QuarkusCoreBundle.message("explyt.quarkus.gutter.tooltip.title.choose.autowired.candidate"))
            .setPopupTitle(QuarkusCoreBundle.message("explyt.quarkus.gutter.popup.title.choose.autowired.candidate"))
            .setEmptyPopupText(QuarkusCoreBundle.message("explyt.quarkus.gutter.notfound.title"))
            .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }

    private fun findBeans(module: Module, uElement: UElement): List<PsiBean> {
        val beanClass = QuarkusUtil.getBeanClass(uElement) ?: return emptyList()
        return QuarkusSearchService.getInstance(module.project).allBeanSequence(module)
            .filter { it.psiClass == beanClass }.toList()
    }

    private fun processConstructorMethods(
        uElement: UClass,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (uElement.javaPsi.language != KotlinLanguage.INSTANCE) return
        for (method in uElement.methods) {
            if (method.isConstructor) {
                processMethod(method, module, result)
            }
        }
    }

    private fun processField(
        uField: UField,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val psiField = uField.javaPsi as? PsiField ?: return
        if (psiField.isAnnotatedBy(QuarkusCoreClasses.PRODUCES.allFqns)) {
            val sourcePsi = uField.uastAnchor?.sourcePsi ?: return
            val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.Bean)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithInject(uField.type, uField, module) })
                .setTooltipText(QuarkusCoreBundle.message("explyt.quarkus.gutter.tooltip.title.choose.autowired.candidate"))
                .setPopupTitle(QuarkusCoreBundle.message("explyt.quarkus.gutter.popup.title.choose.autowired.candidate"))
                .setEmptyPopupText(QuarkusCoreBundle.message("explyt.quarkus.gutter.notfound.title.choose.autowired.candidate"))
                .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
            result.add(builder.createLineMarkerInfo(sourcePsi))
            return
        }
        if (uField.isStatic || (uField.hasInitializer() && !isInjectExpression(psiField))) return
        if (!isInjectExpression(psiField) && !isLombokAnnotatedClassFieldExpression(psiField)) return

        val sourcePsi = uField.uastAnchor?.sourcePsi ?: return
        val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.BeanDependencies)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { getBeanDeclarations(uField, module) })
            .setTooltipText(QuarkusCoreBundle.message("explyt.quarkus.gutter.tooltip.title.choose.bean.candidate"))
            .setPopupTitle(QuarkusCoreBundle.message("explyt.quarkus.gutter.popup.title.choose.bean.candidate"))
            .setEmptyPopupText(QuarkusCoreBundle.message("explyt.quarkus.gutter.notfound.title.choose.bean.candidate"))
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }

    private fun processMethod(
        method: UMethod,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val psiElement = method.uastAnchor?.sourcePsi ?: return
        if (isInjectExpression(method.javaPsi)) {
            checkMethodParameters(method, module, result)
            return
        } else if (method.isConstructor) {
            val beanClass = method.getContainingUClass()?.javaPsi ?: return
            val allBeanSequence = QuarkusSearchService.getInstance(module.project).allBeanSequence(module)
            if (allBeanSequence.none { it.psiClass == beanClass }) return
            checkMethodParameters(method, module, result)
            return
        }

        if (method.javaPsi.isMetaAnnotatedBy(QuarkusCoreClasses.PRODUCES.allFqns)) {
            val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.Bean)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithInject(method.returnType, method, module) })
                .setTooltipText(QuarkusCoreBundle.message("explyt.quarkus.gutter.tooltip.title.choose.autowired.candidate"))
                .setPopupTitle(QuarkusCoreBundle.message("explyt.quarkus.gutter.popup.title.choose.autowired.candidate"))
                .setEmptyPopupText(QuarkusCoreBundle.message("explyt.quarkus.gutter.notfound.title.choose.autowired.candidate"))
                .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
            result.add(builder.createLineMarkerInfo(psiElement))

            checkMethodParameters(method, module, result)
        }
    }

    private fun checkMethodParameters(
        method: UMethod,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        for (uParameter in method.uastParameters) {
            val sourcePsi = uParameter.uastAnchor?.sourcePsi ?: continue
            val builder = NavigationGutterIconBuilder.create(QuarkusCoreIcons.BeanDependencies)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { getBeanDeclarations(uParameter, module) })
                .setTooltipText(QuarkusCoreBundle.message("explyt.quarkus.gutter.tooltip.title.choose.bean.candidate"))
                .setPopupTitle(QuarkusCoreBundle.message("explyt.quarkus.gutter.popup.title.choose.bean.candidate"))
                .setEmptyPopupText(QuarkusCoreBundle.message("explyt.quarkus.gutter.notfound.title.choose.bean.candidate"))
            result.add(builder.createLineMarkerInfo(sourcePsi))
        }
    }

    fun isInjectExpression(javaPsi: PsiModifierListOwner): Boolean {
        return javaPsi.isAnnotatedBy(JavaEeClasses.INJECT.allFqns)

    }

    private fun findBeanDeclarations(uClass: UClass, module: Module): List<PsiElement> {
        val targetClass = QuarkusUtil.getBeanClass(uClass) ?: return emptyList()
        val directBeanDeclaration = QuarkusSearchService.getInstance(uClass.javaPsi.project).allBeanSequence(module)
            .filter { it.isMember() }
            .filter { it.psiClass == targetClass }
            .mapToList { it.psiMember }
        val inheritorBeanDeclaration = QuarkusSearchService.getInstance(module.project)
            .searchBeansByProject().asSequence()
            .filter { it.isMember() }
            .filter { it.psiClass != targetClass }
            .filter { it.psiClass.qualifiedName != null }
            .filter { InheritanceUtil.isInheritor(uClass.javaPsi, it.psiClass.qualifiedName!!) }
            .filter { it.psiMember.toUElement()?.asRenderString()?.contains(it.psiClass.qualifiedName!!) == true }
            .mapToList { it.psiMember }
        return directBeanDeclaration + inheritorBeanDeclaration
    }

    private fun getBeanDeclarations(uVariable: UVariable, module: Module): Collection<PsiElement> {
        val javaPsi = uVariable.javaPsi ?: return emptyList()
        val allActiveBeans = QuarkusSearchService.getInstance(module.project).allBeanSequence(module).toList()
        val activeBean = QuarkusSearchService.getInstance(module.project)
            .findActiveBeanDeclarations(allActiveBeans, uVariable)
        if ((javaPsi as? PsiModifierListOwner)?.isMetaAnnotatedBy(QuarkusCoreClasses.DELEGATE.allFqns) == true) {
            return activeBean.filter {
                if (it is PsiClass) !it.isMetaAnnotatedBy(QuarkusCoreClasses.DECORATOR.allFqns) else true
            }
        }
        return activeBean
    }

    private fun findFieldsAndMethodsWithInject(
        targetType: PsiType?, uElement: UElement, module: Module
    ): Collection<PsiElement> {
        val project = module.project
        val targetClass = QuarkusUtil.getBeanClass(uElement) ?: return emptyList()
        val targetClasses = targetClass.allSupers()
        val searchService = QuarkusSearchService.getInstance(project)
        val componentBeans = searchService.allBeanSequence(module).filter { !it.isMember() }.toSet()

        val allFieldsWithAutowired = searchService.allBeanSequence(module)
            .mapNotNull { bean -> bean.psiClass.toUElementOfType<UClass>()?.fields }
            .flatMap { field ->
                field.asSequence()
                    .filter { it.isAnnotatedBy(QuarkusCoreClasses.INJECT.allFqns) }
                    .filter { it.isCandidateQuarkus(targetType, targetClasses, targetClass) }
                    .mapNotNull { it.navigationElement.toUElement() as? UVariable }
            }.toSet()


        val allParametersWithAutowired = mutableSetOf<UVariable>()
        searchService.allBeanSequence(module).forEach { bean ->
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