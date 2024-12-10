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

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.JavaEeClasses
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.quickfix.AddQualifierQuickFix
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.core.util.SpringCoreUtil.getArrayType
import com.explyt.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.explyt.spring.core.util.SpringCoreUtil.targetClass
import com.explyt.util.ExplytPsiUtil.getMetaAnnotation
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isOptional
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.explyt.util.ExplytPsiUtil.returnPsiType
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.*
import com.intellij.util.applyIf
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField


class SpringBeanIncorrectAutowiringInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkField(
        uField: UField,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor> {
        val field = uField.javaPsi as? PsiField ?: return ProblemDescriptor.EMPTY_ARRAY
        val module = ModuleUtilCore.findModuleForPsiElement(field) ?: return ProblemDescriptor.EMPTY_ARRAY
        if (!field.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED)) return ProblemDescriptor.EMPTY_ARRAY

        val arrayType = uField.returnPsiType?.getArrayType()
        if (arrayType != null) {
            val isArrayBeanExist = SpringSearchServiceFacade.getInstance(manager.project)
                .searchArrayComponentPsiClassesByBeanMethods(module).asSequence()
                .mapNotNull { (it.psiMember as? PsiMethod)?.returnType }
                .any { arrayType.isAssignableFrom(it) }
            if (isArrayBeanExist) return emptyArray()
        }

        val psiClass = field.containingClass ?: return emptyArray()
        if (isBeanExist(module, psiClass)) {
            return getProblemAutowired(module, field, manager, isOnTheFly)
        }
        return ProblemDescriptor.EMPTY_ARRAY
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor> {
        val aClass = uClass.javaPsi
        val module = ModuleUtilCore.findModuleForPsiElement(aClass) ?: return ProblemDescriptor.EMPTY_ARRAY
        if (!SpringCoreUtil.isSpringBeanCandidateClass(aClass)) return ProblemDescriptor.EMPTY_ARRAY

        var problems = emptyArray<ProblemDescriptor>()
        if (!isBeanExist(module, aClass)) {
            problems += getProblemByClassWithoutComponent(aClass, manager, isOnTheFly)
        } else {
            problems += getProblemConstructors(aClass, manager, isOnTheFly)
            val methods = aClass.allMethods
                .filter { it.isInjectOrAutowiredByRequiredTrue() }
            for (method in methods) {
                val params = method.parameterList.parameters
                for (parameter in params.toList()) {
                    problems += getProblemAutowired(module, parameter, manager, isOnTheFly)
                }
                problems += getProblemByMethodWithoutParams(method, params, manager, isOnTheFly)
            }
        }

        return problems
    }

    private fun getProblemConstructors(
        aClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        var problems = emptyArray<ProblemDescriptor>()

        val uniqueConstructors = aClass.constructors.distinctBy { it.text }

        if (uniqueConstructors.size > 1) {
            if (isConfigPropertyWithEmptyConstructor(aClass, uniqueConstructors)) return problems

            val autowiredConstructors = uniqueConstructors
                .filter { it.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED) || it.isMetaAnnotatedBy(JavaEeClasses.INJECT.allFqns) }

            if (autowiredConstructors.isEmpty() && uniqueConstructors.isNotEmpty()) {

                if (aClass.nameIdentifier != null) {
                    val psiElement = getIdentifyingElement(aClass)?.navigationElement ?: return problems
                    problems += manager.createProblemDescriptor(
                        psiElement,
                        SpringCoreBundle.message("explyt.spring.inspection.constructor.without.autowiring"),
                        isOnTheFly,
                        emptyArray(),
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
            } else if (autowiredConstructors.size > 1) {
                autowiredConstructors
                    .mapNotNull { getIdentifyingElement(it)?.navigationElement }
                    .forEach {
                        problems += manager.createProblemDescriptor(
                            it,
                            SpringCoreBundle.message("explyt.spring.inspection.constructor.multiple.autowiring"),
                            isOnTheFly,
                            emptyArray(),
                            ProblemHighlightType.GENERIC_ERROR
                        )
                    }
            }
        }
        return problems
    }

    private fun isConfigPropertyWithEmptyConstructor(aClass: PsiClass, uniqueConstructors: List<PsiMethod>): Boolean {
        return aClass.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION_PROPERTIES)
                && uniqueConstructors.any { it.parameterList.isEmpty }
    }

    private fun PsiType.isMultipleBean(module: Module): Boolean {
        return SpringSearchService.getInstance(module.project).run { isMultipleBean(module) }
    }

    private fun getProblemAutowired(
        module: Module,
        element: PsiJvmModifiersOwner,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        if (element !is PsiVariable) return ProblemDescriptor.EMPTY_ARRAY

        val psiType = element.type
        val nameClass = (psiType.resolveBeanPsiClass ?: psiType.resolvedPsiClass)?.name ?: return ProblemDescriptor.EMPTY_ARRAY
        val problemElement = getIdentifyingElement(element) ?: return ProblemDescriptor.EMPTY_ARRAY

        val springSearchService = SpringSearchServiceFacade.getInstance(module.project)
        val beanDeclarations = springSearchService
            .findActiveBeanDeclarations(
                module,
                element.name ?: "",
                element.getLanguage(),
                psiType,
                element.getQualifierAnnotation()
            )

        if (psiType.isOptional || element.isAutowiredByRequiredTrue() == false) {
            if (beanDeclarations.size > 1) {
                val actualPsiType = psiType.applyIf(psiType.isOptional) { (psiType as PsiClassType).parameters.firstOrNull() ?: PsiTypes.nullType() }
                if (!actualPsiType.isMultipleBean(module)) {
                    return arrayOf(
                        manager.createProblemDescriptor(
                            problemElement,
                            getWarningMessageInheritor(module, beanDeclarations,
                                SpringCoreBundle.message(
                                    "explyt.spring.inspection.bean.autowired.optional.too-many",
                                    nameClass
                                )
                            ),
                            isOnTheFly,
                            emptyArray(),
                            ProblemHighlightType.WARNING
                        )
                    )
                }
            }
            return ProblemDescriptor.EMPTY_ARRAY
        }

        if (beanDeclarations.isEmpty()) {
            return arrayOf(
                manager.createProblemDescriptor(
                    problemElement,
                    getMessageTypeNone(psiType, nameClass),
                    isOnTheFly, emptyArray(), ProblemHighlightType.GENERIC_ERROR
                )
            )
        }

        if (beanDeclarations.size == 1
            || beanDeclarations.count { it.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY) } == 1
        ) {
            return ProblemDescriptor.EMPTY_ARRAY
        }

        if (SpringCoreClasses.QUALIFIERS.any { element.isMetaAnnotatedBy(it) }) {
            return getProblemQualifier(module, element, manager, isOnTheFly)
        }
        var problems = emptyArray<ProblemDescriptor>()
        if (beanDeclarations.isEmpty() || !psiType.isMultipleBean(module)) {
            problems += manager.createProblemDescriptor(
                problemElement,
                getWarningMessageInheritor(module, beanDeclarations,
                    SpringCoreBundle.message("explyt.spring.inspection.bean.autowired.too-many", nameClass)
                ),
                AddQualifierQuickFix(SpringCoreClasses.QUALIFIER, problemElement),
                ProblemHighlightType.GENERIC_ERROR, isOnTheFly
            )
        }

        return problems
    }

    private fun getProblemQualifier(
        module: Module,
        element: PsiVariable,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        if (element.isAutowiredByRequiredTrue() == false) return ProblemDescriptor.EMPTY_ARRAY
        if (element.type.resolvedPsiClass == null) return ProblemDescriptor.EMPTY_ARRAY

        val qualifier = element.getQualifierAnnotation() ?: return ProblemDescriptor.EMPTY_ARRAY
        val beanDeclarations = SpringSearchServiceFacade.getInstance(module.project)
            .findActiveBeanDeclarations(module, element.name ?: "", element.language, element.type, qualifier)

        if (beanDeclarations.isNotEmpty()) {
            return ProblemDescriptor.EMPTY_ARRAY
        }


        return arrayOf(
            manager.createProblemDescriptor(
                qualifier,
                SpringCoreBundle.message(
                    "explyt.spring.inspection.bean.class.unknown.qualifier.bean",
                    qualifier.text
                ),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR
            )
        )

    }

    private fun isBeanExist(module: Module, psiClass: PsiClass): Boolean {
        if (SpringCoreUtil.isComponentCandidate(psiClass)) return true

        return SpringSearchServiceFacade.getInstance(module.project).getAllActiveBeans(module).asSequence()
            .filter { it.psiClass.qualifiedName == psiClass.qualifiedName }
            .toList().isNotEmpty()
    }

    private fun getProblemByMethodWithoutParams(
        method: PsiMethod,
        params: Array<out PsiParameter>,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        var problems = emptyArray<ProblemDescriptor>()
        val identifier = getIdentifyingElement(method)
        if (method.isAutowiredByRequiredTrue() == true && params.isEmpty() && identifier != null) {
            problems += manager.createProblemDescriptor(
                identifier,
                SpringCoreBundle.message("explyt.spring.inspection.method.without.autowiring"),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR
            )
        }
        return problems
    }

    private fun getProblemByClassWithoutComponent(
        aClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        var problems = emptyArray<ProblemDescriptor>()

        val fields = aClass.fields
            .filter { it.isInjectOrAutowiredByRequiredTrue() }
            .map { it as PsiNameIdentifierOwner }.toSet()
        val methods = aClass.methods
            .filter { it.isInjectOrAutowiredByRequiredTrue() }
            .map { it as PsiNameIdentifierOwner }.toSet()
        val elements = fields + methods

        if (elements.isNotEmpty() && (aClass.nameIdentifier != null)) {
            elements.map {
                val identifier = getIdentifyingElement(it) ?: return@map
                problems += manager.createProblemDescriptor(
                    identifier,
                    SpringCoreBundle.message("explyt.spring.inspection.class.without.component"),
                    isOnTheFly,
                    emptyArray(),
                    ProblemHighlightType.GENERIC_ERROR
                )
            }
        }
        return problems
    }

    private fun getWarningMessageInheritor(
        module: Module,
        beanCandidates: List<PsiMember>,
        errorText: String
    ): String {
        val message = StringBuilder()
        message.append("<html><table><tr><td>")
        message.append(errorText)
        message.append("</td></tr><tr><td><table><tr><td valign='top'> Beans: </td><td>")
        beanCandidates.map { bean -> "${bean.resolveBeanName(module)} {@link ${bean.targetClass?.name}${(bean as? PsiMethod)?.name?.let { "#$it" } ?: ""}}" }.sorted().joinTo(message, " <br>")
        message.append("</td></tr></table></td></tr></table></html>")
        return message.toString()
    }

    private fun checkBeans(beanDeclarations: List<PsiMember>): Boolean {
        when {
            beanDeclarations.size == 1 -> return true
            beanDeclarations.count { it.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY) } == 1 -> return true
        }
        return false
    }

    private fun PsiModifierListOwner.isInjectOrAutowiredByRequiredTrue(): Boolean {
        if (isAutowiredByRequiredTrue() == true) {
            return true
        }
        if (isMetaAnnotatedBy(SpringCoreClasses.LOAD_BALANCED)) {
            return false
        }
        return isMetaAnnotatedBy(SpringCoreClasses.QUALIFIERS) || isMetaAnnotatedBy(SpringCoreClasses.BOOTSTRAP_WITH)
    }

    private fun PsiModifierListOwner.isAutowiredByRequiredTrue(): Boolean? {
        if (isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED)) {
            return getMetaAnnotation(SpringCoreClasses.AUTOWIRED)?.let {
                AnnotationUtil.getBooleanAttributeValue(it, "required")
            }
        }
        return null
    }

    private fun getMessageTypeNone(psiType: PsiType, className: String): String {
        if (psiType.presentableText != className) {
            return SpringCoreBundle.message(
                "explyt.spring.inspection.bean.autowired.type.none.or",
                className,
                psiType.presentableText
            )
        }
        return SpringCoreBundle.message("explyt.spring.inspection.bean.autowired.type.none", psiType.presentableText)
    }

    private fun getIdentifyingElement(namedElement: PsiNameIdentifierOwner): PsiElement? {
        return (namedElement.toSourcePsi() as? PsiNameIdentifierOwner)?.identifyingElement
    }

}