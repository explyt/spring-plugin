package com.esprito.spring.core.providers

import com.esprito.spring.core.*
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.util.SpringCoreUtil.beanPsiType
import com.esprito.spring.core.util.SpringCoreUtil.canResolveBeanClass
import com.esprito.spring.core.util.SpringCoreUtil.filterByInheritedTypes
import com.esprito.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.esprito.spring.core.util.SpringCoreUtil.isEqualOrInheritorBeanType
import com.esprito.util.EspritoAnnotationUtil.getMetaAnnotationMemberValues
import com.esprito.util.EspritoPsiUtil.isAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isFinal
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.psiClassType
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.esprito.util.EspritoPsiUtil.returnPsiType
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.lang.properties.IProperty
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import org.jetbrains.uast.*


class SpringBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val processor = getLineMarkerElementProcessor(element) ?: return

        if (processor.isComponentClassOrBeanMethod()) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBean)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { processor.findFieldsAndMethodsWithAutowired() })
                .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.autowired.candidate"))
                .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.autowired.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.autowired.candidate"))
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

    class LineMarkerElementProcessor(
        private val element: PsiElement,
        private val module: Module,
        private val uParent: UElement,
    ) {
        private val springSearchService = SpringSearchService.getInstance(module.project)
        private val autoConfigureProperties = NotNullLazyValue.lazy { getAutoconfigureIPropertiesByStringKey() }

        fun isComponentClassOrBeanMethod(): Boolean {
            if (uParent is UClass && SpringCoreUtil.isSpringBeanCandidateClass(uParent.javaPsi)) {
                val isComponentExpression = uParent.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
                return (isComponentExpression
                        || findTargetClass() in springSearchService.getAllBeansClassesWithAncestors(module))
                        && !dependsOnIncorrectBean(uParent.javaPsi)
            }
            if (uParent is UMethod) {
                return isBeanMethodExpression(uParent.javaPsi)
                        && !dependsOnIncorrectBean(uParent.javaPsi)
                        && !dependsOnIncorrectBean(uParent.javaPsi.containingClass)
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
                val psiClass = psiVariable.parentOfType<PsiClass>() ?: return false
                val isClassIsComponentConstructed =
                    getBeanPsiClassesAnnotatedByComponent(module).any { it.psiClass == psiClass }
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
                        )
                        && !dependsOnIncorrectBean(uMethod.getContainingUClass()?.javaPsi)
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
            if (element !is PsiIdentifier) {
                return null
            }

            return when (uParent) {
                is UMethod -> uParent.returnPsiClass
                is UClass -> uParent.javaPsi
                else -> null
            }
        }

        private fun findTargetType(): PsiType? {
            if (element !is PsiIdentifier) {
                return null
            }

            return element.parentOfType<PsiMethod>()?.returnPsiType  // method annotated by Bean
                ?: element.parentOfType<PsiClass>()?.returnPsiType // class annotated as Component
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

            val allFieldsWithAutowired = allBeans.asSequence().flatMap {
                it.psiClass.allFields.asSequence()
                    .filter { it.isAnnotatedBy(allAutowiredAnnotationsNames) }
                    .filter {
                        targetType == it.type
                                || it.type.canResolveBeanClass(targetClasses)
                                && (targetType !is PsiClassType
                                || it.type.beanPsiType!!.psiClassType?.isEqualOrInheritorBeanType(targetType) == true)
                    }
                    .map { it.navigationElement as? PsiVariable }
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
                    .map { it.navigationElement as? PsiVariable }
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
    }

}
