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
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
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
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.frame.*
import com.sun.jdi.*
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference


class SpringDebuggerContextRenderer : ExtraDebugNodesProvider {

    override fun addExtraNodes(evaluationContext: EvaluationContext, children: XValueChildrenList) {
        if (!SpringToolRunConfigurationsSettingsState.getInstance().isDebugMode) return
        val project = evaluationContext.project ?: return
        val debugProcess = evaluationContext.debugProcess as? DebugProcessImpl ?: return
        val nodeManagerImpl = debugProcess.xdebugProcess?.nodeManager ?: return
        val suspendContext = evaluationContext.suspendContext as? SuspendContextImpl ?: return
        val evaluationContextImpl = evaluationContext as? EvaluationContextImpl ?: return
        if (!debugProcess.isEvaluationPossible(suspendContext)) return

        val explytContextClass = getExplytContextInstance(debugProcess, evaluationContext) ?: return
        val springContext = getSpringContextRef(evaluationContext, explytContextClass) ?: return
        val transactionData = getTransactionManagerData(debugProcess, evaluationContext)

        val debugContextDescriptor = DebugSpringContextDescriptor(project, springContext)
        val value = JavaValue.create(null, debugContextDescriptor, evaluationContextImpl, nodeManagerImpl, false)
        children.add(0, value)
        transactionData?.let {
            val valueTxStatus = getTransactionStatus(debugProcess, evaluationContextImpl)?.let {
                val descriptor = TransactionStatusDescriptor(project, it)
                JavaValue.create(null, descriptor, evaluationContextImpl, nodeManagerImpl, false)
            }

            children.add(1, object : XNamedValue("Explyt: Active Transaction") {
                override fun canNavigateToSource(): Boolean = false
                override fun computePresentation(node: XValueNode, place: XValuePlace) {
                    node.setPresentation(AllIcons.Actions.IntentionBulb, null, transactionData, valueTxStatus != null)
                }

                override fun computeChildren(node: XCompositeNode) {
                    if (valueTxStatus == null) return super.computeChildren(node)
                    node.addChildren(XValueChildrenList.singleton(valueTxStatus), false)
                    node.addChildren(XValueChildrenList.EMPTY, true)
                }
            })
        }

        syncDebugBeanToolWindow(project, evaluationContext, debugProcess, explytContextClass)
    }

    private fun syncDebugBeanToolWindow(
        project: Project,
        evaluationContext: EvaluationContextImpl,
        debugProcess: DebugProcessImpl,
        explytContextClass: ClassType
    ) {
        val applicationAddress = debugProcess.connection.applicationAddress?.takeIf { it.isNotEmpty() } ?: return
        if (!isNeedSync(applicationAddress)) return
        val runConfigurationId = getRunConfigurationId(evaluationContext, explytContextClass) ?: return
        val rawBeanData = getRawBeanData(evaluationContext, explytContextClass)?.takeIf { it.isNotEmpty() } ?: return
        ApplicationManager.getApplication().runReadAction {
            AttachSpringBootProjectAction.attachDebugProject(project, rawBeanData, runConfigurationId)
        }
    }

    private fun isNeedSync(applicationAddress: String): Boolean {
        val state = DebugToolWindowCache.state.get()
        if (state == null) {
            DebugToolWindowCache.state.set(DebugProcessState(applicationAddress, LocalDateTime.now()))
            return true
        }
        if (state.addressId != applicationAddress) {
            DebugToolWindowCache.state.set(DebugProcessState(applicationAddress, LocalDateTime.now()))
            return true
        }
        val nowTime = LocalDateTime.now()
        if (state.time.plusMinutes(5) < nowTime) {
            DebugToolWindowCache.state.set(DebugProcessState(applicationAddress, LocalDateTime.now()))
            return true
        }
        return false
    }

    private fun getSpringContextRef(
        evaluationContext: EvaluationContextImpl, explytContextClass: ClassType
    ): ObjectReference? {
        return try {
            evaluationContext.computeAndKeep {
                DebuggerUtilsImpl.invokeClassMethod(
                    evaluationContext,
                    explytContextClass,
                    "getContext",
                    null,
                    emptyList()
                ) as? ObjectReference
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getRawBeanData(
        evaluationContext: EvaluationContextImpl, explytContextClass: ClassType
    ): String? {
        return try {
            (DebuggerUtilsImpl.invokeClassMethod(
                evaluationContext, explytContextClass, "getRawBeanData", null, emptyList()
            ) as? StringReference)?.value()
        } catch (_: Exception) {
            null
        }
    }

    private fun getRunConfigurationId(
        evaluationContext: EvaluationContextImpl, explytContextClass: ClassType
    ): String? {
        return try {
            (evaluationContext.computeAndKeep {
                DebuggerUtilsImpl.invokeClassMethod(
                    evaluationContext, explytContextClass, "getConfigurationId", null, emptyList()
                ) as? StringReference
            })?.value()
        } catch (_: Exception) {
            null
        }
    }

    private fun getExplytContextInstance(
        debugProcess: DebugProcessImpl,
        evaluationContext: EvaluationContextImpl
    ): ClassType? = try {
        debugProcess.findLoadedClass(
            evaluationContext,
            "com.explyt.spring.boot.bean.reader.InternalHolderContext",
            evaluationContext.classLoader
        )
    } catch (_: EvaluateException) {
        null
    } as? ClassType

    private fun getTransactionManagerData(
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
            ) as? ObjectReference)?.let { mapToLevelString(it) } ?: "DEFAULT"

            "Isolation=$level, ReadOnly=$isReadOnly"
        } catch (_: Exception) {
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

    private fun getTransactionStatus(
        debugProcess: DebugProcessImpl,
        evaluationContext: EvaluationContextImpl
    ): ObjectReference? {
        return try {
            val txManager = "org.springframework.transaction.interceptor.TransactionAspectSupport"
            val txReferenceType = debugProcess.findLoadedClass(
                evaluationContext, txManager, evaluationContext.classLoader
            ) as? ClassType ?: return null

            return evaluationContext.computeAndKeep {
                DebuggerUtilsImpl.invokeClassMethod(
                    evaluationContext,
                    txReferenceType,
                    "currentTransactionStatus",
                    null,
                    emptyList()
                ) as? ObjectReference
            }
        } catch (_: Exception) {
            null
        }
    }
}

private class DebugSpringContextDescriptor(project: Project, value: Value) : ValueDescriptorImpl(project, value) {
    override fun calcValue(contextImpl: EvaluationContextImpl?): Value = value

    override fun getDescriptorEvaluation(context: DebuggerContext?) = null

    override fun calcValueName() = "Explyt: Spring Context"
}

private class TransactionStatusDescriptor(project: Project, value: Value) : ValueDescriptorImpl(project, value) {
    override fun calcValue(contextImpl: EvaluationContextImpl?): Value = value

    override fun getDescriptorEvaluation(context: DebuggerContext?) = null

    override fun calcValueName() = "Transaction status"
}

private object DebugToolWindowCache {
    var state: AtomicReference<DebugProcessState> = AtomicReference(null)
}

private data class DebugProcessState(val addressId: String, val time: LocalDateTime)