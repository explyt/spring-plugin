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

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.externalsystem.action.AttachSpringBootProjectAction
import com.explyt.spring.core.hint.PropertyDebugValueCodeVisionProvider
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.intellij.codeInsight.codeVision.CodeVisionHost
import com.intellij.codeInsight.codeVision.CodeVisionHost.LensInvalidateSignal
import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerUtilsImpl
import com.intellij.debugger.ui.impl.watch.NodeManagerImpl
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.debugger.ui.tree.ExtraDebugNodesProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import com.intellij.xdebugger.impl.evaluate.XDebuggerEvaluationDialog
import com.sun.jdi.*
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference


private const val INTERNAL_HOLDER_CLASS = "com.explyt.spring.boot.bean.reader.InternalHolderContext"

private const val springContextDataLabel = "Explyt Spring Context Data"

class SpringDebuggerContextRenderer : ExtraDebugNodesProvider {

    override fun addExtraNodes(evaluationContext: EvaluationContext, children: XValueChildrenList) {
        if (!SpringToolRunConfigurationsSettingsState.getInstance().isDebugMode) return
        val project = evaluationContext.project ?: return
        val debugProcess = evaluationContext.debugProcess as? DebugProcessImpl ?: return
        val nodeManagerImpl = debugProcess.xdebugProcess?.nodeManager ?: return
        val suspendContext = evaluationContext.suspendContext as? SuspendContextImpl ?: return
        val evaluationContextImpl = evaluationContext as? EvaluationContextImpl ?: return
        if (!debugProcess.isEvaluationPossible(suspendContext)) return

        val explytContextClass = loadClass(debugProcess, evaluationContext, INTERNAL_HOLDER_CLASS) ?: return

        invalidatePropertyCodeVisionHint(project)

        addContextNode(evaluationContextImpl, nodeManagerImpl, explytContextClass, children, debugProcess)
        addTransactionNode(debugProcess, evaluationContextImpl, nodeManagerImpl, children)

        syncDebugBeanToolWindow(project, evaluationContext, debugProcess, explytContextClass)
    }

