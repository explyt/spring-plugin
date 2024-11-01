package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.inspections.utils.ResourceFileInspectionUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.lang.properties.PropertiesFileType
import org.jetbrains.uast.UClass

class SpringPropertySourceClasspathInspection : SpringBaseUastLocalInspectionTool() {
    override fun checkClass(
        aClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<out ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val propertySourceAnnotation = ResourceFileInspectionUtil.psiAnnotationPropertySourceMembers(aClass.javaPsi)
        for (member in propertySourceAnnotation) {
            val pathValue = AnnotationUtil.getStringAttributeValue(member) ?: continue
            problems += ResourceFileInspectionUtil.getPathProblemsClasspath(
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