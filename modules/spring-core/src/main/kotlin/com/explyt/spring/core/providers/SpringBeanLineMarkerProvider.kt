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

import com.explyt.spring.core.*
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.core.util.SpringCoreUtil.getArrayType
import com.explyt.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.explyt.spring.core.util.SpringCoreUtil.isCandidate
import com.explyt.spring.core.util.SpringCoreUtil.isComponentCandidate
import com.explyt.util.ExplytAnnotationUtil.getMetaAnnotationMemberValues
import com.explyt.util.ExplytPsiUtil.allSupers
import com.explyt.util.ExplytPsiUtil.isAbstract
import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isFinal
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isPrivate
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.LOG
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.fileStatusAttributes
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.diagnostic.PluginException
import com.intellij.ide.util.ModuleRendererFactory
import com.intellij.ide.util.PlatformModuleRendererFactory
import com.intellij.lang.properties.IProperty
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.*
import com.intellij.util.TextWithIcon
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.uast.*
import java.util.regex.Pattern

class SpringBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        val module = getModule(elements)
        if (module == null) {
            SpringBeanLineMarkerProviderNativeLibrary().collectSlowLineMarkers(elements, result)
        } else if (isExternalProjectExist(elements)) {
            SpringBeanLineMarkerProviderNative().collectSlowLineMarkers(elements, result)
        } else {
            super.collectSlowLineMarkers(elements, result)
        }
    }

    private fun isExternalProjectExist(elements: MutableList<out PsiElement>): Boolean {
        val project = elements.firstOrNull()?.project ?: return false
        return SpringSearchServiceFacade.isExternalProjectExist(project)
    }

    private fun getModule(elements: MutableList<out PsiElement>): Module? {
        return elements.firstOrNull()
            ?.let { ModuleUtilCore.findModuleForPsiElement(it) }
    }

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val processor = getLineMarkerElementProcessor(element) ?: return

        if (processor.isComponentClassOrBeanMethod()) {
            val builder = NavigationGutterIconBuilder.create(getComponentIcon(processor))
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { processor.findFieldsAndMethodsWithAutowired() })
                .setTooltipText(getTooltipMessage(processor))
                .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.autowired.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title"))
                .setTargetRenderer { getTargetRender() }
            result.add(builder.createLineMarkerInfo(element))
        } else if (processor.isClassForBeanMethod()) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { processor.findBeanDeclarations() })
                .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.bean.candidate"))
                .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.bean.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.bean.candidate"))
            result.add(builder.createLineMarkerInfo(element))
        } else if (processor.isFieldOrAutowiredParameter()) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { processor.getBeanDeclarations() })
                .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.bean.candidate"))
                .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.bean.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.bean.candidate"))

            result.add(builder.createLineMarkerInfo(element))
        }
        if (processor.isAutoConfigurationClass()) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringFactories)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { processor.findFactoriesMetadataFiles() })
                .setTooltipText(SpringCoreBundle.message("explyt.spring.factories.gutter.tooltip"))
                .setPopupTitle(SpringCoreBundle.message("explyt.spring.factories.gutter.popup.title"))
            result.add(builder.createLineMarkerInfo(element))
        }
    }

    private fun getLineMarkerElementProcessor(element: PsiElement): LineMarkerElementProcessor? {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
        val uParent = getUParentForIdentifier(element) ?: return null
        if (uParent is UClass || uParent is UMethod || uParent is UField || uParent is UParameter) {
            return LineMarkerElementProcessor(element, module, uParent)
        }
        return null
    }

    private fun getTooltipMessage(processor: LineMarkerElementProcessor) =
        if (processor.inSpringContextClass == true)
            SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.autowired.candidate")
        else
            SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.autowired.candidate.innactive")

    private fun getComponentIcon(processor: LineMarkerElementProcessor) =
        if (processor.inSpringContextClass == true)
            SpringIcons.SpringBean else SpringIcons.springBeanInactive


    class LineMarkerElementProcessor(
        private val element: PsiElement,
        private val module: Module,
        private val uParent: UElement,
    ) {
        private val springSearchService = SpringSearchService.getInstance(module.project)
        private val autoConfigureProperties = NotNullLazyValue.lazy { getAutoconfigureIPropertiesByStringKey() }

        var inSpringContextClass: Boolean? = null

        fun isComponentClassOrBeanMethod(): Boolean {
            var result = false
            if (uParent is UClass && SpringCoreUtil.isSpringBeanCandidateClass(uParent.javaPsi)) {
                result = isComponentCandidate(uParent.javaPsi) && !dependsOnIncorrectBean(uParent.javaPsi)
                if (!result && hasSpringInterfaces(uParent)) {
                    result = springSearchService.isInSpringContext(uParent, module)
                }
            }
            if (uParent is UMethod) {
                result = isBeanMethodExpression(uParent.javaPsi)
                        && !dependsOnIncorrectBean(uParent.javaPsi)
                        && !dependsOnIncorrectBean(uParent.javaPsi.containingClass)
            }
            if (result) {
                inSpringContextClass = springSearchService.isInSpringContext(uParent, module)
            }
            return result
        }

        private fun hasSpringInterfaces(uParent: UClass): Boolean {
            val interfaces = uParent.javaPsi.interfaces.takeIf { it.isNotEmpty() } ?: return false
            return interfaces.any { it.qualifiedName?.startsWith(SpringProperties.BASE_SPRING_PACKAGE) == true }
        }

        fun isClassForBeanMethod(): Boolean {
            if (uParent is UClass && SpringCoreUtil.isSpringBeanCandidateClass(uParent.javaPsi)) {
                val targetClass = SpringSearchUtils.getBeanClass(uParent) ?: return false
                val allBeans = springSearchService.searchAllBeanLight(module)
                return allBeans.any { it.psiClass == targetClass && it.psiClass != it.psiMember }
            }
            return false
        }

        private fun dependsOnIncorrectBean(member: PsiMember?): Boolean {
            val beanNames = springSearchService.getAllBeanByNamesLight(module)

            return member?.getMetaAnnotationMemberValues(SpringCoreClasses.DEPENDS_ON)
                ?.any {
                    !beanNames.contains(
                        AnnotationUtil.getStringAttributeValue(it)
                    )
                } ?: false
        }

        private fun isBeanMethodExpression(javaPsi: PsiMethod): Boolean {
            return javaPsi.isMetaAnnotatedBy(SpringCoreClasses.BEAN)
        }

        fun isFieldOrAutowiredParameter(): Boolean {
            if (uParent is UField) {
                if (!springSearchService.isBeanCacheable(uParent.getContainingUClass())) return false
                val psiField = uParent.javaPsi.takeIf { it is PsiField }?.let { it as PsiField } ?: return false
                return (isAutowiredFieldExpression(psiField) || isLombokAnnotatedClassFieldExpression(psiField))
                        && !dependsOnIncorrectBean(psiField.containingClass)
            }

            if (uParent is UParameter) {
                if (!springSearchService.isBeanCacheable(uParent.getContainingUClass())) return false
                val uMethod = (uParent.uastParent as? UMethod)?.takeIf { filterMethod(it) } ?: return false
                val javaPsiClass = uMethod.getContainingUClass()?.javaPsi ?: return false
                if (javaPsiClass.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION_PROPERTIES)) return false
                val psiMethod = uMethod.javaPsi
                return ((uMethod.isConstructor && isComponent(uMethod))
                        || isAutowiredMethodExpression(psiMethod)
                        || isBeanMethodExpression(psiMethod)
                        ) && !dependsOnIncorrectBean(javaPsiClass)
            }
            return false
        }

        private fun filterMethod(it: UMethod): Boolean {
            if (it.isPrivate) return false
            if (it.isAbstract) return false
            if (it.isConstructor) return true
            return it.uAnnotations.isNotEmpty()
        }

        fun getBeanDeclarations(): Collection<PsiElement> {
            StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_BEAN_DECLARATION)
            val uField = element.toUElement()?.getParentOfType(UVariable::class.java) ?: return emptyList()
            val beanPsiType = uField.type
            val beanName = uField.name ?: return emptyList()
            val qualifierAnnotation = uField.getQualifierAnnotation()

            val activeBean = springSearchService.findActiveBeanDeclarations(
                module, beanName, element.language, beanPsiType, qualifierAnnotation
            )
            if (activeBean.isNotEmpty()) {
                return activeBean
            }
            val arrayType = beanPsiType.getArrayType()
            if (arrayType != null) {
                val arrayBeans = getArrayBeans(arrayType)
                if (arrayBeans.isNotEmpty()) return arrayBeans
            }
            return emptyList()
        }

        private fun getArrayBeans(arrayPsiType: PsiArrayType): List<PsiMember> {
            return springSearchService.searchArrayComponentPsiClassesByBeanMethods(module).asSequence()
                .filter {
                    it.psiMember is PsiMethod && it.psiMember.returnType != null
                            && arrayPsiType.isAssignableFrom(it.psiMember.returnType!!)
                }
                .map { it.psiMember }.toList()
        }

        private fun findTargetType(): PsiType? {
            return if (uParent is UMethod) uParent.returnType else null
        }

        private fun getAutoconfigureIPropertiesByStringKey(): List<Pair<String, IProperty>> {
            return SpringConfigurationPropertiesSearch.getInstance(element.project)
                .getAllFactoriesMetadataFiles(module)
                .filter { it.key != null && it.value != null }
                .map { (it.key ?: "") + (it.value ?: "") to it }
        }

        fun isAutoConfigurationClass(): Boolean {
            if (uParent is UClass) {
                val qualifiedName = uParent.qualifiedName ?: return false
                return autoConfigureProperties.value.any { it.first.contains(qualifiedName) }
            }
            return false
        }

        fun findFactoriesMetadataFiles(): Collection<PsiElement> {
            StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_FACTORIES_METADATA_FIND)
            if (uParent !is UClass) return emptyList()
            val qualifiedName = uParent.qualifiedName ?: return emptyList()
            return autoConfigureProperties.value.asSequence()
                .filter { it.first.contains(qualifiedName) }
                .map { it.second.psiElement }
                .toList()
        }

        private fun isComponent(uMethod: UMethod): Boolean {
            val uClass = uMethod.getContainingUClass() ?: return false
            if (!SpringCoreUtil.isSpringBeanCandidateClass(uClass.javaPsi)) return false
            if (isComponentCandidate(uClass.javaPsi)) {
                return true
            }
            return springSearchService.isInSpringContext(uParent, module)
        }

        fun findFieldsAndMethodsWithAutowired(): Collection<PsiElement> {
            StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_BEAN_USAGE)
            val isArrayType = uParent is UMethod && uParent.returnType is PsiArrayType
            val targetClass = SpringSearchUtils.getBeanClass(uParent, isArrayType) ?: return emptyList()
            val targetClasses = targetClass.allSupers()
            val targetType = findTargetType()

            val allAutowiredAnnotations = SpringSearchUtils.getAutowiredFieldAnnotations(module)
            val allAutowiredAnnotationsNames = allAutowiredAnnotations.mapNotNull { it.qualifiedName }

            val allBeans = springSearchService.getActiveBeansClasses(module) +
                    springSearchService.getDependentBeanPsiClassesAnnotatedByComponent(module)

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
                allParametersWithAutowired.addAll(
                    methods.asSequence()
                        .filter {
                            it.isAnnotatedBy(allAutowiredAnnotationsNames)
                                    || it.isAnnotatedBy(SpringCoreClasses.BEAN)
                                    || it.isConstructor
                                    && bean in springSearchService.getBeanPsiClassesAnnotatedByComponent(module)
                        }
                        .flatMap { it.parameterList.parameters.asSequence() }
                        .filter { it.isCandidate(targetType, targetClass, targetClasses) }
                        .map { it.navigationElement.toUElement() as? UVariable }
                        .filterNotNull().toSet())
            }

            val allByType = allFieldsWithAutowired + allParametersWithAutowired
            val filteredByName = allByType.filter {
                val beanName = it.name ?: return@filter true
                val beanPsiType = it.type
                val resolvedBeanTargets = springSearchService.findActiveBeanDeclarations(
                    module, beanName, it.language, beanPsiType, it.getQualifierAnnotation()
                )
                return@filter uParent.javaPsi in resolvedBeanTargets
            }

            return filteredByName.ifEmpty {
                allByType
            }
        }

        fun findBeanDeclarations(): List<PsiElement> {
            if (uParent is UClass) {
                val targetClass = SpringSearchUtils.getBeanClass(uParent) ?: return emptyList()
                return springSearchService.getActiveBeansClasses(module).asSequence()
                    .filter { it.psiClass == targetClass && it.psiClass != it.psiMember }
                    .map { it.psiMember }
                    .toList()
            }
            return emptyList()
        }
    }

    fun getTargetRender(): PsiTargetPresentationRenderer<PsiElement> {
        return object : PsiTargetPresentationRenderer<PsiElement>() {

            override fun getPresentation(element: PsiElement): TargetPresentation {
                val project = element.project
                val file = element.containingFile?.virtualFile
                val itemPresentation = (element as? NavigationItem)?.presentation
                val presentableText: String = itemPresentation?.presentableText
                    ?: (element as? PsiNamedElement)?.name
                    ?: element.text
                    ?: run {
                        presentationError(element)
                        element.toString()
                    }

                val moduleTextWithIcon = getModuleTextWithIcon(element)
                val containerText = itemPresentation?.getContainerText() ?: getOptionContainerText(file, element)
                return TargetPresentation
                    .builder(presentableText)
                    .backgroundColor(file?.let { VfsPresentationUtil.getFileBackgroundColor(project, file) })
                    .icon(element.getIcon(Iconable.ICON_FLAG_VISIBILITY or Iconable.ICON_FLAG_READ_STATUS))
                    .containerText(containerText, file?.let { fileStatusAttributes(project, file) })
                    .locationText(moduleTextWithIcon?.text, moduleTextWithIcon?.icon)
                    .presentation()
            }
        }
    }

    private fun getOptionContainerText(file: VirtualFile?, element: PsiElement): String {
        val uElement = if (element is UElement) element else element.toUElement()
        val methodName = uElement?.getParentOfType<UMethod>()?.let { getElementPresentationName(it, file) }
        return methodName ?: (file?.name + ":" + element.getLineNumber())
    }

    private fun getElementPresentationName(it: UMethod, file: VirtualFile?): String {
        val className = it.javaPsi.containingClass?.name ?: file?.name
        return if (className == null) it.name else (className + "#" + it.name)
    }

    private fun presentationError(element: PsiElement) {
        val instance = (element as? PomTargetPsiElement)?.target ?: element
        val clazz = instance.javaClass
        LOG.error(PluginException.createByClass("${clazz.name} cannot be presented", null, clazz))
    }

    private fun ItemPresentation.getContainerText(): String? {
        val locationString = locationString ?: return null
        val matcher = CONTAINER_PATTERN.matcher(locationString)
        return if (matcher.matches()) matcher.group(2) else locationString
    }

    fun getModuleTextWithIcon(value: Any?): TextWithIcon? {
        val factory = ModuleRendererFactory.findInstance(value)
        if (factory is PlatformModuleRendererFactory) {
            return null
        }
        return factory.getModuleTextWithIcon(value)
    }

    companion object {
        private val CONTAINER_PATTERN = Pattern.compile("(\\(in |\\()?([^)]*)(\\))?")

        fun isLombokAnnotatedClassFieldExpression(psiField: PsiField): Boolean {
            return psiField.containingClass?.let {
                it.isMetaAnnotatedBy(LombokClasses.ALL_ARGS_CONSTRUCTOR)
                        || it.isMetaAnnotatedBy(LombokClasses.REQUIRED_ARGS_CONSTRUCTOR) && psiField.isFinal
            } == true
        }

        fun isAutowiredFieldExpression(javaPsi: PsiField): Boolean {
            return javaPsi.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED)
                    || javaPsi.isAnnotatedBy(JavaEeClasses.INJECT.allFqns)
                    || javaPsi.isAnnotatedBy(JavaEeClasses.RESOURCE.allFqns)
                    || javaPsi.isAnnotatedBy(SpringCoreClasses.MOCK_BEAN)
                    || javaPsi.isAnnotatedBy(SpringCoreClasses.SPY_BEAN)
        }

        fun isAutowiredMethodExpression(javaPsi: PsiMethod): Boolean {
            return javaPsi.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED)
                    || javaPsi.isAnnotatedBy(JavaEeClasses.INJECT.allFqns)
                    || javaPsi.isAnnotatedBy(JavaEeClasses.RESOURCE.allFqns)
        }
    }
}
