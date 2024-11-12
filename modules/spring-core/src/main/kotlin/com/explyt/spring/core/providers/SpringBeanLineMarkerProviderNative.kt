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

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.providers.SpringBeanLineMarkerProvider.Companion.isAutowiredFieldExpression
import com.explyt.spring.core.providers.SpringBeanLineMarkerProvider.Companion.isAutowiredMethodExpression
import com.explyt.spring.core.providers.SpringBeanLineMarkerProvider.Companion.isLombokAnnotatedClassFieldExpression
import com.explyt.spring.core.service.NativeSearchService
import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.core.util.SpringCoreUtil.beanPsiType
import com.explyt.spring.core.util.SpringCoreUtil.canResolveBeanClass
import com.explyt.spring.core.util.SpringCoreUtil.filterByBeanPsiType
import com.explyt.spring.core.util.SpringCoreUtil.filterByExactMatch
import com.explyt.spring.core.util.SpringCoreUtil.filterByInheritedTypes
import com.explyt.spring.core.util.SpringCoreUtil.getArrayType
import com.explyt.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.explyt.spring.core.util.SpringCoreUtil.isCandidate
import com.explyt.spring.core.util.SpringCoreUtil.isComponentCandidate
import com.explyt.util.ExplytPsiUtil.allSupers
import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.lang.properties.IProperty
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.*
import org.jetbrains.uast.*

