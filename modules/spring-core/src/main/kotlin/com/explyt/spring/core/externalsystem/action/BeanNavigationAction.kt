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