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

package com.explyt.spring.core.completion.doker

import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar


class DockerEnvVariableYamlCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), DockerEnvVariableYamlCompletionProvider())
    }

    private class DockerEnvVariableYamlCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet,
        ) {
            if (!isDockerEnvCandidate(parameters.position)) return
            val module = ModuleUtilCore.findModuleForPsiElement(parameters.originalFile) ?: return

            val envVariables = SpringConfigurationPropertiesSearch.getInstance(module.project)
                .getAllPropertiesSystemEnvironment(module)

            envVariables.forEach {
                result.addElement(
                    LookupElementBuilder.create(it)
                        .withCaseSensitivity(false)
                        .withInsertHandler(DockerEnvCompletionInsertHandler)
                )
            }
        }
    }
}

private object DockerEnvCompletionInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val startOffset = context.startOffset
        val tailOffset = context.tailOffset

        context.editor.document.replaceString(startOffset, tailOffset, item.lookupString)
        context.editor.caretModel.moveToOffset(startOffset + item.lookupString.length)
        context.commitDocument()
    }

}

fun isDockerEnvCandidate(psiElement: PsiElement): Boolean {
    if (psiElement.language != YAMLLanguage.INSTANCE) return false
    val psiFile = psiElement.containingFile ?: return false
    if (SpringCoreUtil.isConfigurationPropertyFile(psiFile)) return false
    val yamlScalar = psiElement as? YAMLScalar ?: psiElement.parent as? YAMLScalar ?: return false

    val keyElement = yamlScalar.parentOfType<YAMLKeyValue>()?.key ?: return false
    val text = keyElement.text ?: return false
    return text.equals("environment", true)
}