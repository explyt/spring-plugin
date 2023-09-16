package com.esprito.spring.core.providers

import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.util.SpringCoreUtil.canResolveBeanClass
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanName
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanNameByPsiAnnotations
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.util.EspritoPsiUtil.isAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isEqualOrInheritor
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
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
                .setTargets(NotNullLazyValue.lazy { processor.getBeanComponentsTargets() })
                .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.bean.candidate"))
                .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.bean.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.bean.candidate"))

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

        fun isComponentClassOrBeanMethod(): Boolean {
            if (uParent is UClass && SpringCoreUtil.isSpringBeanCandidateClass(uParent.javaPsi)) {
                val isComponentExpression = uParent.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
                return isComponentExpression || findTargetClass() in springSearchService.getAllBeansClassesWithAncestors(module)
            }
            if (uParent is UMethod) {
                return isBeanMethodExpression(uParent.javaPsi)
            }
            return false
        }

        private fun isAutowiredFieldExpression(javaPsi: PsiField): Boolean {
            return javaPsi.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED)
                    || javaPsi.isAnnotatedBy(JavaEeClasses.INJECT.allFqns)
                    || javaPsi.isAnnotatedBy(JavaEeClasses.RESOURCE.allFqns)
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
            fun checkParam(psiVariable: PsiVariable): Boolean {
                val psiClass = psiVariable.parentOfType<PsiClass>() ?: return false
                val isClassIsComponentConstructed = springSearchService.getBeanPsiClassesAnnotatedByComponent(module).any { it.psiClass == psiClass }
                if (!isClassIsComponentConstructed) {
                    return false
                }
                val hasResolvableBeanType = psiVariable.type.canResolveBeanClass(springSearchService.getAllBeansClassesWithAncestors(module))
                if (!hasResolvableBeanType) {
                    return false
                }
                return true
            }

            if (uParent is UField) {
                val psiField = uParent.javaPsi.takeIf { it is PsiField }?.let { it as PsiField } ?: return false
                if (!checkParam(psiField)) {
                    return false
                }
                return isAutowiredFieldExpression(psiField)
            }
            if (uParent is UParameter) {
                val uParameterParent = uParent.parent?.parent ?: return false
                if (uParameterParent is PsiMethod) {
                    val psiParameter = uParent.javaPsi.takeIf { it is PsiParameter }?.let { it as PsiParameter } ?: return false
                    if (!checkParam(psiParameter)) {
                        return false
                    }
                    return uParameterParent.isConstructor || isAutowiredMethodExpression(uParameterParent) || isBeanMethodExpression(uParameterParent)
                }
            }
            return false
        }

        fun getBeanComponentsTargets(): Collection<PsiElement> {
            val psiField = element.parentOfType<PsiField>()
            if (psiField != null) {
                val strongBeanName = psiField.resolveBeanName
                val beanName = strongBeanName ?: psiField.name
                val beanPsiClass = psiField.type.resolveBeanPsiClass

                return resolvedBeanTargets(strongBeanName, beanName, beanPsiClass)
            }
            val psiParameter = element.parentOfType<PsiParameter>() ?: return emptyList()
            val strongBeanName = psiParameter.resolveBeanName
            val beanName = strongBeanName ?: psiParameter.name
            val beanPsiClass = psiParameter.type.resolveBeanPsiClass

            return resolvedBeanTargets(strongBeanName, beanName, beanPsiClass)
        }

        private fun resolvedBeanTargets(strongBeanName: String?, beanName: String, beanPsiClass: PsiClass?): List<PsiElement> {
            val componentPsiBeans = springSearchService.getBeanPsiClassesAnnotatedByComponent(module)
            val methodsPsiBeans = springSearchService.getComponentBeanPsiMethods(module)

            val byNameBeanMethods = methodsPsiBeans.filter { it.resolveBeanName == beanName }
            val byNameComponents = componentPsiBeans.filter { it.name == beanName }.map { it.psiClass }
            val resultByName = (byNameBeanMethods + byNameComponents).filter {
                if (beanPsiClass == null) {
                    return@filter true
                }
                val targetClass = when (it) {
                    is PsiMethod -> it.returnType?.resolvedPsiClass
                    is PsiClass -> it
                    else -> null
                } ?: return@filter true

                return@filter targetClass.isEqualOrInheritor(beanPsiClass)
            }
            if (strongBeanName != null || resultByName.size == 1) {
                return resultByName
            }

            if (beanPsiClass == null) {
                return resultByName
            }

            val byTypeBeanMethods = methodsPsiBeans.filter { it.returnPsiClass?.isEqualOrInheritor(beanPsiClass) == true }
            val byTypeComponents = componentPsiBeans.filter { it.psiClass.isEqualOrInheritor(beanPsiClass) }.map { it.psiClass }
            val resultByType = byTypeBeanMethods + byTypeComponents

            if (resultByName.isNotEmpty()) {
                return resultByName.filter { it in resultByType }
            }
            return resultByType
        }


        private fun findTargetClass(): PsiClass? {
            if (element !is PsiIdentifier) {
                return null
            }

            return element.parentOfType<PsiMethod>()?.returnPsiClass  // method annotated by Bean
                ?: element.parentOfType<PsiClass>() // class annotated as Component
        }

        private fun findTargetBeanName(): String? {
            if (element !is PsiIdentifier) {
                return null
            }

            return element.parentOfType<PsiMethod>()?.resolveBeanName  // method annotated by Bean
                ?: element.parentOfType<PsiClass>()?.resolveBeanNameByPsiAnnotations(
                    SpringSearchService.getInstance(module.project).getComponentClassAnnotations(module)
                ) // class annotated as Component
        }

        fun findFieldsAndMethodsWithAutowired(): Collection<PsiElement> {
            val targetClass = findTargetClass() ?: return emptyList()
            val targetClasses = targetClass.supers.toSet() + targetClass
            val targetBeanName = findTargetBeanName()

            val allAutowiredAnnotations = springSearchService.getAutowiredFieldAnnotations(module)
            val allAutowiredAnnotationsNames = allAutowiredAnnotations.mapNotNull { it.qualifiedName }

            val allBeans = springSearchService.getAllBeansClasses(module)

            val allFieldsWithAutowired = allBeans.asSequence().flatMap {
                it.psiClass.allFields.asSequence()
                    .filter { it.isAnnotatedBy(allAutowiredAnnotationsNames) }
                    .filter { it.type.canResolveBeanClass(targetClasses) }
            }.toSet()

            val allParametersWithAutowired = allBeans.asSequence().flatMap { bean ->
                bean.psiClass.allMethods.asSequence()
                    .filter {
                        it.isAnnotatedBy(allAutowiredAnnotationsNames)
                                || it.isAnnotatedBy(SpringCoreClasses.BEAN)
                                || it.isConstructor && bean in springSearchService.getBeanPsiClassesAnnotatedByComponent(module)
                    }
                    .flatMap { it.parameterList.parameters.asSequence() }
                    .filter { it.type.canResolveBeanClass(targetClasses) }
            }.toSet()

            val allByType = allFieldsWithAutowired + allParametersWithAutowired
            val filteredByName = allByType.filter {
                val strongBeanName = it.resolveBeanName
                val beanName = strongBeanName ?: (it as PsiNamedElement).name
                val beanPsiClass = it.type.resolveBeanPsiClass
                if (beanName == null) {
                    return@filter true
                }
                val resolvedBeanTargets = resolvedBeanTargets(strongBeanName, beanName, beanPsiClass)
                return@filter uParent.javaPsi in resolvedBeanTargets
            }

            return filteredByName.ifEmpty {
                allByType
            }
        }
    }

}
