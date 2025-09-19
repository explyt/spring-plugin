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

package com.explyt.spring.core.hint

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.intellij.codeInsight.hints.declarative.*
import com.intellij.debugger.engine.JavaValue
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.endOffset
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XValue
import com.sun.jdi.StringReference
import fleet.multiplatform.shims.ConcurrentHashMap
import org.jetbrains.yaml.psi.YAMLFile
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PropertiesDebugValueHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        if (file !is PropertiesFile) return null
        return getCollector(file)
    }
}

class YamlDebugValueHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        if (file !is YAMLFile) return null
        return getCollector(file)
    }
}

private class Collector(
    val propertiesMapByPsiElement: Map<PsiElement, List<DefinedConfigurationProperty>>,
    val runtimeValueMap: Map<String, String>
) : SharedBypassCollector {

    override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
        val properties = propertiesMapByPsiElement[element] ?: return
        if (properties.size != 1) return
        val configurationProperty = properties.first()
        configurationProperty.value?.takeIf { it.isNotEmpty() } ?: return
        val runtimeValue = runtimeValueMap[configurationProperty.key] ?: return
        if (runtimeValue == configurationProperty.value) return
        sink.addPresentation(
            InlineInlayPosition(element.endOffset, true), hintFormat = HintFormat.default
        ) {
            text("  Runtime value: $runtimeValue")
        }
    }
}

private fun getCollector(file: PsiFile): Collector? {
    val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return null
    val project = file.project
    val currentSession = XDebuggerManager.getInstance(project).currentSession ?: return null
    val propertiesMapByPsiElement = DefinedConfigurationPropertiesSearch.getInstance(project)
        .getAllProperties(module).asSequence()
        .filter { it.sourceFile == file.name }
        .filter { it.psiElement?.containingFile == file }
        .groupBy { it.psiElement!! }
    if (ApplicationManager.getApplication().isDispatchThread) return null

    val xSourcePosition = currentSession.currentStackFrame?.sourcePosition ?: return null
    val countDownLatch = CountDownLatch(propertiesMapByPsiElement.size)
    val runtimeValueMap = ConcurrentHashMap<String, String>()
    val currentTimeMillis = System.currentTimeMillis()
    propertiesMapByPsiElement.values.flatMap { it }.forEach {
        evaluatePropertyRuntimeValue(currentSession, xSourcePosition, it.key, countDownLatch, runtimeValueMap)
    }
    countDownLatch.await(3, TimeUnit.SECONDS)
    println("!!!!!!!!!!!111111111111111111 - " + (System.currentTimeMillis() - currentTimeMillis))
    return Collector(propertiesMapByPsiElement, runtimeValueMap)
}

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
                runtimeValueMap[propertyName] = (runtimeValue as? StringReference)?.value() ?: runtimeValue.toString()
            }

            override fun errorOccurred(errorMessage: @NlsContexts.DialogMessage String) {
                countDownLatch.countDown()
                ProgressManager.checkCanceled()
            }
        },
        currentSourcePosition
    )
}
