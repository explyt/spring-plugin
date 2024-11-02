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

package com.explyt.jpa.ql.psi.impl

import com.explyt.jpa.ql.JpqlLanguage
import com.explyt.jpa.ql.psi.JpqlFile
import com.explyt.jpa.ql.psi.JpqlIdentifier
import com.explyt.jpa.ql.psi.JpqlInputParameterExpression
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil


@Service(Service.Level.PROJECT)
class JpqlElementFactory(
    private val project: Project
) {
    fun createIdentifier(name: String): JpqlIdentifier {
        val file: JpqlFile = createFile("FROM $name")
        return PsiTreeUtil.findChildOfType(file, JpqlIdentifier::class.java, false)
            ?: throw ProcessCanceledException()
    }

    fun createNamedInputParameter(newElementName: String): JpqlInputParameterExpression {
        val file: JpqlFile = createFile("FROM foo WHERE bar > :$newElementName")
        return PsiTreeUtil.findChildOfType(file, JpqlInputParameterExpression::class.java, false)
            ?: throw ProcessCanceledException()
    }

    fun createFile(text: String, eventSystemEnabled: Boolean = false): JpqlFile {
        val name = "dummy.jpql"
        return PsiFileFactory.getInstance(project)
            .createFileFromText(
                name,
                JpqlLanguage.INSTANCE,
                text,
                eventSystemEnabled,
                false
            ) as JpqlFile
    }

    companion object {
        fun getInstance(project: Project): JpqlElementFactory = project.service()
    }
}