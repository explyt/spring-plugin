/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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

    val keyValueElement = yamlScalar.parentOfType<YAMLKeyValue>() ?: return false
    val text = keyValueElement.key?.text ?: return false
    //docker compose conf - 'environment:'
    if (text.equals("environment", true)) return true
    //k8s conf - 'env.name:'
    if (text.equals("name", true)) {
        val keyParent = keyValueElement.parentOfType<YAMLKeyValue>()?.key ?: return false
        return keyParent.text == "env"
    }
    return false
}