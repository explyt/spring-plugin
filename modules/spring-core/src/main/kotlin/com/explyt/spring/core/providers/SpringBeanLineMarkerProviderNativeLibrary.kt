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

package com.explyt.spring.core.providers

import com.explyt.spring.core.JavaEeClasses
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.providers.SpringBeanLineMarkerProvider.Companion.isAutowiredFieldExpression
import com.explyt.spring.core.providers.SpringBeanLineMarkerProvider.Companion.isAutowiredMethodExpression
import com.explyt.spring.core.service.NativeSearchService
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.util.SpringCoreUtil.beanPsiType
import com.explyt.spring.core.util.SpringCoreUtil.canResolveBeanClass
import com.explyt.spring.core.util.SpringCoreUtil.filterByBeanPsiType
import com.explyt.spring.core.util.SpringCoreUtil.filterByExactMatch
import com.explyt.spring.core.util.SpringCoreUtil.filterByInheritedTypes
import com.explyt.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.explyt.spring.core.util.SpringCoreUtil.isCandidate
import com.explyt.spring.core.util.SpringCoreUtil.isComponentCandidate
import com.explyt.spring.core.util.SpringCoreUtil.isSpringBeanCandidateClass
import com.explyt.util.ExplytKotlinUtil.mapToSet
import com.explyt.util.ExplytPsiUtil.allSupers
import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.*
import org.jetbrains.uast.*

