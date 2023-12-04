package com.esprito.spring.core.inspections

import com.esprito.spring.core.inspections.utils.ResourceFileInspectionUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.psi.PsiClass

class SpringContextConfigurationClasspathInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun checkClass(
        aClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<out ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val propertySourceAnnotation = ResourceFileInspectionUtil.psiAnnotationContextConfigurationMembers(aClass)
        for (member in propertySourceAnnotation) {
            val pathValue = AnnotationUtil.getStringAttributeValue(member) ?: continue
            problems += ResourceFileInspectionUtil.getPathProblemsClasspath(
                "XML",
                pathValue,
                member,
                manager,
                isOnTheFly
            )
        }
        return problems.toTypedArray()
    }
}