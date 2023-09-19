package com.esprito.spring.core.inspections

import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.inspections.quick_fix.AddQualifierQuickFix
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanClass
import com.esprito.util.EspritoAnnotationUtil.getArrayAttributeAsPsiLiteral
import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isEqualOrInheritor
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.*
import com.intellij.psi.search.searches.ClassInheritorsSearch
import java.util.*


class SpringBeanIncorrectAutowiringInspection: AbstractBaseJavaLocalInspectionTool() {

    private val stringQualifiers = listOf(SpringCoreClasses.QUALIFIER) + JavaEeClasses.NAMED.allFqns

    private val stringInjects = listOf(
        JavaEeClasses.INJECT.allFqns,
        JavaEeClasses.RESOURCE.allFqns
    )

    override fun checkField(
        field: PsiField,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor> {
        val module = ModuleUtilCore.findModuleForPsiElement(field) ?: return ProblemDescriptor.EMPTY_ARRAY

        if (isInjectOrAutowiredByRequiredTrue(field)
            && isAnnotationComponentContainingClass(field)
        ) {
            var problems = emptyArray<ProblemDescriptor>()
            if (SpringCoreUtil.existComponentScan(module)) {
                problems += getProblemAutowired(module, field, manager, isOnTheFly)
            }
            return problems
        }
        return ProblemDescriptor.EMPTY_ARRAY
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
                    .filter { isInjectOrAutowiredByRequiredTrue(it) }
                for (method in methods) {
                    for (parameter in method.parameterList.parameters.toList()) {
                        problems += getProblemAutowired(module, parameter, manager, isOnTheFly)
                    }
                }
            }
        } else {
            problems = getProblemByClassWithoutComponent(aClass, manager, isOnTheFly)
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
                .filter { it.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED) }

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
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        var problems = emptyArray<ProblemDescriptor>()

        if (element is PsiVariable) {
            val psiType = element.type
            val resolvedPsiClass = psiType.resolveBeanClass() ?: return ProblemDescriptor.EMPTY_ARRAY
            val nameClass = resolvedPsiClass.name ?: return ProblemDescriptor.EMPTY_ARRAY
            val problemElement = getIdentifyingElement(element) ?: return ProblemDescriptor.EMPTY_ARRAY

            val classInheritors = ClassInheritorsSearch.search(resolvedPsiClass).findAll()
                .asSequence()
                .filterNotNull()
                .filter { it.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT) }
                .filter { it.name != null }
                .toMutableList()

            val methodsPsiBeans = SpringSearchService.getInstance(module.project).getComponentBeanPsiMethods(module)
                .filter { it.returnPsiClass?.isEqualOrInheritor(resolvedPsiClass) == true }

            if (!resolvedPsiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
                && !isBeanExist(element, resolvedPsiClass)
                && classInheritors.isEmpty() && methodsPsiBeans.isEmpty()
            ) {
                return arrayOf(
                    manager.createProblemDescriptor(
                        problemElement,
                        SpringCoreBundle.message("esprito.spring.inspection.bean.autowired.type.none", nameClass),
                        isOnTheFly, emptyArray(), ProblemHighlightType.GENERIC_ERROR
                    )
                )
            }

