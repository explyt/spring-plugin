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
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
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
import java.util.function.Supplier

class SpringBeanLineMarkerProviderNativeLibrary : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uClass = getUParentForIdentifier(element) as? UClass ?: return
        val psiClass = uClass.javaPsi
        if (!isSpringBeanCandidateClass(psiClass)) return
        val isComponentCandidate = isComponentCandidate(psiClass)
        val project = element.project
        val libraryBeans = NativeSearchService.getInstance(project).getAllProjectNodesLibraryBeans()
        if (libraryBeans.isEmpty()) {
            val beanSupplier = { SpringSearchService.getInstance(project).getAllActiveBeans().toList() }
            if (isComponentCandidate) {
                addContextBean(uClass, false, result, beanSupplier)
            }
            processMethods(uClass, result, beanSupplier)
            processFields(uClass, result, beanSupplier)
            return
        }
        val targetQualifiedName = psiClass.qualifiedName
        val contextBean = libraryBeans.find { isContextClass(it, targetQualifiedName) }

        if (contextBean?.psiMember is PsiMethod) {
            addMethodBeanDeclaration(uClass, contextBean, result)
        }

        if (contextBean == null && !isComponentCandidate) return

        addContextBean(uClass, contextBean == null, result) { libraryBeans }

        if (psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION_PROPERTIES)) return
        processMethodsNative(uClass, result) { libraryBeans }
        processFields(uClass, result) { libraryBeans }
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
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
        supplier: Supplier<List<PsiBean>>
    ) {
        val fields = uClass.fields.takeIf { it.isNotEmpty() } ?: return
        for (uField in fields) {
            val psiField = uField.javaPsi as? PsiField ?: continue
            if (!isAutowiredFieldExpression(psiField)) continue

            val sourcePsi = uField.uastAnchor?.sourcePsi ?: continue
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { getBeanDeclarations(uField, supplier.get()) })
                .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.bean.candidate"))
                .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.bean.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.bean.candidate"))
            result.add(builder.createLineMarkerInfo(sourcePsi))
        }
    }

    private fun processMethodsNative(
        uClass: UClass,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
        beanSupplier: Supplier<List<PsiBean>>
    ) {
        val libraryBeans = beanSupplier.get()
        val beanMethodNames = libraryBeans.asSequence()
            .filter { it.psiMember is PsiMethod && it.psiMember.containingClass?.qualifiedName == uClass.qualifiedName }
            .mapToSet { (it.psiMember as PsiMethod).name }
        for (method in uClass.methods) {
            val psiElement = method.uastAnchor?.sourcePsi ?: continue
            if (method.isConstructor || isAutowiredMethodExpression(method.javaPsi)) {
                checkMethodParameters(method, result, beanSupplier)
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
                checkMethodParameters(method, result, beanSupplier)
            }
        }
    }

    private fun processMethods(
        uClass: UClass,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
        beanSupplier: Supplier<List<PsiBean>>
    ) {

        for (method in uClass.methods) {
            val psiElement = method.uastAnchor?.sourcePsi ?: continue
            if (method.isConstructor || isAutowiredMethodExpression(method.javaPsi)) {
                checkMethodParameters(method, result, beanSupplier)
                continue
            }

            if (!method.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) continue

            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBean)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy {
                    findFieldsAndMethodsWithAutowired(null, method, beanSupplier.get())
                })
                .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.autowired.candidate"))
                .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.autowired.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.autowired.candidate"))
                .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
            result.add(builder.createLineMarkerInfo(psiElement))
            checkMethodParameters(method, result, beanSupplier)
        }
    }


    private fun checkMethodParameters(
        method: UMethod,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
        beanSupplier: Supplier<List<PsiBean>>
    ) {
        val uastParameters = method.uastParameters.takeIf { it.isNotEmpty() } ?: return

        for (uParameter in uastParameters) {
            val sourcePsi = uParameter.uastAnchor?.sourcePsi ?: continue
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { getBeanDeclarations(uParameter, beanSupplier.get()) })
                .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.bean.candidate"))
                .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.bean.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.bean.candidate"))
            result.add(builder.createLineMarkerInfo(sourcePsi))
        }
    }

    private fun addContextBean(
        uClass: UClass,
        isComponentCandidate: Boolean,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
        beanSupplier: Supplier<List<PsiBean>>
    ) {
        val sourcePsi = uClass.uastAnchor?.sourcePsi ?: return
        val icon = if (isComponentCandidate) SpringIcons.springBeanInactive else SpringIcons.SpringBean
        val text = if (isComponentCandidate)
            SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.autowired.candidate.innactive") else
            SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.autowired.candidate")
        val builder = NavigationGutterIconBuilder.create(icon)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithAutowired(uClass, null, beanSupplier.get()) })
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
        val projectBeans = nativeSearchService.getProjectBeans()
        val allBeans = if (projectBeans.isNotEmpty()) libraryBeans + projectBeans
        else SpringSearchService.getInstance(project).getAllActiveBeans()

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
