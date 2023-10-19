package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.BEAN
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.util.EspritoPsiUtil.fitsForReference
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.impl.quickfix.createVoidMethodFixes
import com.intellij.codeInspection.*
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.module.ModuleUtilCore
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
        val initDestroyMethodValues = metaHolder.getAnnotationMemberValues(method, setOf("initMethod", "destroyMethod"))

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
                fixes += createVoidMethodFixes(returnTypeClass, methodName, JvmModifier.PRIVATE)
            }

            problems += manager.createProblemDescriptor(
                member,
                member.getHighlightRange(),
                SpringCoreBundle.message("esprito.spring.inspection.bean.method"),
                ProblemHighlightType.GENERIC_ERROR,
                isOnTheFly,
                *fixes.toTypedArray()
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