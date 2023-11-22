package com.esprito.spring.core.inspections

import com.esprito.spring.core.inspections.utils.ResourceFileUtil
import com.esprito.util.ModuleUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.psi.PsiClass

class SpringPropertySourceFileInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun checkClass(
        aClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val propertySourceAnnotation = ResourceFileUtil.psiAnnotationPropertySourceMembers(aClass)
        for (member in propertySourceAnnotation) {
            val pathValue = AnnotationUtil.getStringAttributeValue(member) ?: continue
            problems += ResourceFileUtil.getPathProblemsWithPrefixFile("Properties", pathValue, member, manager, isOnTheFly)
            ModuleUtil.getContentRootFile(member)
        }
        return problems.toTypedArray()
    }


}

