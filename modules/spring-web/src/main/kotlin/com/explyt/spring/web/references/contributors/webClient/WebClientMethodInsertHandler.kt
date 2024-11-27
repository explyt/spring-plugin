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

package com.explyt.spring.web.references.contributors.webClient

import com.explyt.spring.core.statistic.StatisticActionId.COMPLETION_WEB_CLIENT_METHOD_CALL_WITH_TYPE
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import org.jetbrains.kotlin.idea.base.psi.imports.addImport
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

class WebClientMethodInsertHandler(private val type: String, private val toImport: Set<FqName> = setOf()) :
    InsertHandler<LookupElement> {

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        StatisticService.getInstance().addActionUsage(COMPLETION_WEB_CLIENT_METHOD_CALL_WITH_TYPE)

        EditorModificationUtilEx.insertStringAtCaret(context.editor, type)
        PsiDocumentManager.getInstance(context.project).commitDocument(context.document)

        val ktFile = context.file as? KtFile
        if (ktFile != null) {
            for (fqName in toImport) {
                ktFile.addImport(fqName)
            }
            PsiDocumentManager.getInstance(context.project).commitDocument(context.document)
            OptimizeImportsProcessor(context.project, context.file).runWithoutProgress()
            return
        }

        val underCursor = context.file.findElementAt(context.tailOffset)?.parent ?: return
        JavaCodeStyleManager.getInstance(context.project).shortenClassReferences(underCursor)
    }

}
