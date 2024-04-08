package com.esprito.spring.core.providers

import com.esprito.spring.core.*
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.util.SpringCoreUtil.beanPsiType
import com.esprito.spring.core.util.SpringCoreUtil.canResolveBeanClass
import com.esprito.spring.core.util.SpringCoreUtil.filterByInheritedTypes
import com.esprito.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.esprito.spring.core.util.SpringCoreUtil.isComponentCandidate
import com.esprito.spring.core.util.SpringCoreUtil.isEqualOrInheritorBeanType
import com.esprito.util.EspritoAnnotationUtil.getMetaAnnotationMemberValues
import com.esprito.util.EspritoPsiUtil.isAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isFinal
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.psiClassType
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.intellij.codeInsight.AnnotationUtil
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
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val processor = getLineMarkerElementProcessor(element) ?: return

        if (processor.isComponentClassOrBeanMethod()) {
            val builder = NavigationGutterIconBuilder.create(getComponentIcom(processor))
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { processor.findFieldsAndMethodsWithAutowired() })
                .setTooltipText(getTooltipMessage(processor))
                .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.autowired.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.autowired.candidate"))
                .setTargetRenderer { getTargetRender() }
            result.add(builder.createLineMarkerInfo(element))
        } else if (processor.isClassForBeanMethod()) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { processor.findBeanDeclarations() })
                .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.bean.candidate"))
                .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.bean.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.bean.candidate"))
            result.add(builder.createLineMarkerInfo(element))
        } else if (processor.isFieldOrAutowiredParameter()) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { processor.getBeanDeclarations() })
                .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.bean.candidate"))
                .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.bean.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.bean.candidate"))

            result.add(builder.createLineMarkerInfo(element))
        }
        if (processor.isAutoConfigurationClass()) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringFactories)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { processor.findFactoriesMetadataFiles() })
                .setTooltipText(SpringCoreBundle.message("esprito.spring.factories.gutter.tooltip"))
                .setPopupTitle(SpringCoreBundle.message("esprito.spring.factories.gutter.popup.title"))
            result.add(builder.createLineMarkerInfo(element))
        }
    }

    private fun getLineMarkerElementProcessor(element: PsiElement): LineMarkerElementProcessor? {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
        val uParent = getUParentForIdentifier(element) ?: return null
        return LineMarkerElementProcessor(element, module, uParent)
    }

    private fun getTooltipMessage(processor: LineMarkerElementProcessor) =
        if (processor.inSpringContextClass == true)
            SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.autowired.candidate")
        else
            SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.autowired.candidate.innactive")

    private fun getComponentIcom(processor: LineMarkerElementProcessor) =
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
            }
            if (uParent is UMethod) {
                result = isBeanMethodExpression(uParent.javaPsi)
                        && !dependsOnIncorrectBean(uParent.javaPsi)
                        && !dependsOnIncorrectBean(uParent.javaPsi.containingClass)
            }
            if (result) {
                inSpringContextClass = inSpringContext()
            }
            return result
        }

        private fun inSpringContext() = findTargetClass() in springSearchService.getAllActiveBeans(module)

        fun isClassForBeanMethod(): Boolean {
            if (uParent is UClass && SpringCoreUtil.isSpringBeanCandidateClass(uParent.javaPsi)) {
                val targetClass = findTargetClass() ?: return false
                val allBeans = springSearchService.getActiveBeansClasses(module)
                return allBeans.any { it.psiClass == targetClass && it.psiClass != it.psiMember }
            }
            return false
        }

        private fun dependsOnIncorrectBean(member: PsiMember?): Boolean {
            val beanNames = springSearchService.getAllBeanByNames(module)

            return member?.getMetaAnnotationMemberValues(SpringCoreClasses.DEPENDS_ON)
                ?.any {
                    !beanNames.contains(
                        AnnotationUtil.getStringAttributeValue(it)
                    )
                } ?: false
        }

        private fun isAutowiredFieldExpression(javaPsi: PsiField): Boolean {
            return javaPsi.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED)
                    || javaPsi.isAnnotatedBy(JavaEeClasses.INJECT.allFqns)
                    || javaPsi.isAnnotatedBy(JavaEeClasses.RESOURCE.allFqns)
        }

        private fun isLombokAnnotatedClassFieldExpression(psiField: PsiField): Boolean {
            return psiField.containingClass?.let {
                it.isMetaAnnotatedBy(LombokClasses.ALL_ARGS_CONSTRUCTOR)
                        || it.isMetaAnnotatedBy(LombokClasses.REQUIRED_ARGS_CONSTRUCTOR) && psiField.isFinal
            } == true
        }

        private fun isAutowiredMethodExpression(javaPsi: PsiMethod): Boolean {
            return javaPsi.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED)
                    || javaPsi.isAnnotatedBy(JavaEeClasses.INJECT.allFqns)
                    || javaPsi.isAnnotatedBy(JavaEeClasses.RESOURCE.allFqns)
        }

        private fun isBeanMethodExpression(javaPsi: PsiMethod): Boolean {
            return javaPsi.isMetaAnnotatedBy(SpringCoreClasses.BEAN)
        }

        fun isFieldOrAutowiredParameter(): Boolean {
            fun checkParam(psiVariable: PsiVariable): Boolean = with(springSearchService) {
                val javaPsiClass = uParent.getContainingUClass()?.javaPsi ?: return false
                val isClassIsComponentConstructed = SpringCoreUtil.isSpringBeanCandidateClass(javaPsiClass)
                        && isComponentCandidate(javaPsiClass)

                if (!isClassIsComponentConstructed) {
                    return false
                }
                val allBeansClassesWithAncestors = getAllBeansClassesWithAncestors(module)
                if (psiVariable.type.canResolveBeanClass(allBeansClassesWithAncestors)) {
                    return true
                }
                val componentBeanPsiMethods = getComponentBeanPsiMethods(module)
                val hasExactType = componentBeanPsiMethods.filterByExactMatch(psiVariable.type).isNotEmpty()
                if (hasExactType) {
                    return true
                }
                val beanPsiType = psiVariable.type.beanPsiType
                val foundInheritedTypes = if (beanPsiType != null) {
                    componentBeanPsiMethods.filterByBeanPsiType(psiVariable.type, beanPsiType).isNotEmpty()
                } else {
                    componentBeanPsiMethods.filterByInheritedTypes(psiVariable.type, null).isNotEmpty()
                }
                if (foundInheritedTypes) {
                    return true
                }

                return false
            }

            if (uParent is UField) {
                val psiField = uParent.javaPsi.takeIf { it is PsiField }?.let { it as PsiField } ?: return false
                if (!checkParam(psiField)) {
                    return false
                }
                return (isAutowiredFieldExpression(psiField) || isLombokAnnotatedClassFieldExpression(psiField))
                        && !dependsOnIncorrectBean(psiField.containingClass)
            }
            if (uParent is UParameter) {
                val uMethod = uParent.uastParent ?: return false
                if (uMethod is UMethod) {
                    val psiParameter =
                        uParent.javaPsi.takeIf { it is PsiParameter }?.let { it as PsiParameter } ?: return false
                    if (!checkParam(psiParameter)) {
                        return false
                    }
                    val psiMethod = uMethod.javaPsi
                    return (uMethod.isConstructor
                            || isAutowiredMethodExpression(psiMethod)
                            || isBeanMethodExpression(psiMethod)
                            ) && !dependsOnIncorrectBean(uMethod.getContainingUClass()?.javaPsi)
                }
            }
            return false
        }

        fun getBeanDeclarations(): Collection<PsiElement> {
            val uField = element.toUElement()?.getParentOfType(UVariable::class.java) ?: return emptyList()
            val beanPsiType = uField.type
            val beanName = uField.name ?: return emptyList()
            val qualifierAnnotation = uField.getQualifierAnnotation()

            return springSearchService.findActiveBeanDeclarations(
                module, beanName, beanPsiType, qualifierAnnotation
            )
        }

        private fun findTargetClass(): PsiClass? {
            return when (uParent) {
                is UMethod -> uParent.returnPsiClass
                is UClass -> uParent.javaPsi
                else -> null
            }
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
            if (uParent !is UClass) return emptyList()
            val qualifiedName = uParent.qualifiedName ?: return emptyList()
            return autoConfigureProperties.value.asSequence()
                .filter { it.first.contains(qualifiedName) }
                .map { it.second.psiElement }
                .toList()
        }

        fun findFieldsAndMethodsWithAutowired(): Collection<PsiElement> {
            val targetClass = findTargetClass() ?: return emptyList()
            val targetClasses = targetClass.supers.toSet() + targetClass
            val targetType = findTargetType()

            val allAutowiredAnnotations = springSearchService.getAutowiredFieldAnnotations(module)
            val allAutowiredAnnotationsNames = allAutowiredAnnotations.mapNotNull { it.qualifiedName }

            val allBeans = springSearchService.getActiveBeansClasses(module)

            val allFieldsWithAutowired = allBeans.asSequence().flatMap { bean ->
                bean.psiClass.allFields.asSequence()
                    .filter { it.isAnnotatedBy(allAutowiredAnnotationsNames) }
                    .filter {
                        targetType == it.type
                                || it.type.canResolveBeanClass(targetClasses)
                                && (targetType !is PsiClassType
                                || it.type.beanPsiType!!.psiClassType?.isEqualOrInheritorBeanType(targetType) == true)
                    }
                    .map { it.navigationElement.toUElement() as? UVariable }
                    .filterNotNull()
            }.toSet()

            val allParametersWithAutowired = allBeans.asSequence().flatMap { bean ->
                bean.psiClass.allMethods.asSequence()
                    .filter {
                        it.isAnnotatedBy(allAutowiredAnnotationsNames)
                                || it.isAnnotatedBy(SpringCoreClasses.BEAN)
                                || it.isConstructor
                                && bean in springSearchService.getBeanPsiClassesAnnotatedByComponent(module)
                    }
                    .flatMap { it.parameterList.parameters.asSequence() }
                    .filter { targetType == it.type || it.type.canResolveBeanClass(targetClasses) }
                    .map { it.navigationElement.toUElement() as? UVariable }
                    .filterNotNull()
            }.toSet()

            val allByType = allFieldsWithAutowired + allParametersWithAutowired
            val filteredByName = allByType.filter {
                val beanName = it.name ?: return@filter true
                val beanPsiType = it.type
                val resolvedBeanTargets = springSearchService.findActiveBeanDeclarations(
                    module, beanName, beanPsiType, it.getQualifierAnnotation()
                )
                return@filter uParent.javaPsi in resolvedBeanTargets
            }

            return filteredByName.ifEmpty {
                allByType
            }
        }

        fun findBeanDeclarations(): List<PsiElement> {
            if (uParent is UClass) {
                val targetClass = findTargetClass() ?: return emptyList()
                return springSearchService.getActiveBeansClasses(module).asSequence()
                    .filter { it.psiClass == targetClass && it.psiClass != it.psiMember }
                    .map { it.psiMember }
                    .toList()
            }
            return emptyList()
        }
    }

    /**
     * code from /com/intellij/codeInsight/navigation/util.kt:targetPresentation
     */
    private fun getTargetRender(): PsiTargetPresentationRenderer<PsiElement> {
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
    }
}