class SpringBeanLineMarkerProviderNative : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val uElement = getUParentForIdentifier(element) ?: return

        if (uElement is UClass) {
            if (!SpringCoreUtil.isSpringBeanCandidateClass(uElement.javaPsi)) return
            val inContextBean = isContextBean(module, uElement)
            val componentCandidate = isComponentCandidate(uElement.javaPsi)
            if (componentCandidate && !inContextBean) {
                addComponentCandidateBean(uElement, module, result)
            } else {
                if (isMethodBean(uElement, inContextBean, componentCandidate)) {
                    addMethodBeanDeclaration(uElement, module, result)
                }
                if (inContextBean) {
                    addContextBean(uElement, module, result)
                }
            }
            if (isAutoConfigurationClass(uElement, module)) {
                val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringFactories)
                    .setAlignment(GutterIconRenderer.Alignment.LEFT)
                    .setTargets(NotNullLazyValue.lazy { findFactoriesMetadataFiles(uElement, module) })
                    .setTooltipText(SpringCoreBundle.message("explyt.spring.factories.gutter.tooltip"))
                    .setPopupTitle(SpringCoreBundle.message("explyt.spring.factories.gutter.popup.title"))
                result.add(builder.createLineMarkerInfo(element))
            }
        } else if (uElement is UField && checkClassOnBeanCandidate(module, uElement.getContainingUClass())) {
            processField(uElement, module, result)
        } else if (uElement is UMethod && checkClassOnBeanCandidate(module, uElement.getContainingUClass())) {
            processMethod(uElement, module, result)
        }
    }

    private fun isContextBean(module: Module, uElement: UElement): Boolean {
        val allBeanClasses = NativeSearchService.getInstance(module.project).getAllBeanClasses(module)
        return SpringSearchUtils.getBeanClass(uElement) in allBeanClasses
    }

    private fun checkClassOnBeanCandidate(module: Module, uClass: UClass?): Boolean {
        val javaPsiClass = uClass?.javaPsi ?: return false
        val isClassIsComponentConstructed = SpringCoreUtil.isSpringBeanCandidateClass(javaPsiClass)
                && isComponentCandidate(javaPsiClass)
        return isClassIsComponentConstructed || isContextBean(module, uClass)
    }

    private fun processField(
        uField: UField,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val psiField = uField.javaPsi as? PsiField ?: return
        if (!isAutowiredFieldExpression(psiField) && !isLombokAnnotatedClassFieldExpression(psiField)) return
        if (checkParam(psiField, module)) {
            val sourcePsi = uField.uastAnchor?.sourcePsi ?: return
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { getBeanDeclarations(uField, module) })
                .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.bean.candidate"))
                .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.bean.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.bean.candidate"))
            result.add(builder.createLineMarkerInfo(sourcePsi))
        }
    }

    private fun processMethods(
        uElement: UClass,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        for (method in uElement.methods) {
            processMethod(method, module, result)
        }
    }

    private fun processMethod(
        method: UMethod,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val psiElement = method.uastAnchor?.sourcePsi ?: return
        if (method.isConstructor || isAutowiredMethodExpression(method.javaPsi)) {
            checkMethodParameters(method, module, result)
            return
        }

        if (!method.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) return
        if (isMethodBean(module, method)) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBean)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithAutowired(null, method, module) })
                .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.autowired.candidate"))
                .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.autowired.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.autowired.candidate"))
                .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
            result.add(builder.createLineMarkerInfo(psiElement))
            checkMethodParameters(method, module, result)
        }
    }

    private fun isMethodBean(module: Module, method: UMethod) =
        NativeSearchService.getInstance(module.project)
            .getAllActiveBeans(module)
            .any { it.psiMember == method.sourcePsi || it.psiMember == method.javaPsi }

    private fun checkMethodParameters(
        method: UMethod,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        for (uParameter in method.uastParameters) {
            val psiParameter = uParameter.javaPsi as? PsiParameter ?: continue
            if (checkParam(psiParameter, module)) {
                val sourcePsi = uParameter.uastAnchor?.sourcePsi ?: continue
                val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                    .setAlignment(GutterIconRenderer.Alignment.LEFT)
                    .setTargets(NotNullLazyValue.lazy { getBeanDeclarations(uParameter, module) })
                    .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.bean.candidate"))
                    .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.bean.candidate"))
                    .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.bean.candidate"))
                result.add(builder.createLineMarkerInfo(sourcePsi))
            }
        }
    }

    private fun addContextBean(
        uClass: UClass,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val sourcePsi = uClass.uastAnchor?.sourcePsi ?: return
        val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBean)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithAutowired(uClass, null, module) })
            .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.autowired.candidate"))
            .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.autowired.candidate"))
            .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title"))
            .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }

    private fun addMethodBeanDeclaration(
        uClass: UClass,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val sourcePsi = uClass.sourcePsi ?: return
        val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { findBeanDeclarations(uClass, module) })
            .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.bean.candidate"))
            .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.bean.candidate"))
            .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.bean.candidate"))
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }

    private fun addComponentCandidateBean(
        uClass: UClass,
        module: Module,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        val sourcePsi = uClass.uastAnchor?.sourcePsi ?: return
        val builder = NavigationGutterIconBuilder.create(SpringIcons.springBeanInactive)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithAutowired(uClass, null, module) })
            .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.autowired.candidate.innactive"))
            .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.autowired.candidate"))
            .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.autowired.candidate"))
            .setTargetRenderer { SpringBeanLineMarkerProvider().getTargetRender() }
        result.add(builder.createLineMarkerInfo(sourcePsi))
    }

    private fun isAutoConfigurationClass(uClass: UClass, module: Module): Boolean {
        val qualifiedName = uClass.qualifiedName ?: return false
        return getAutoconfigureIPropertiesByStringKey(module).any { it.first.contains(qualifiedName) }
    }

    private fun isMethodBean(uElement: UClass, inContextBean: Boolean, componentCandidate: Boolean): Boolean {
        if (!inContextBean) return false
        if (componentCandidate) return false
        val project = uElement.javaPsi.project
        return NativeSearchService.getInstance(project).getSpringMethodBeans()
            .any { uElement.qualifiedName == NativeBootUtils.getBeanTypePsiClass(project, it)?.qualifiedName }
    }

    private fun findFactoriesMetadataFiles(uElement: UClass, module: Module): Collection<PsiElement> {
        val qualifiedName = uElement.qualifiedName ?: return emptyList()
        return getAutoconfigureIPropertiesByStringKey(module).asSequence()
            .filter { it.first.contains(qualifiedName) }
            .map { it.second.psiElement }
            .toList()
    }

    private fun getAutoconfigureIPropertiesByStringKey(module: Module): List<Pair<String, IProperty>> {
        return SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getAllFactoriesMetadataFiles(module)
            .filter { it.key != null && it.value != null }
            .map { (it.key ?: "") + (it.value ?: "") to it }
    }

    private fun findFieldsAndMethodsWithAutowired(
        uClass: UClass?, uMethod: UMethod?, module: Module
    ): Collection<PsiElement> {
        val isArrayType = uMethod?.returnType is PsiArrayType
        val uElement = getUElement(uClass, uMethod)
        val project = module.project

        val targetClass = SpringSearchUtils.getBeanClass(uElement, isArrayType) ?: return emptyList()
        val targetClasses = targetClass.allSupers()
        val targetType = null

        val allAutowiredAnnotations = SpringSearchUtils.getAutowiredFieldAnnotations(module)
        val allAutowiredAnnotationsNames = allAutowiredAnnotations.mapNotNull { it.qualifiedName }

        val nativeSearchService = NativeSearchService.getInstance(project)
        val allBeans = nativeSearchService.getAllActiveBeans(module)

        val allFieldsWithAutowired = allBeans.asSequence()
            .mapNotNull { bean -> bean.psiClass.toUElementOfType<UClass>()?.fields }
            .flatMap { field ->
                field.asSequence()
                    .filter { it.isAnnotatedBy(allAutowiredAnnotationsNames) }
                    .filter { it.isCandidate(targetType, targetClasses, targetClass) }
                    .mapNotNull { it.navigationElement.toUElement() as? UVariable }
            }.toSet()


        val allParametersWithAutowired = mutableSetOf<UVariable>()
        allBeans.forEach { bean ->
            val methods = bean.psiClass.toUElementOfType<UClass>()?.methods ?: return@forEach
            allParametersWithAutowired.addAll(methods.asSequence()
                .filter {
                    it.isAnnotatedBy(allAutowiredAnnotationsNames)
                            || it.isAnnotatedBy(SpringCoreClasses.BEAN)
                            || it.isConstructor
                            && bean in nativeSearchService.getBeanPsiClassesAnnotatedByComponent(module)
                }
                .flatMap { it.parameterList.parameters.asSequence() }
                .filter { targetType == it.type || it.type.canResolveBeanClass(targetClasses, it.language) }
                .map { it.navigationElement.toUElement() as? UVariable }
                .filterNotNull().toSet())
        }

        val allByType = allFieldsWithAutowired + allParametersWithAutowired
        val filteredByName = allByType.filter {
            val beanName = it.name ?: return@filter true
            val beanPsiType = it.type
            val resolvedBeanTargets = nativeSearchService.findActiveBeanDeclarations(
                module, beanName, it.language, beanPsiType, it.getQualifierAnnotation()
            )
            return@filter uElement.javaPsi in resolvedBeanTargets
        }

        return filteredByName.ifEmpty {
            allByType
        }
    }

    private fun getUElement(uClass: UClass?, uMethod: UMethod?): UElement {
        return uClass ?: uMethod ?: throw RuntimeException("No uElement")
    }

    private fun findBeanDeclarations(uClass: UClass, module: Module): List<PsiElement> {
        val targetClass = SpringSearchUtils.getBeanClass(uClass) ?: return emptyList()
        return NativeSearchService.getInstance(uClass.javaPsi.project).getAllActiveBeans(module).asSequence()
            .filter { it.psiClass == targetClass && it.psiClass != it.psiMember }
            .map { it.psiMember }
            .toList()
    }

    private fun checkParam(psiVariable: PsiVariable, module: Module): Boolean {
        val nativeSearchService = NativeSearchService.getInstance(psiVariable.project)
        val allBeansClassesWithAncestors = nativeSearchService.getAllBeansClassesWithAncestors(module)
        if (psiVariable.type.canResolveBeanClass(allBeansClassesWithAncestors, psiVariable.language)) {
            return true
        }
        val componentBeanPsiMethods = nativeSearchService.getComponentBeanPsiMethods(module)
        val hasExactType = componentBeanPsiMethods.filterByExactMatch(psiVariable.type).any()
        if (hasExactType) {
            return true
        }
        val arrayPsiType = psiVariable.type.getArrayType()
        if (arrayPsiType != null) {
            return nativeSearchService.searchArrayComponentPsiClassesByBeanMethods(module).asSequence()
                .mapNotNull { (it.psiMember as? PsiMethod)?.returnType }
                .any { arrayPsiType.isAssignableFrom(it) }
        }
        val beanPsiType = psiVariable.type.beanPsiType
        val foundInheritedTypes = if (beanPsiType != null) {
            componentBeanPsiMethods.filterByBeanPsiType(beanPsiType).any()
        } else {
            componentBeanPsiMethods.filterByInheritedTypes(psiVariable.type, null).any()
        }
        return foundInheritedTypes
    }

    private fun getBeanDeclarations(uVariable: UVariable, module: Module): Collection<PsiElement> {
        val sourcePsi = uVariable.sourcePsi ?: return emptyList()
        val language = sourcePsi.language
        val beanPsiType = uVariable.type
        val beanName = uVariable.name ?: return emptyList()
        val qualifierAnnotation = (sourcePsi as? PsiModifierListOwner)?.getQualifierAnnotation()
        val activeBean = NativeSearchService.getInstance(module.project).findActiveBeanDeclarations(
            module, beanName, language, beanPsiType, qualifierAnnotation
        )
        if (activeBean.isNotEmpty()) {
            return activeBean
        }
        val arrayType = beanPsiType.getArrayType()
        if (arrayType != null) {
            val arrayBeans = getArrayBeans(arrayType, module)
            if (arrayBeans.isNotEmpty()) return arrayBeans
        }
        return emptyList()
    }

    private fun getArrayBeans(arrayPsiType: PsiArrayType, module: Module): List<PsiMember> {
        return NativeSearchService.getInstance(module.project).searchArrayComponentPsiClassesByBeanMethods(module)
            .asSequence()
            .filter {
                it.psiMember is PsiMethod && it.psiMember.returnType != null
                        && arrayPsiType.isAssignableFrom(it.psiMember.returnType!!)
            }
            .map { it.psiMember }.toList()
    }
}
