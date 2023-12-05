package com.esprito.spring.core.inspections

import com.esprito.spring.core.inspections.utils.ResourceFileInspectionUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.psi.PsiClass

class SpringPropertySourceFileReferenceInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun checkClass(
        aClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<out ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val propertySourceAnnotation = ResourceFileInspectionUtil.psiAnnotationPropertySourceMembers(aClass)
        for (member in propertySourceAnnotation) {
            val pathValue = AnnotationUtil.getStringAttributeValue(member) ?: continue
            problems += ResourceFileInspectionUtil.getPathProblems(
                PropertiesFileType.INSTANCE,
                pathValue,
                member,
                manager,
                isOnTheFly
            )
        }
        return problems.toTypedArray()
    }
}