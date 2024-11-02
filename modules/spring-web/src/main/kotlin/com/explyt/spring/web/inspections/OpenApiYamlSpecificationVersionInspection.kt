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

package com.explyt.spring.web.inspections

import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.util.text.StringUtil
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
            val text = versionKeyValue.text?.trim('"') ?: return ProblemDescriptor.EMPTY_ARRAY
            if (StringUtil.compareVersionNumbers(text, "3.0.0") < 0) {
                return problemDescriptors(manager, versionKeyValue, versionKeyValue.text, isOnTheFly)
            }
        }
        return ProblemDescriptor.EMPTY_ARRAY
    }

}