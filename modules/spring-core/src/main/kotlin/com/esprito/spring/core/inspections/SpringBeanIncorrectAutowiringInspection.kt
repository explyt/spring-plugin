package com.esprito.spring.core.inspections

import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.inspections.quickfix.AddQualifierQuickFix
import com.esprito.spring.core.service.PsiBean
import com.esprito.spring.core.service.SpringBeanService
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.util.SpringCoreUtil.canBeMoreThanOneBean
import com.esprito.spring.core.util.SpringCoreUtil.getBeanName
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanName
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.util.EspritoAnnotationUtil.getArrayAttributeAsPsiLiteral
import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isGeneric
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


class SpringBeanIncorrectAutowiringInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkField(
        field: PsiField,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor> {
        val module = ModuleUtilCore.findModuleForPsiElement(field) ?: return ProblemDescriptor.EMPTY_ARRAY

        var problems = emptyArray<ProblemDescriptor>()
        if (field.isInjectOrAutowiredByRequiredTrue()) {
            if (isAnnotationComponentContainingClass(field)) {
                if (SpringCoreUtil.existComponentScan(module)) {
                    problems += getProblemAutowired(module, field, manager, isOnTheFly)
                }
            } else {
                val annotation = field.getMetaAnnotation(SpringCoreClasses.AUTOWIRED)
                if (annotation != null) {
                    problems += manager.createProblemDescriptor(
                        annotation,
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

    override fun checkClass(
        aClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor> {
        var problems = emptyArray<ProblemDescriptor>()

        val module = ModuleUtilCore.findModuleForPsiElement(aClass) ?: ProblemDescriptor.EMPTY_ARRAY
        if (!SpringCoreUtil.existComponentScan(module as Module)) return ProblemDescriptor.EMPTY_ARRAY

        if (SpringCoreUtil.isSpringBeanCandidateClass(aClass)) {
            if (aClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)) {

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
            } else {
                problems += getProblemByClassWithoutComponent(aClass, manager, isOnTheFly)
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
        var problems = emptyArray<ProblemDescriptor>()

        if (element is PsiVariable) {
            val psiType = element.type
            val resolvedPsiBeanClass = psiType.resolveBeanPsiClass ?: return ProblemDescriptor.EMPTY_ARRAY
            val nameClass = resolvedPsiBeanClass.name ?: return ProblemDescriptor.EMPTY_ARRAY
            val problemElement = (element as? PsiNameIdentifierOwner)?.identifyingElement ?: return ProblemDescriptor.EMPTY_ARRAY

            val searchService = SpringSearchService.getInstance(module.project)
            val classInheritors = searchService.searchClassInheritors(resolvedPsiBeanClass).toMutableSet()
            val allBeansPsiMethods = searchService.getComponentBeanPsiMethods(module)
            val beansPsiMethods = searchService.getBeansPsiMethods(psiType, allBeansPsiMethods, resolvedPsiBeanClass)

            if (psiType.isOptional && classInheritors.isEmpty() && beansPsiMethods.isEmpty()) {
                return ProblemDescriptor.EMPTY_ARRAY
            }

            if (!resolvedPsiBeanClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
                && !isBeanExist(element, resolvedPsiBeanClass)
                && classInheritors.isEmpty() && beansPsiMethods.isEmpty()
            ) {
                return arrayOf(
                    manager.createProblemDescriptor(
                        problemElement,
                        getMessageTypeNone(psiType, nameClass),
                        isOnTheFly, emptyArray(), ProblemHighlightType.GENERIC_ERROR
                    )
                )
            }

            if (resolvedPsiBeanClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT) &&
                (!resolvedPsiBeanClass.isGeneric(psiType) || beansPsiMethods.isEmpty())) {
                classInheritors += resolvedPsiBeanClass
            }

            val nameElement = (element as? PsiNamedElement)?.name ?: ""
            when {
                checkBeans(classInheritors, beansPsiMethods, nameElement) -> return ProblemDescriptor.EMPTY_ARRAY

                (classInheritors + beansPsiMethods).isEmpty() -> {
                    problems += manager.createProblemDescriptor(
                        problemElement,
                        SpringCoreBundle.message("esprito.spring.inspection.bean.autowired.type.none", psiType.presentableText),
                        isOnTheFly, emptyArray(), ProblemHighlightType.GENERIC_ERROR
                    )
                }

                else -> {
                    if (SpringCoreClasses.STRING_QUALIFIERS.any { element.isMetaAnnotatedBy(it) }) {
                        problems += getProblemQualifier(module, element, manager, isOnTheFly)
                    } else {
                         if (!psiType.canBeMoreThanOneBean(classInheritors)) {
                             val beanCandidates = classInheritors.toList() + beansPsiMethods
                             problems += manager.createProblemDescriptor(
                                 problemElement,
                                 getWarningMessageInheritor(nameClass, beanCandidates),
                                 AddQualifierQuickFix(SpringCoreClasses.QUALIFIER, problemElement),
                                 ProblemHighlightType.GENERIC_ERROR, isOnTheFly
                            )
                         }
                    }
                }
            }
        }

        return problems
    }

    private fun getProblemQualifier(
        module: Module,
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        if (element !is PsiVariable) return ProblemDescriptor.EMPTY_ARRAY
        val resolvedPsiClass = element.type.resolvedPsiClass ?: return ProblemDescriptor.EMPTY_ARRAY

        var problems = emptyArray<ProblemDescriptor>()
        val elementLiterals = getLiteralQualifier(element)

        val beanCandidates = SpringBeanService.getInstance(module.project).getBeanCandidates(element.type, module)
        val elementPsiBean = elementLiterals?.let { PsiBean(it, resolvedPsiClass, null) }

        if (!beanCandidates.any { it.name == elementPsiBean?.name }) {
            val psiLiteralList = getPsiLiteralList(element)
            psiLiteralList.forEach {
                val psiElementQualifier: PsiElement? = if (it.value.toString().isNotBlank()) it else null
                if (psiElementQualifier != null) {
                    problems += arrayOf(
                        manager.createProblemDescriptor(
                            psiElementQualifier,
                            SpringCoreBundle.message(
                                "esprito.spring.inspection.bean.class.unknown.qualifier.bean",
                                it.value.toString()
                            ),
                            isOnTheFly,
                            emptyArray(),
                            if (it.value.toString().isBlank()) ProblemHighlightType.GENERIC_ERROR else ProblemHighlightType.ERROR
                        )
                    )
                }
            }
        }

        return problems
    }

    private fun getAnnotationValue(psiClassList: List<PsiClass>, annotationName: String): Set<String?> {
        return psiClassList
            .asSequence()
            .filter { it.isMetaAnnotatedBy(annotationName) }
            .mapNotNull { it.getMetaAnnotation(annotationName) }
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it, "value") }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun getLiteralQualifier(element: PsiElement): String? {
        if (element !is PsiModifierListOwner) return null

        if (element is PsiParameter) {
            val valueMethod = getLiteralQualifier(element.declarationScope)
            if (valueMethod != null) return valueMethod
        }

        return SpringCoreClasses.STRING_QUALIFIERS
            .asSequence()
            .map { getAnnotationAttributeValue(element, it) }
            .firstOrNull { it != null }
    }

    private fun getPsiLiteralList(element: PsiElement): Collection<PsiLiteral> {
        if (element !is PsiModifierListOwner) return emptyList()

        if (element is PsiParameter) {
            val psiLiterals = getPsiLiteralList(element.declarationScope)
            if (psiLiterals.isNotEmpty()) return psiLiterals
        }

        return SpringCoreClasses.STRING_QUALIFIERS
            .asSequence()
            .map { getLiteralValueByAnnotationName(element, it) }
            .firstOrNull { it.isNotEmpty() }
            ?: emptyList()
    }

    private fun PsiModifierListOwner.getPsiAnnotationByAnnotationName(annotationName: String): PsiAnnotation? {
        if (isMetaAnnotatedBy(annotationName)) {
            return getMetaAnnotation(annotationName)
        }
        return null
    }

    private fun getAnnotationAttributeValue(element: PsiModifierListOwner, annotationName: String): String? {
        val annotation = element.getPsiAnnotationByAnnotationName(annotationName) ?: return null
        return AnnotationUtil.getStringAttributeValue(annotation, "value")
    }

    private fun getLiteralValueByAnnotationName(
        element: PsiModifierListOwner,
        annotationName: String
    ): Collection<PsiLiteral> {
        if (element.isMetaAnnotatedBy(annotationName)) {
            val annotation = element.getMetaAnnotation(annotationName)
            if (annotation != null) {
                return annotation.getArrayAttributeAsPsiLiteral("value")
            }
        }
        return emptyList()
    }

    private fun isAnnotationComponentContainingClass(field: PsiField): Boolean {
        return field.containingClass?.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT) ?: false
    }

    private fun isBeanExist(element: PsiElement, psiClass: PsiClass): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return false
        return SpringSearchService.getInstance(module.project).getAllBeansClasses(module)
            .any { it.psiClass == psiClass }
    }

    private fun getProblemByMethodWithoutParams(
        method: PsiMethod,
        params: Array<out PsiParameter>,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        var problems = emptyArray<ProblemDescriptor>()
        val identifier = method.identifyingElement
        if (method.isAutowiredByRequiredTrue() && params.isEmpty() && identifier != null) {
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
            val psiElement = aClass.nameIdentifier!!.navigationElement
            problems += manager.createProblemDescriptor(
                psiElement,
                SpringCoreBundle.message("esprito.spring.inspection.class.without.component"),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR
            )
        }
        return problems
    }

    private fun getWarningMessageInheritor(
        name: String,
        beanCandidates: List<PsiModifierListOwner>,
    ): String {
        val message = StringBuilder()
        message.append("<html><table><tr><td>")
        message.append(SpringCoreBundle.message("esprito.spring.inspection.bean.class.autowired.type", name))
        message.append("</td></tr><tr><td><table><tr><td valign='top'> Beans: </td><td>")
        beanCandidates.forEach { message.append(getName(it) + " <br>") }
        message.append("</td></tr></table></td></tr></table></html>")
        return message.toString()
    }


    private fun checkBeans(
        classInheritors: Collection<PsiClass>, methods: Collection<PsiMethod>,
        nameElement: String
    ): Boolean {
        when {
            (classInheritors.size + methods.size) == 1 -> return true

            (classInheritors.filter { it.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY) }.size +
                    methods.filter { it.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY) }.size) == 1 -> return true

            (classInheritors.filter { it.getBeanName() == nameElement }.size +
                    methods.filter { it.resolveBeanName == nameElement }.size) == 1 -> return true

            getAnnotationValue(classInheritors.toList(), SpringCoreClasses.COMPONENT)
                .filter { it == nameElement }.size == 1 -> return true
        }
        return false
    }

    private fun PsiModifierListOwner.isInjectOrAutowiredByRequiredTrue(): Boolean {
        if (this.isAutowiredByRequiredTrue()) {
            return true
        }
        return SpringCoreClasses.STRING_QUALIFIERS
            .any { this.isMetaAnnotatedBy(it) }
    }

    private fun PsiModifierListOwner.isAutowiredByRequiredTrue(): Boolean {
        if (this.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED))
            return this.annotations
                .flatMap { it.getArrayAttributeAsPsiLiteral("required") }
                .any { (it.value as Boolean) }
        return false
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

    private fun getName(value: PsiModifierListOwner): String {
        return when (value) {
            is PsiClass -> {
                value.name.toString()
            }

            is PsiMethod -> {
                value.name
            }

            else -> ""
        }
    }

}