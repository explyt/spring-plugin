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