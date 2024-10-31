package com.esprito.spring.web.inspections

import com.esprito.spring.web.util.SpringWebUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping

class OpenApiYamlSpecificationVersionInspection : OpenApiVersionInspectionBase() {

    override fun getProblems(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        if (file.fileType !is YAMLFileType) return ProblemDescriptor.EMPTY_ARRAY
        val documents = (file as YAMLFile).documents
        if (documents.isEmpty()) return ProblemDescriptor.EMPTY_ARRAY

        val topLevelValue = documents[0].topLevelValue
        if (topLevelValue is YAMLMapping) {
            val versionKey = topLevelValue.keyValues.find { it.keyText == SpringWebUtil.OPEN_API }
                ?: return ProblemDescriptor.EMPTY_ARRAY
            val versionKeyValue = versionKey.value ?: return ProblemDescriptor.EMPTY_ARRAY
            if (versionKeyValue.text != "3.0.0" && versionKeyValue.text != "3.1.0") {
                return problemDescriptors(manager, versionKeyValue, versionKeyValue.text, isOnTheFly)
            }
        }
        return ProblemDescriptor.EMPTY_ARRAY
    }

}