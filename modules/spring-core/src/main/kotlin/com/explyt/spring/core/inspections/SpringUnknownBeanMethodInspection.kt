package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses.BEAN
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.explyt.util.ExplytPsiUtil.fitsForReference
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.impl.quickfix.createVoidMethodFixes
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.uast.UMethod
import java.util.regex.Pattern


class SpringUnknownBeanMethodInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val method = uMethod.javaPsi
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

            val memberSourcePsi = member.toSourcePsi() ?: continue

            problems += manager.createProblemDescriptor(
                memberSourcePsi,
                memberSourcePsi.getHighlightRange(),
                SpringCoreBundle.message("explyt.spring.inspection.bean.method"),
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