class SpringBeanLineMarkerProviderNativeLibrary : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uClass = getUParentForIdentifier(element) as? UClass ?: return

        val psiClass = uClass.javaPsi
        if (!isSpringBeanCandidateClass(psiClass)) return
        val libraryBeans = NativeSearchService.getInstance(element.project).getLibraryBeans()
        val targetQualifiedName = psiClass.qualifiedName
        val contextBean = libraryBeans.find { isContextClass(it, targetQualifiedName) }
        val isComponentCandidate = isComponentCandidate(psiClass)

        if (contextBean?.psiMember is PsiMethod) {
            addMethodBeanDeclaration(uClass, contextBean, result)
        }

        if (contextBean == null && !isComponentCandidate) return

        addContextBean(uClass, libraryBeans, contextBean == null && isComponentCandidate, result)
        processMethods(uClass, libraryBeans, result)
        processFields(uClass, libraryBeans, result)
    }

    private fun isContextClass(it: PsiBean, targetQualifiedName: String?): Boolean {
        return if (it.psiMember is PsiClass) {
            it.psiMember.qualifiedName == targetQualifiedName
        } else {
            it.psiMember.containingClass?.qualifiedName == targetQualifiedName
        }
    }

    private fun processFields(
        uClass: UClass,
        libraryBeans: List<PsiBean>,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {

        val fields = uClass.fields.takeIf { it.isNotEmpty() } ?: return
        val allBeansClassesWithAncestors = libraryBeans.flatMapTo(mutableSetOf()) {
            it.psiClass.supers.asSequence() + it.psiClass
        }
        for (uField in fields) {
            val psiField = uField.javaPsi as? PsiField ?: continue
            if (!isAutowiredFieldExpression(psiField)) continue
            if (checkParam(psiField, libraryBeans, allBeansClassesWithAncestors)) {
                val sourcePsi = uField.uastAnchor?.sourcePsi ?: continue
                val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                    .setAlignment(GutterIconRenderer.Alignment.LEFT)
                    .setTargets(NotNullLazyValue.lazy { getBeanDeclarations(uField, libraryBeans) })
                    .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.bean.candidate"))
                    .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.bean.candidate"))
                    .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.bean.candidate"))
                result.add(builder.createLineMarkerInfo(sourcePsi))
            }
        }
    }

    private fun processMethods(
        uClass: UClass,
        libraryBeans: List<PsiBean>,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val beanMethodNames = libraryBeans.asSequence()
            .filter { it.psiMember is PsiMethod && it.psiMember.containingClass?.qualifiedName == uClass.qualifiedName }
            .mapToSet { (it.psiMember as PsiMethod).name }
        for (method in uClass.methods) {
            val psiElement = method.uastAnchor?.sourcePsi ?: continue
            if (method.isConstructor || isAutowiredMethodExpression(method.javaPsi)) {
                checkMethodParameters(method, libraryBeans, result)
                if (beanMethodNames.isEmpty()) return
                continue
            }

            if (!method.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) continue
            if (method.name in beanMethodNames) {
                val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBean)
                    .setAlignment(GutterIconRenderer.Alignment.LEFT)
                    .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithAutowired(null, method, libraryBeans) })
                    .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.autowired.candidate"))
                    .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.autowired.candidate"))
                    .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.autowired.candidate"))
                    .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
                result.add(builder.createLineMarkerInfo(psiElement))
                checkMethodParameters(method, libraryBeans, result)
            }
        }
    }

    private fun checkMethodParameters(
        method: UMethod,
        libraryBeans: List<PsiBean>,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uastParameters = method.uastParameters.takeIf { it.isNotEmpty() } ?: return
        val allBeansClassesWithAncestors = libraryBeans.flatMapTo(mutableSetOf()) {
            it.psiClass.supers.asSequence() + it.psiClass
        }
        for (uParameter in uastParameters) {
            val psiParameter = uParameter.javaPsi as? PsiParameter ?: continue
            if (checkParam(psiParameter, libraryBeans, allBeansClassesWithAncestors)) {
                val sourcePsi = uParameter.uastAnchor?.sourcePsi ?: continue
                val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                    .setAlignment(GutterIconRenderer.Alignment.LEFT)
                    .setTargets(NotNullLazyValue.lazy { getBeanDeclarations(uParameter, libraryBeans) })
                    .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.bean.candidate"))
                    .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.bean.candidate"))
                    .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.bean.candidate"))
                result.add(builder.createLineMarkerInfo(sourcePsi))
            }
        }
    }

    private fun addContextBean(
        uClass: UClass,
        libraryBeans: List<PsiBean>,
        isComponentCandidate: Boolean,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val sourcePsi = uClass.uastAnchor?.sourcePsi ?: return
        val icon = if (isComponentCandidate) SpringIcons.springBeanInactive else SpringIcons.SpringBean
        val text = if (isComponentCandidate)
            SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.autowired.candidate.innactive") else
            SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.autowired.candidate")
        val builder = NavigationGutterIconBuilder.create(icon)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithAutowired(uClass, null, libraryBeans) })
            .setTooltipText(text)
            .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.autowired.candidate"))
            .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title"))
            .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }

    private fun addMethodBeanDeclaration(
        uClass: UClass,
        contextBean: PsiBean,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val sourcePsi = uClass.sourcePsi ?: return
        val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(listOf(contextBean.psiMember))
            .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.bean.candidate"))
            .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.bean.candidate"))
            .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.bean.candidate"))
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }


    private fun findFieldsAndMethodsWithAutowired(
        uClass: UClass?, uMethod: UMethod?, libraryBeans: List<PsiBean>
    ): Collection<PsiElement> {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_BEAN_LIBRARY_USAGE)
        val isArrayType = uMethod?.returnType is PsiArrayType
        val uElement = getUElement(uClass, uMethod)

        val targetClass = SpringSearchUtils.getBeanClass(uElement, isArrayType) ?: return emptyList()
        val targetClasses = targetClass.allSupers()
        val targetType = if (uElement is UMethod) uElement.returnType else null
        val project = targetClass.project

        val allAutowiredAnnotationsNames =
            JavaEeClasses.INJECT.allFqns + JavaEeClasses.RESOURCE.allFqns + SpringCoreClasses.AUTOWIRED

        val nativeSearchService = NativeSearchService.getInstance(project)
        val allBeans = libraryBeans + nativeSearchService.getProjectBeans()

        val allFieldsWithAutowired = allBeans.asSequence()
            .mapNotNull { bean -> bean.psiClass.toUElementOfType<UClass>()?.fields }
            .flatMap { field ->
                field.asSequence()
                    .filter { it.isAnnotatedBy(allAutowiredAnnotationsNames) }
                    .filter { it.isCandidate(targetType, targetClasses, targetClass) }
                    .mapNotNull { it.navigationElement.toUElement() as? UVariable }
            }.toSet()

        val componentBeanNames = allBeans.asSequence()
            .filter { it.psiMember is PsiClass }
            .mapToSet { it.name }
        val allParametersWithAutowired = mutableSetOf<UVariable>()
        allBeans.forEach { bean ->
            val methods = bean.psiClass.toUElementOfType<UClass>()?.methods ?: return@forEach
            allParametersWithAutowired.addAll(
                methods.asSequence()
                    .filter {
                        it.isAnnotatedBy(allAutowiredAnnotationsNames)
                                || it.isAnnotatedBy(SpringCoreClasses.BEAN)
                                || it.isConstructor && bean.name in componentBeanNames
                    }
                    .flatMap { it.parameterList.parameters.asSequence() }
                    .filter { it.isCandidate(targetType, targetClass, targetClasses) }
                    .map { it.navigationElement.toUElement() as? UVariable }
                    .filterNotNull().toSet())
        }

        return allFieldsWithAutowired + allParametersWithAutowired
    }

    private fun getUElement(uClass: UClass?, uMethod: UMethod?): UElement {
        return uClass ?: uMethod ?: throw RuntimeException("No uElement")
    }

    private fun checkParam(
        psiVariable: PsiVariable,
        libraryBeans: List<PsiBean>,
        allBeansClassesWithAncestors: MutableSet<PsiClass>
    ): Boolean {
        if (psiVariable.type.canResolveBeanClass(allBeansClassesWithAncestors, psiVariable.language)) {
            return true
        }
        val componentBeanPsiMethods = libraryBeans.mapNotNull { it.psiMember as? PsiMethod }
        val hasExactType = componentBeanPsiMethods.filterByExactMatch(psiVariable.type).any()
        if (hasExactType) {
            return true
        }
        val beanPsiType = psiVariable.type.beanPsiType
        val foundInheritedTypes = if (beanPsiType != null) {
            componentBeanPsiMethods.filterByBeanPsiType(beanPsiType).any()
        } else {
            componentBeanPsiMethods.filterByInheritedTypes(psiVariable.type, null).any()
        }
        return foundInheritedTypes
    }

    private fun getBeanDeclarations(uVariable: UVariable, libraryBeans: List<PsiBean>): Collection<PsiElement> {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_BEAN_LIBRARY_DECLARATION)
        val sourcePsi = uVariable.sourcePsi ?: return emptyList()
        val language = sourcePsi.language
        val beanPsiType = uVariable.type
        val beanName = uVariable.name ?: return emptyList()
        val qualifierAnnotation = (sourcePsi as? PsiModifierListOwner)?.getQualifierAnnotation()
        return NativeSearchService.getInstance(sourcePsi.project).findActiveBeanDeclarations(
            libraryBeans, beanName, language, beanPsiType, qualifierAnnotation
        )
    }

}
