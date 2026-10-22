/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.externalsystem.view.nodes.SpringBeanViewNode
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.ide.util.EditSourceUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.externalSystem.action.ExternalSystemAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable

class BeanNavigationAction : ExternalSystemAction() {
    override fun actionPerformed(e: AnActionEvent) {
        StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_BOOT_PANEL_BEAN_NAVIGATION)
        val project = e.project ?: return
        val data = e.getData(ExternalSystemDataKeys.SELECTED_NODES)
        if (data?.size != 1) return
        val externalSystemNode = data[0] as? SpringBeanViewNode ?: return
        val beanData = externalSystemNode.dataNode.data

        ReadAction.nonBlocking(Callable { findNavigatable(project, beanData) })
            .inSmartMode(project)
            .expireWhen { project.isDisposed }
            .finishOnUiThread(ModalityState.nonModal()) { it?.navigate(true) }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun findNavigatable(project: Project, beanData: SpringBeanData): Navigatable? {
        val psiClass = NativeBootUtils.getPsiClassLocation(project, beanData) ?: return null
        val psiElement = beanData.methodName
            ?.let { psiClass.findMethodsByName(it, false).firstOrNull() } ?: psiClass
        return EditSourceUtil.getDescriptor(psiElement)
    }
}
