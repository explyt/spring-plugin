/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.externalsystem.view.nodes.SpringBeanViewNode
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.ExternalSystemAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys

class BeanNavigationAction : ExternalSystemAction() {
    override fun actionPerformed(e: AnActionEvent) {
        StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_BOOT_PANEL_BEAN_NAVIGATION)
        val project = e.project ?: return
        val data = e.getData(ExternalSystemDataKeys.SELECTED_NODES)
        if (data?.size != 1) return
        val externalSystemNode = data[0] as? SpringBeanViewNode ?: return
        val beanData = externalSystemNode.dataNode.data

        val psiClass = NativeBootUtils.getPsiClassLocation(project, beanData) ?: return

        val psiElement = beanData.methodName
            ?.let { psiClass.findMethodsByName(beanData.methodName, false).firstOrNull() } ?: psiClass
        psiElement.navigate(true)
    }
}