    private fun addContextNode(
        evaluationContext: EvaluationContextImpl,
        nodeManagerImpl: NodeManagerImpl,
        explytContextClass: ClassType,
        children: XValueChildrenList,
        debugProcess: DebugProcessImpl
    ) {
        val project = evaluationContext.project
        val springContexts = invokeClassMethod(evaluationContext, explytContextClass, "getContexts")
        val contextsSizeRef = springContexts?.let { invokeObjectMethod(evaluationContext, it, "size") }
        val contextsSize = (contextsSizeRef as? IntegerValue)?.value() ?: 0
        if (contextsSize > 1) {
            val debugContextDescriptor = DebugSpringContextDescriptor(project, springContexts!!, "Contexts")
            val value = JavaValue.create(null, debugContextDescriptor, evaluationContext, nodeManagerImpl, false)
            children.add(0, object : XNamedValue(springContextDataLabel) {
                override fun canNavigateToSource(): Boolean = false
                override fun computePresentation(node: XValueNode, place: XValuePlace) {
                    node.setPresentation(
                        SpringIcons.SpringBoot, null, SpringCoreBundle.message("explyt.spring.debugger.root.node"), true
                    )
                    node.setFullValueEvaluator(object : JavaValue.JavaFullValueEvaluator(
                        SpringCoreBundle.message("explyt.spring.debugger.show.dialog"), evaluationContext
                    ) {
                        override fun isShowValuePopup() = false
                        override fun evaluate(callback: XFullValueEvaluationCallback) {
                            if (callback.isObsolete) return
                            callback.evaluated("")
                            val debugSession = debugProcess.xdebugProcess?.session ?: return
                            val editorsProvider = debugProcess.xdebugProcess?.editorsProvider ?: return
                            val fromText = XExpressionImpl.fromText("explyt.Explyt.contexts", EvaluationMode.EXPRESSION)
                            ApplicationManager.getApplication().invokeLater {
                                XDebuggerEvaluationDialog(debugSession, editorsProvider, fromText, null, true).show()
                            }
                        }
                    })
                }

                override fun computeChildren(node: XCompositeNode) {
                    node.addChildren(XValueChildrenList.singleton(value), true)
                }
            })
            return
        }
        val springContext = invokeClassMethod(evaluationContext, explytContextClass, "getContext") ?: return
        val debugContextDescriptor = DebugSpringContextDescriptor(project, springContext, "Context")
        val value = JavaValue.create(null, debugContextDescriptor, evaluationContext, nodeManagerImpl, false)
        val beanFactoryValue = invokeClassMethod(evaluationContext, explytContextClass, "getBeanFactory")?.let {
            val valueDescriptor = DebugSpringContextDescriptor(project, it, "BeanFactory")
            JavaValue.create(null, valueDescriptor, evaluationContext, nodeManagerImpl, false)
        }
        val environmentValue = invokeClassMethod(evaluationContext, explytContextClass, "getEnvironment")?.let {
            val valueDescriptor = DebugSpringContextDescriptor(project, it, "Environment")
            JavaValue.create(null, valueDescriptor, evaluationContext, nodeManagerImpl, false)
        }
        children.add(0, object : XNamedValue(springContextDataLabel) {
            override fun canNavigateToSource(): Boolean = false
            override fun computePresentation(node: XValueNode, place: XValuePlace) {
                node.setPresentation(
                    SpringIcons.SpringBoot, null, SpringCoreBundle.message("explyt.spring.debugger.root.node"), true
                )
                node.setFullValueEvaluator(object : JavaValue.JavaFullValueEvaluator(
                    SpringCoreBundle.message("explyt.spring.debugger.show.dialog"), evaluationContext
                ) {
                    override fun isShowValuePopup() = false
                    override fun evaluate(callback: XFullValueEvaluationCallback) {
                        if (callback.isObsolete) return
                        callback.evaluated("")
                        val debugSession = debugProcess.xdebugProcess?.session ?: return
                        val editorsProvider = debugProcess.xdebugProcess?.editorsProvider ?: return
                        val fromText = XExpressionImpl.fromText("explyt.Explyt.context", EvaluationMode.EXPRESSION)
                        ApplicationManager.getApplication().invokeLater {
                            XDebuggerEvaluationDialog(debugSession, editorsProvider, fromText, null, true).show()
                        }
                    }
                })
            }

            override fun computeChildren(node: XCompositeNode) {
                node.addChildren(XValueChildrenList.singleton(value), false)
                if (beanFactoryValue != null) node.addChildren(XValueChildrenList.singleton(beanFactoryValue), false)
                if (environmentValue != null) node.addChildren(XValueChildrenList.singleton(environmentValue), false)
                node.addChildren(XValueChildrenList.EMPTY, true)
            }
        })
    }

