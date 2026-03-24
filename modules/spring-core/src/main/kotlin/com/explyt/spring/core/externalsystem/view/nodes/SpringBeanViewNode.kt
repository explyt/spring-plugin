/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.view.nodes

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.externalSystem.view.ExternalProjectsView
import com.intellij.openapi.externalSystem.view.ExternalSystemNode

@Order(5)
class SpringBeanViewNode(externalProjectsView: ExternalProjectsView, val dataNode: DataNode<SpringBeanData>) :
    ExternalSystemNode<SpringBeanData>(externalProjectsView, null, dataNode) {

    override fun update(presentation: PresentationData) {
        super.update(presentation)
        presentation.setIcon(SpringIcons.SpringBean)
        val data = dataNode.data
        presentation.tooltip = """
                name: ${data.beanName}<br>
                scope: ${data.scope}<br>
                class: ${data.className}            
        """.trimIndent()
    }

    override fun getName() = getBeanViewName()

    override fun getActionId() = "NavigateToSpringBean"

    private fun getBeanViewName(): String {
        val beanName = dataNode.data.beanName
        if (beanName.contains(".")) return beanName.substringAfterLast(".")
        return beanName
    }

    override fun isAlwaysLeaf() = true
}