            if (resolvedPsiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)) {
                classInheritors.add(resolvedPsiClass)
            }

            val nameElement = getElementName(element)
            when {
                checkBeans(nameElement, classInheritors, methodsPsiBeans) -> return ProblemDescriptor.EMPTY_ARRAY

                classInheritors.isEmpty() && methodsPsiBeans.isEmpty() -> {
                        problems += manager.createProblemDescriptor(
                            problemElement,
                            SpringCoreBundle.message("esprito.spring.inspection.bean.autowired.type.none", nameClass),
                            AddQualifierQuickFix(SpringCoreClasses.QUALIFIER, problemElement),
                            ProblemHighlightType.GENERIC_ERROR, isOnTheFly
                        )
                }

                else -> {
                    val elementLiterals = getLiteralQualifier(element)
                    if (elementLiterals == null) {
                        problems += manager.createProblemDescriptor(
                            problemElement,
                            getWarningMessageInheritor(nameClass, classInheritors, methodsPsiBeans),
                            AddQualifierQuickFix(SpringCoreClasses.QUALIFIER, problemElement),
                            ProblemHighlightType.GENERIC_ERROR, isOnTheFly
                        )
                    } else {
                        problems += getProblemQualifier(element, manager, isOnTheFly)
                    }
                }
            }
        }

        return problems
    }

    private fun getProblemQualifier(
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        if (element !is PsiVariable) return ProblemDescriptor.EMPTY_ARRAY
        val resolvedPsiClass = element.type.resolveBeanClass() ?: return ProblemDescriptor.EMPTY_ARRAY

        var problems = emptyArray<ProblemDescriptor>()
        val elementLiterals = getLiteralQualifier(element)

        val classInheritors = ClassInheritorsSearch.search(resolvedPsiClass).findAll()
            .asSequence()
            .filterNotNull()
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT) }
            .toList()

        val psiLiteralInheritors =
            getAnnotationValue(classInheritors, SpringCoreClasses.QUALIFIER) +
                    getAnnotationValue(classInheritors, JavaEeClasses.NAMED.jakarta) +
                    getAnnotationValue(classInheritors, JavaEeClasses.NAMED.javax) +
                    getAnnotationValue(classInheritors, SpringCoreClasses.COMPONENT) +
                    getBeanName(classInheritors)

        if (!psiLiteralInheritors.contains(elementLiterals)) {
            val psiLiteralList = getPsiLiteralList(element)
            psiLiteralList.forEach {
                val psiElementQualifier: PsiElement? = if (it.value.toString().isBlank()) getPsiAnnotation(element)?.navigationElement else it
                if (psiElementQualifier != null) {
                    problems += arrayOf(
                        manager.createProblemDescriptor(
                            psiElementQualifier,
                            SpringCoreBundle.message(
                                "esprito.spring.inspection.bean.class.unknown.qualifier.bean",
                                it.value.toString()
                            ),
                            AddQualifierQuickFix(SpringCoreClasses.QUALIFIER, element),
                            if (it.value.toString().isBlank()) ProblemHighlightType.GENERIC_ERROR else ProblemHighlightType.ERROR,
                            isOnTheFly
                        )
                    )
                }
            }
        }

        return problems
    }

    private fun getIdentifyingElement(element: PsiElement): PsiElement? =
        (element as? PsiNameIdentifierOwner)?.identifyingElement

    private fun getElementName(element: PsiElement): String = (element as? PsiNamedElement)?.name ?: ""

    private fun getAnnotationValue(psiClassList: List<PsiClass>, annotationName: String): Set<String?> {
    return psiClassList
        .asSequence()
        .filter { it.isMetaAnnotatedBy(annotationName) }
        .mapNotNull { it.getMetaAnnotation(annotationName) }
        .mapNotNull { AnnotationUtil.getStringAttributeValue(it, "value") }
        .filter { it.isNotBlank() }
        .toSet()
    }

    private fun getBeanName(classInheritors: List<PsiClass>): Set<String> =
        classInheritors
            .mapNotNull { it.name.lowerFirstChar() }
            .toSet()

    private fun getLiteralQualifier(element: PsiElement): String? {
        if (element !is PsiModifierListOwner) return null

        if (element is PsiParameter) {
            val valueMethod = getLiteralQualifier(element.declarationScope)
            if (valueMethod != null) return valueMethod
        }

        return stringQualifiers
            .asSequence()
            .map { getAnnotationAttributeValue(element, it) }
            .firstOrNull { it != null }
    }

    private fun getPsiAnnotation(element: PsiElement): PsiAnnotation? {
        if (element !is PsiModifierListOwner) return null

        if (element is PsiParameter) {
            val valueMethod = getPsiAnnotation(element.declarationScope)
            if (valueMethod != null) return valueMethod
        }

        return stringQualifiers
            .asSequence()
            .map { getPsiAnnotationByAnnotationName(element, it) }
            .firstOrNull { it != null }
    }

    private fun getPsiLiteralList(element: PsiElement): Collection<PsiLiteral> {
        if (element !is PsiModifierListOwner) return emptyList()

        if (element is PsiParameter) {
            val psiLiterals = getPsiLiteralList(element.declarationScope)
            if (psiLiterals.isNotEmpty()) return psiLiterals
        }

        return stringQualifiers
            .asSequence()
            .map { getLiteralValueByAnnotationName(element, it) }
            .firstOrNull { it.isNotEmpty() }
            ?: emptyList()
    }

    private fun getPsiAnnotationByAnnotationName(element: PsiModifierListOwner, annotationName: String): PsiAnnotation? {
        if (element.isMetaAnnotatedBy(annotationName)) {
            val annotation = element.getMetaAnnotation(annotationName)
            if (annotation != null) {
                return annotation
            }
        }
        return null
    }

    private fun getAnnotationAttributeValue(element: PsiModifierListOwner, annotationName: String): String? {
        if (element.isMetaAnnotatedBy(annotationName)) {
            val annotation = element.getMetaAnnotation(annotationName)
            if (annotation != null) {
                return AnnotationUtil.getStringAttributeValue(annotation, "value")
            }
        }
        return null
    }

    private fun getLiteralValueByAnnotationName(element: PsiModifierListOwner, annotationName: String): Collection<PsiLiteral> {
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
            .any{ it.psiClass == psiClass }
    }

    private fun getProblemByClassWithoutComponent(
        aClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        var problems = emptyArray<ProblemDescriptor>()

        val fields = aClass.allFields
            .filter { isInjectOrAutowiredByRequiredTrue(it) }
            .map { it as PsiElement }.toSet()
        val methods = aClass.allMethods
            .filter { isInjectOrAutowiredByRequiredTrue(it) }
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

    private fun getWarningMessageInheritor(name: String, classInheritors: List<PsiClass>, methodsBeans: List<PsiMethod>): String {
        val message = StringBuilder()
        message.append("<html><table><tr><td>")
        message.append(SpringCoreBundle.message("esprito.spring.inspection.bean.class.autowired.type", name))
        message.append("</td></tr><tr><td><table><tr><td valign='top'> Beans: </td><td>")
        classInheritors.forEach { message.append(it.name.toString() + " <br>")}
        methodsBeans.forEach { message.append(it.name + " <br>")}
        message.append("</td></tr></table></td></tr></table></html>")
        return message.toString()
    }

    private fun String?.lowerFirstChar(): String? {
        if (isNullOrEmpty()) return null

        return replaceFirstChar { it.lowercase(Locale.getDefault()) }
    }

    private fun checkBeans(nameElement: String, classInheritors: Collection<PsiClass>, methods: Collection<PsiMethod>): Boolean {
        when {
            (classInheritors.size + methods.size) == 1 -> return true

            (classInheritors.filter { it.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY) }.size +
            methods.filter { it.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY) }.size) == 1 -> return true

            (classInheritors.filter{ it.name.lowerFirstChar() == nameElement }.size +
            methods.filter{ it.name.lowerFirstChar() == nameElement }.size) == 1 -> return true

            getAnnotationValue(classInheritors.toList(), SpringCoreClasses.COMPONENT)
                .filter{ it == nameElement }.size == 1 -> return true
        }
        return false
    }

    private fun isInjectOrAutowiredByRequiredTrue(owner: PsiModifierListOwner): Boolean {
        if (owner.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED))
            return owner.annotations
                .flatMap { it.getArrayAttributeAsPsiLiteral("required") }
                .any { (it.value as Boolean) }

        return stringInjects.any { owner.isMetaAnnotatedBy(it) }
    }
}