    private fun addTransactionNode(
        debugProcess: DebugProcessImpl,
        evaluationContext: EvaluationContextImpl,
        nodeManagerImpl: NodeManagerImpl,
        children: XValueChildrenList
    ) {
        val transactionData = getTransactionManagerData(debugProcess, evaluationContext)
        transactionData?.let {
            val valueTxStatus = getTransactionStatus(debugProcess, evaluationContext)?.let {
                val descriptor = TransactionStatusDescriptor(evaluationContext.project, it)
                JavaValue.create(null, descriptor, evaluationContext, nodeManagerImpl, false)
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
    }

    private fun invalidatePropertyCodeVisionHint(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            project.service<CodeVisionHost>().invalidateProvider(
                LensInvalidateSignal(null, listOf(PropertyDebugValueCodeVisionProvider.ID))
            )
        }
    }

    private fun syncDebugBeanToolWindow(
        project: Project,
        evaluationContext: EvaluationContextImpl,
        debugProcess: DebugProcessImpl,
        explytContextClass: ClassType
    ) {
        invalidatePropertyCodeVisionHint(project)
        val applicationAddress = debugProcess.connection.applicationAddress?.takeIf { it.isNotEmpty() } ?: return
        if (!isNeedSync(applicationAddress)) return
        val rawBeanData = getRawBeanData(evaluationContext, explytContextClass)?.takeIf { it.isNotEmpty() } ?: return
        ApplicationManager.getApplication().runReadAction {
            val sessionName = debugProcess.session.sessionName
            AttachSpringBootProjectAction.attachDebugProject(project, rawBeanData, sessionName)
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

    private fun invokeClassMethod(
        evaluationContext: EvaluationContextImpl, explytContextClass: ClassType, methodName: String
    ): ObjectReference? {
        return try {
            evaluationContext.computeAndKeep {
                DebuggerUtilsImpl.invokeClassMethod(
                    evaluationContext,
                    explytContextClass,
                    methodName,
                    null,
                ) as? ObjectReference
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun invokeObjectMethod(
        evaluationContext: EvaluationContextImpl, objectReference: ObjectReference, methodName: String
    ): Value? {
        return try {
            evaluationContext.computeAndKeep {
                DebuggerUtilsImpl.invokeObjectMethod(
                    evaluationContext,
                    objectReference,
                    methodName,
                    null,
                    emptyList()
                )
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
                evaluationContext, explytContextClass, "getRawBeanData", null,
            ) as? StringReference)?.value()
        } catch (_: Exception) {
            null
        }
    }

    private fun loadClass(
        debugProcess: DebugProcessImpl,
        evaluationContext: EvaluationContextImpl,
        className: String
    ): ClassType? = try {
        val classLoader = evaluationContext.classLoader
        debugProcess.findLoadedClass(evaluationContext, className, classLoader)
            ?: debugProcess.loadClass(evaluationContext, className, classLoader)
    } catch (_: EvaluateException) {
        null
    } as? ClassType

    private fun getTransactionManagerData(
        debugProcess: DebugProcessImpl,
        evaluationContext: EvaluationContextImpl
    ): String? {
        return try {
            val txManagerClass = "org.springframework.transaction.support.TransactionSynchronizationManager"
            val txReferenceType = loadClass(debugProcess, evaluationContext, txManagerClass) ?: return null
            val isActive = (DebuggerUtilsImpl.invokeClassMethod(
                evaluationContext,
                txReferenceType,
                "isActualTransactionActive",
                null,
            ) as? BooleanValue)?.value() ?: false
            if (!isActive) return null
            val isReadOnly = (DebuggerUtilsImpl.invokeClassMethod(
                evaluationContext,
                txReferenceType,
                "isCurrentTransactionReadOnly",
                null,
            ) as? BooleanValue)?.value() ?: false

            val level = (DebuggerUtilsImpl.invokeClassMethod(
                evaluationContext,
                txReferenceType,
                "getCurrentTransactionIsolationLevel",
                null,
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
            val txAspectSupportClass = "org.springframework.transaction.interceptor.TransactionAspectSupport"
            val txReferenceType = loadClass(debugProcess, evaluationContext, txAspectSupportClass) ?: return null

            return evaluationContext.computeAndKeep {
                DebuggerUtilsImpl.invokeClassMethod(
                    evaluationContext,
                    txReferenceType,
                    "currentTransactionInfo",
                    null,
                ) as? ObjectReference
            }
        } catch (_: Exception) {
            null
        }
    }
}

private class DebugSpringContextDescriptor(project: Project, value: Value, val valueName: String) :
    ValueDescriptorImpl(project, value) {
    override fun calcValue(contextImpl: EvaluationContextImpl?): Value = value

    override fun getDescriptorEvaluation(context: DebuggerContext?) = null

    override fun calcValueName() = valueName
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