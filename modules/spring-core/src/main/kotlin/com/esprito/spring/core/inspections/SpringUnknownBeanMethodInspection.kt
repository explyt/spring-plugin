package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.BEAN
import com.esprito.spring.core.inspections.quickfix.AddMethodQuickFix
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.util.EspritoAnnotationUtil.getMemberValues
import com.esprito.util.EspritoPsiUtil.fitsForReference
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.isAbstract
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.*
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiMethod
import java.util.regex.Pattern


class SpringUnknownBeanMethodInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkMethod(
        method: PsiMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val module = ModuleUtilCore.findModuleForPsiElement(method) ?: return null
        val searchService = SpringSearchService.getInstance(method.project)

        val metaHolder = searchService.getMetaAnnotations(module, BEAN)
        val targetMethods = setOf("initMethod", "destroyMethod")

        val initDestroyMethodValues = mutableListOf<PsiAnnotationMemberValue>()

        for (annotation in method.annotations) {
            val annotationFqn = annotation.qualifiedName ?: continue
            if (!metaHolder.contains(annotation)) continue

            initDestroyMethodValues +=
                annotation.attributes.asSequence()
                .filter {
                    metaHolder.isAttributeRelatedWith(
                        annotationFqn,
                        it.attributeName,
                        BEAN,
                        targetMethods
                    )
                }
                .flatMap { annotation.getMemberValues(it.attributeName) }
        }

        val problems: MutableList<ProblemDescriptor> = mutableListOf()

        for (member in (initDestroyMethodValues)) {
            val methodName = AnnotationUtil.getStringAttributeValue(member)
            val returnTypeClass = method.returnType?.resolveBeanPsiClass ?: continue

            if (methodName.isNullOrBlank() || methodName == "(inferred)") continue
            if (returnTypeClass.allMethods.any {
                    fitsForReference(it)
                            && it.name == methodName }) continue

            val fixes: MutableList<LocalQuickFix> = mutableListOf()
            if (isMethodNameValid(methodName)) {
                fixes.add(AddMethodQuickFix(methodName, false, returnTypeClass))
                if (returnTypeClass.isAbstract && !returnTypeClass.isInterface) {
                    fixes.add(AddMethodQuickFix(methodName, true, returnTypeClass))
                }
            }

            problems.add(
                manager.createProblemDescriptor(
                    member,
                    member.getHighlightRange(),
                    SpringCoreBundle.message("esprito.spring.inspection.bean.method"),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly,
                    *fixes.toTypedArray()
                )
            )
        }

        return problems.toTypedArray()
    }

    private fun isMethodNameValid(methodName: String): Boolean {
        return ValidationUtil.methodPattern.matcher(methodName).matches()
    }

    object ValidationUtil {
        private const val METHOD_REGEX = "^[_\$\\w&&\\D][_\$\\w]*\$"
        val methodPattern: Pattern = Pattern.compile(METHOD_REGEX)
    }

}