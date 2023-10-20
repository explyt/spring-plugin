package com.esprito.spring.core.inspections

import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.inspections.quickfix.AddQualifierQuickFix
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.util.SpringCoreUtil.canBeMoreThanOneBean
import com.esprito.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanName
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.spring.core.util.SpringCoreUtil.targetClass
import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isOptional
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType


class SpringBeanIncorrectAutowiringInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkField(
        field: PsiField,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor> {
        val module = ModuleUtilCore.findModuleForPsiElement(field) ?: return ProblemDescriptor.EMPTY_ARRAY
        if (!field.isInjectOrAutowiredByRequiredTrue()) return ProblemDescriptor.EMPTY_ARRAY

        if (isBeanExist(module, field.containingClass) && SpringCoreUtil.existComponentScan(module)) {
            return getProblemAutowired(module, field, manager, isOnTheFly)
        }
        return ProblemDescriptor.EMPTY_ARRAY
    }

    override fun checkClass(
        aClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor> {
        val module = ModuleUtilCore.findModuleForPsiElement(aClass) ?: ProblemDescriptor.EMPTY_ARRAY
        if (!SpringCoreUtil.existComponentScan(module as Module)) return ProblemDescriptor.EMPTY_ARRAY
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

        if (aClass.constructors.size > 1) {
            val autowiredConstructors = aClass.constructors
                .filter { it.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED) || it.isMetaAnnotatedBy(JavaEeClasses.INJECT.allFqns) }

            if (autowiredConstructors.isEmpty() && aClass.constructors.isNotEmpty()) {
                if (aClass.nameIdentifier != null) {
                    val psiElement = aClass.nameIdentifier!!.navigationElement
                    problems += manager.createProblemDescriptor(
                        psiElement,
                        SpringCoreBundle.message("esprito.spring.inspection.constructor.without.autowiring"),
                        isOnTheFly,
                        emptyArray(),
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
            } else if (autowiredConstructors.size > 1) {
                autowiredConstructors
                    .mapNotNull { it.nameIdentifier?.navigationElement }
                    .forEach {
                        problems += manager.createProblemDescriptor(
                            it,
                            SpringCoreBundle.message("esprito.spring.inspection.constructor.multiple.autowiring"),
                            isOnTheFly,
                            emptyArray(),
                            ProblemHighlightType.GENERIC_ERROR
                        )
                    }
            }
        }
        return problems
    }

    private fun getProblemAutowired(
        module: Module,
        element: PsiJvmModifiersOwner,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        if (element !is PsiVariable) return ProblemDescriptor.EMPTY_ARRAY

        val psiType = element.type
        val nameClass = psiType.resolveBeanPsiClass?.name ?: return ProblemDescriptor.EMPTY_ARRAY
        val problemElement =
            (element as? PsiNameIdentifierOwner)?.identifyingElement ?: return ProblemDescriptor.EMPTY_ARRAY

        val beanDeclarations = SpringSearchService.getInstance(module.project)
            .findActiveBeanDeclarations(module, element.name ?: "", element.type, element.getQualifierAnnotation())

        if (beanDeclarations.isEmpty()) {
            if (psiType.isOptional || element.isAutowiredByRequiredTrue() == false) {
                return ProblemDescriptor.EMPTY_ARRAY
            }
            return arrayOf(
                manager.createProblemDescriptor(
                    problemElement,
                    getMessageTypeNone(psiType, nameClass),
                    isOnTheFly, emptyArray(), ProblemHighlightType.GENERIC_ERROR
                )
            )
        }

        if (checkBeans(beanDeclarations)) {
            return ProblemDescriptor.EMPTY_ARRAY
        }

        if (SpringCoreClasses.QUALIFIERS.any { element.isMetaAnnotatedBy(it) }) {
            return getProblemQualifier(module, element, manager, isOnTheFly)
        }
        var problems = emptyArray<ProblemDescriptor>()
        if (!psiType.canBeMoreThanOneBean(beanDeclarations)) {
            problems += manager.createProblemDescriptor(
                problemElement,
                getWarningMessageInheritor(nameClass, beanDeclarations, module),
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
        val beanDeclarations = SpringSearchService.getInstance(module.project)
            .findActiveBeanDeclarations(module, element.name ?: "", element.type, qualifier)

        if (beanDeclarations.isNotEmpty()) {
            return ProblemDescriptor.EMPTY_ARRAY
        }


        return arrayOf(
            manager.createProblemDescriptor(
                qualifier,
                SpringCoreBundle.message(
                    "esprito.spring.inspection.bean.class.unknown.qualifier.bean",
                    qualifier.text
                ),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR
            )
        )

    }

    private fun isBeanExist(module: Module, psiClass: PsiClass?): Boolean {
        if (psiClass == null) {
            return false
        }
        if (isAnnotationComponentContainingClass(psiClass)) {
            return true
        }
        return SpringSearchService.getInstance(module.project).getActiveBeansClasses(module).asSequence()
            .filter { it.psiClass.qualifiedName == psiClass.qualifiedName }
            .toList().isNotEmpty()
    }

    private fun isAnnotationComponentContainingClass(containingClass: PsiClass?): Boolean {
        if (containingClass == null) return false
        return containingClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
    }

    private fun getProblemByMethodWithoutParams(
        method: PsiMethod,
        params: Array<out PsiParameter>,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        var problems = emptyArray<ProblemDescriptor>()
        val identifier = method.identifyingElement
        if (method.isAutowiredByRequiredTrue() == true && params.isEmpty() && identifier != null) {
            problems += manager.createProblemDescriptor(
                identifier,
                SpringCoreBundle.message("esprito.spring.inspection.method.without.autowiring"),
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

        val fields = aClass.allFields
            .filter { it.isInjectOrAutowiredByRequiredTrue() }
            .map { it as PsiElement }.toSet()
        val methods = aClass.allMethods
            .filter { it.isInjectOrAutowiredByRequiredTrue() }
            .map { it as PsiElement }.toSet()
        val elements = fields + methods

        if (elements.isNotEmpty() && (aClass.nameIdentifier != null)) {
            elements.map {
                val identifier = it.childrenOfType<PsiIdentifier>().firstOrNull()
                if (identifier != null) {
                    problems += manager.createProblemDescriptor(
                        identifier,
                        SpringCoreBundle.message("esprito.spring.inspection.class.without.component"),
                        isOnTheFly,
                        emptyArray(),
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
            }
        }
        return problems
    }

    private fun getWarningMessageInheritor(
        name: String,
        beanCandidates: List<PsiMember>,
        module: Module
    ): String {
        val message = StringBuilder()
        message.append("<html><table><tr><td>")
        message.append(SpringCoreBundle.message("esprito.spring.inspection.bean.class.autowired.type", name))
        message.append("</td></tr><tr><td><table><tr><td valign='top'> Beans: </td><td>")
        beanCandidates.map { bean -> "${bean.resolveBeanName(module)} ({@link ${bean.targetClass?.name}${(bean as? PsiMethod)?.name?.let { "#$it" }}})" }.sorted().joinTo(message, " <br>")
        message.append("</td></tr></table></td></tr></table></html>")
        return message.toString()
    }

    private fun checkBeans(beanCandidates: List<PsiMember>): Boolean {
        when {
            beanCandidates.size == 1 -> return true
            beanCandidates.filter { it.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY) }.size == 1 -> return true
        }
        return false
    }

    private fun PsiModifierListOwner.isInjectOrAutowiredByRequiredTrue(): Boolean {
        if (isAutowiredByRequiredTrue() == true) {
            return true
        }
        return isMetaAnnotatedBy(SpringCoreClasses.QUALIFIERS)
    }

    private fun PsiModifierListOwner.isAutowiredByRequiredTrue(): Boolean? {
        if (isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED)) {
            // TODO: support several autowired annotations.
            return getMetaAnnotation(SpringCoreClasses.AUTOWIRED)?.let {
                AnnotationUtil.getBooleanAttributeValue(it, "required")
            }
        }
        return null
    }

    private fun getMessageTypeNone(psiType: PsiType, className: String): String {
        if (psiType.presentableText != className) {
            return SpringCoreBundle.message(
                "esprito.spring.inspection.bean.autowired.type.none.or",
                className,
                psiType.presentableText
            )
        }
        return SpringCoreBundle.message("esprito.spring.inspection.bean.autowired.type.none", psiType.presentableText)
    }

}