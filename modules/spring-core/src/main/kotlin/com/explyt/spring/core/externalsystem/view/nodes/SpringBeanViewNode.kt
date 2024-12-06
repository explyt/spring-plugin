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