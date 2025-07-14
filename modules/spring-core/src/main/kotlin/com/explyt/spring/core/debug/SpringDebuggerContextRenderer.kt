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

package com.explyt.spring.core.debug

import com.explyt.spring.core.externalsystem.action.AttachSpringBootProjectAction
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerUtilsImpl
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.debugger.ui.tree.ExtraDebugNodesProvider
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import com.jetbrains.rd.util.concurrentMapOf
import com.sun.jdi.*
import java.time.LocalDateTime


class SpringDebuggerContextRenderer : ExtraDebugNodesProvider {
    val debugMap = concurrentMapOf<String, LocalDateTime>()

    override fun addExtraNodes(evaluationContext: EvaluationContext, children: XValueChildrenList) {
        val project = evaluationContext.project ?: return
        val debugProcess = evaluationContext.debugProcess as? DebugProcessImpl ?: return
        val nodeManagerImpl = debugProcess.xdebugProcess?.nodeManager ?: return
        val suspendContext = evaluationContext.suspendContext as? SuspendContextImpl ?: return
        val evaluationContextImpl = evaluationContext as? EvaluationContextImpl ?: return
        if (!debugProcess.isEvaluationPossible(suspendContext)) return

        val applicationAddress = debugProcess.connection.applicationAddress ?: return
        val explytContextClass = getExplytContextInstance(debugProcess, evaluationContext) ?: return
        val springContext = getSpringContextRef(evaluationContext, explytContextClass) ?: return
        val runConfigurationId = getRunConfigurationId(evaluationContext, explytContextClass)
        val transactionData = getTransactionData(debugProcess, evaluationContext)

        val debugContextDescriptor = DebugSpringContextDescriptor(project, springContext)
        val value = JavaValue.create(null, debugContextDescriptor, evaluationContextImpl, nodeManagerImpl, false)
        children.add(0, value)
        if (transactionData != null) {
            children.add(1, object : XNamedValue("Explyt: Active Transaction") {
                override fun canNavigateToSource(): Boolean = false
                override fun computePresentation(node: XValueNode, place: XValuePlace) {
                    node.setPresentation(AllIcons.Actions.IntentionBulb, null, transactionData, false)
                }
            })
        }

        syncBeanToolWindow(project, applicationAddress, runConfigurationId)
    }

    private fun syncBeanToolWindow(project: Project, applicationAddress: String, runConfigurationId: String?) {
        if (applicationAddress.isEmpty() || runConfigurationId == null) return
        if (!isNeedSync(applicationAddress)) return
        val runConfiguration = runReadAction {
            RunManager.getInstance(project).allConfigurationsList
                .asSequence()
                .filterIsInstance<RunConfigurationBase<*>>()
                .find { NativeBootUtils.getConfigurationId(it) == runConfigurationId }
        } ?: return
        ApplicationManager.getApplication().runReadAction {
            AttachSpringBootProjectAction.attachProject(project, runConfiguration)
        }
    }

    private fun isNeedSync(applicationAddress: String): Boolean {
        val time = debugMap[applicationAddress]
        if (time == null) {
            debugMap.put(applicationAddress, LocalDateTime.now())
            return true
        }
        val nowTime = LocalDateTime.now()
        if (time.plusMinutes(10) < nowTime) {
            debugMap.put(applicationAddress, LocalDateTime.now())
            return true
        }
        return false
    }

    private fun getSpringContextRef(
        evaluationContext: EvaluationContextImpl, explytContextClass: ClassType
    ): ObjectReference? = evaluationContext.computeAndKeep {
        DebuggerUtilsImpl.invokeClassMethod(
            evaluationContext,
            explytContextClass,
            "getContext",
            null,
            emptyList()
        ) as? ObjectReference
    }

    private fun getRunConfigurationId(
        evaluationContext: EvaluationContextImpl, explytContextClass: ClassType
    ): String? {
        return (evaluationContext.computeAndKeep {
            DebuggerUtilsImpl.invokeClassMethod(
                evaluationContext, explytContextClass, "getConfigurationId", null, emptyList()
            ) as? StringReference
        })?.value()
    }

    private fun getExplytContextInstance(
        debugProcess: DebugProcessImpl,
        evaluationContext: EvaluationContextImpl
    ): ClassType? = try {
        debugProcess.findLoadedClass(evaluationContext, EXPLYT_SPRING_CONTEXT_CLASS, evaluationContext.classLoader)
    } catch (_: EvaluateException) {
        null
    } as? ClassType

    private fun getTransactionData(
        debugProcess: DebugProcessImpl,
        evaluationContext: EvaluationContextImpl
    ): String? {
        return try {
            val txManager = "org.springframework.transaction.support.TransactionSynchronizationManager"
            val txReferenceType = debugProcess.findLoadedClass(
                evaluationContext, txManager, evaluationContext.classLoader
            ) as? ClassType ?: return null
            val isActive = (DebuggerUtilsImpl.invokeClassMethod(
                evaluationContext,
                txReferenceType,
                "isActualTransactionActive",
                null,
                emptyList()
            ) as? BooleanValue)?.value() ?: false
            if (!isActive) return null
            val isReadOnly = (DebuggerUtilsImpl.invokeClassMethod(
                evaluationContext,
                txReferenceType,
                "isCurrentTransactionReadOnly",
                null,
                emptyList()
            ) as? BooleanValue)?.value() ?: false

            val level = (DebuggerUtilsImpl.invokeClassMethod(
                evaluationContext,
                txReferenceType,
                "getCurrentTransactionIsolationLevel",
                null,
                emptyList()
            ) as? ObjectReference)?.let { mapToLevelString(it) } ?: ""

            "Isolation=$level, ReadOnly=$isReadOnly"
        } catch (_: EvaluateException) {
            null
        }
    }

    private fun mapToLevelString(level: ObjectReference?): String {
        level ?: return ""
        val field = level.referenceType().fields().firstOrNull { it.name() == "value" } ?: return ""
        val levelInt = (level.getValue(field) as IntegerValue).value()
        return when (levelInt) {
            1 -> "READ_UNCOMMITTED"
            2 -> "READ_COMMITTED"
            4 -> "REPEATABLE_READ"
            8 -> "SERIALIZABLE"
            else -> "DEFAULT"
        }
    }
}

private class DebugSpringContextDescriptor(project: Project, value: Value) : ValueDescriptorImpl(project, value) {
    override fun calcValue(contextImpl: EvaluationContextImpl?): Value = value

    override fun getDescriptorEvaluation(context: DebuggerContext?) = null

    override fun calcValueName() = "Explyt: Spring Context"
}