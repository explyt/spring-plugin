package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.inspections.utils.ResourceFileInspectionUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.ide.highlighter.XmlFileType
import org.jetbrains.uast.UClass

class SpringContextConfigurationFileInspection : SpringBaseUastLocalInspectionTool() {
    override fun checkClass(
        aClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<out ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val propertySourceAnnotation = ResourceFileInspectionUtil
            .psiAnnotationContextConfigurationMembers(aClass.javaPsi)
        for (member in propertySourceAnnotation) {
            val pathValue = AnnotationUtil.getStringAttributeValue(member) ?: continue
            problems += ResourceFileInspectionUtil.getPathProblemsWithPrefixFile(
                XmlFileType.INSTANCE,
                pathValue,
                member,
                manager,
                isOnTheFly
            )
        }
        return problems.toTypedArray()
    }
}