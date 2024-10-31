package com.esprito.spring.web.inspections

import com.esprito.spring.web.util.SpringWebUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonValue
import com.intellij.psi.PsiFile

class OpenApiJsonSpecificationVersionInspection : OpenApiVersionInspectionBase() {

    override fun getProblems(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        if (file.fileType !is JsonFileType) return ProblemDescriptor.EMPTY_ARRAY
        val topLevelValue = (file as JsonFile).topLevelValue as? JsonObject ?: return ProblemDescriptor.EMPTY_ARRAY

        val openApiVersion = findOpenApiVersion(topLevelValue) ?: return ProblemDescriptor.EMPTY_ARRAY
        val text = openApiVersion.text?.trim('"') ?: return ProblemDescriptor.EMPTY_ARRAY
        if (text != "3.0.0" && text != "3.1.0") {
            return problemDescriptors(manager, openApiVersion, text, isOnTheFly)
        }

        return ProblemDescriptor.EMPTY_ARRAY
    }

    private fun findOpenApiVersion(jsonObject: JsonObject): JsonValue? {
        val openApiProperty = jsonObject.propertyList.find { it.name == SpringWebUtil.OPEN_API }
        return openApiProperty?.value
    }

}