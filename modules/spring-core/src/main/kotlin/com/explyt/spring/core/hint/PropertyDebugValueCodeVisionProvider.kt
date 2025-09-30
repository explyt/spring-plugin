/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.spring.core.hint

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.util.DebugUtil
import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.codeVision.CodeVisionState.Companion.READY_EMPTY
import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingProvider
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.InlayHintsUtils
import com.intellij.debugger.engine.JavaValue
import com.intellij.icons.AllIcons
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XValue
import com.sun.jdi.StringReference
import fleet.multiplatform.shims.ConcurrentHashMap
import org.jetbrains.yaml.YAMLFileType
import java.lang.Integer.min
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val PROPERTY_FILE_TYPES = setOf(PropertiesFileType.INSTANCE, YAMLFileType.YML)

class PropertyDebugValueCodeVisionProvider : CodeVisionProvider<VirtualFile> {

    override fun isAvailableFor(project: Project) = true//SpringCoreUtil.isExplytDebug(project)

    override fun precomputeOnUiThread(editor: Editor) = editor.virtualFile

    override fun computeCodeVision(editor: Editor, virtualFile: VirtualFile): CodeVisionState {
        val project = editor.project ?: return READY_EMPTY
        if (virtualFile.fileType !in PROPERTY_FILE_TYPES) return READY_EMPTY
        val currentSession = XDebuggerManager.getInstance(project).currentSession ?: return READY_EMPTY
        val xSourcePosition = currentSession.currentStackFrame?.sourcePosition ?: return READY_EMPTY
        val propertiesValueByKey = runReadAction {
            val module = ModuleUtilCore.findModuleForFile(virtualFile, project) ?: return@runReadAction emptyMap()
            DefinedConfigurationPropertiesSearch.getInstance(project)
                .getAllProperties(module).asSequence()
                .filter { it.sourceFile == virtualFile.name }
                .filter { it.psiElement?.containingFile?.virtualFile == virtualFile }
                .mapNotNull { it.value?.let { value -> it.key to value } }
                .toMap()
        }
        if (propertiesValueByKey.isEmpty()) return READY_EMPTY

        val countDownLatch = CountDownLatch(propertiesValueByKey.size)
        val runtimeValueMap = ConcurrentHashMap<String, String>()
        propertiesValueByKey.keys.forEach {
            evaluatePropertyRuntimeValue(currentSession, xSourcePosition, it, countDownLatch, runtimeValueMap)
        }
        countDownLatch.await(3, TimeUnit.SECONDS)
        if (runtimeValueMap.isEmpty()) return READY_EMPTY

        return InlayHintsUtils.computeCodeVisionUnderReadAction {
            computeCodeVision(project, virtualFile, editor, runtimeValueMap)
        }
    }

    private fun computeCodeVision(
        project: Project, virtualFile: VirtualFile, editor: Editor, runtimeValueMap: Map<String, String>
    ): CodeVisionState {
        XDebuggerManager.getInstance(project).currentSession ?: return READY_EMPTY
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return READY_EMPTY
        val module = ModuleUtilCore.findModuleForFile(virtualFile, project) ?: return READY_EMPTY
        val propertiesMapByPsiElement = DefinedConfigurationPropertiesSearch.getInstance(project)
            .getAllProperties(module).asSequence()
            .filter { it.sourceFile == virtualFile.name }
            .filter { it.psiElement?.containingFile?.virtualFile == virtualFile }
            .groupBy { it.psiElement!! }
            .takeIf { it.isNotEmpty() } ?: return READY_EMPTY

        val lenses = mutableListOf<Pair<TextRange, CodeVisionEntry>>()
        psiFile.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val properties = propertiesMapByPsiElement[element]
                if (properties != null) {
                    addCodeVisionForChangedPsiElement(properties, element)
                }
                super.visitElement(element)
            }

            private fun addCodeVisionForChangedPsiElement(
                properties: List<DefinedConfigurationProperty>,
                element: PsiElement
            ): Boolean {
                val configurationProperty = properties.firstOrNull() ?: return true
                val key = configurationProperty.key
                val runtimeValue = runtimeValueMap[key] ?: return true
                if (runtimeValue == configurationProperty.value) return true
                val textRange = InlayHintsUtils.getTextRangeWithoutLeadingCommentsAndWhitespaces(element)
                val length = editor.document.textLength
                val adjustedRange = TextRange(min(textRange.startOffset, length), min(textRange.endOffset, length))

                val entry = ClickableTextCodeVisionEntry(
                    runtimeValue, id,
                    onClick = { _, _ ->
                        val textToEval = "explyt.Explyt.getEnvironment().getPropertySources().stream().skip(1)" +
                                ".filter(it -> it.containsProperty(\"$key\")).findFirst().orElse(null)"
                        DebugUtil.evaluate(project, textToEval)
                    },
                    AllIcons.Actions.StartDebugger,
                    runtimeValue, runtimeValue, emptyList()
                )
                lenses.add(adjustedRange to entry)
                return false
            }
        })
        return CodeVisionState.Ready(lenses)
    }

    override val name = SpringCoreBundle.message("explyt.spring.debugger.property.inlay.hints")
    override val relativeOrderings: List<CodeVisionRelativeOrdering> = emptyList()
    override val defaultAnchor = CodeVisionAnchorKind.Default
    override val id: String = ID
    override val groupId = ID

    private fun evaluatePropertyRuntimeValue(
        currentSession: XDebugSession,
        currentSourcePosition: XSourcePosition,
        propertyName: String,
        countDownLatch: CountDownLatch,
        runtimeValueMap: MutableMap<String, String>
    ) {
        currentSession.debugProcess.evaluator?.evaluate(
            "explyt.Explyt.getEnvironment().getProperty(\"$propertyName\")",
            object : XDebuggerEvaluator.XEvaluationCallback {
                override fun evaluated(result: XValue) {
                    countDownLatch.countDown()
                    ProgressManager.checkCanceled()
                    val runtimeValue = (result as? JavaValue)?.descriptor?.value ?: return
                    val runtimeStringValue = (runtimeValue as? StringReference)?.value() ?: runtimeValue.toString()
                    runtimeValueMap[propertyName] = runtimeStringValue
                }

                override fun errorOccurred(errorMessage: @NlsContexts.DialogMessage String) {
                    countDownLatch.countDown()
                }
            },
            currentSourcePosition
        )
    }

    companion object {
        const val ID = "explyt.spring.debug.code.vision.property.value"
    }
}

class PropertyDebugValueCodeVisionGroupSettingProvider() : CodeVisionGroupSettingProvider {
    override val groupId: String = PropertyDebugValueCodeVisionProvider.ID
    override val description = "Show runtime Spring property values in Debug mode"
    override val groupName = "Explyt Property Runtime Value"
}