/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.view.nodes.profile

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.externalsystem.model.BeanSearch
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.externalSystem.view.ExternalProjectsView
import com.intellij.openapi.externalSystem.view.ExternalSystemNode

@Order(6)
class BeanSearchNode(externalProjectsView: ExternalProjectsView, dataNode: DataNode<BeanSearch>) :
    ExternalSystemNode<BeanSearch>(externalProjectsView, null, dataNode) {

    override fun update(presentation: PresentationData) {
        super.update(presentation)
        val enabled = data?.enabled ?: false
        if (enabled) {
            presentation.setIcon(AllIcons.Diff.GutterCheckBoxSelected)
        } else {
            presentation.setIcon(AllIcons.Diff.GutterCheckBox)
        }
    }

    override fun getName(): String {
        return SpringCoreBundle.message("explyt.external.view.node.bean.search")
    }

    override fun getActionId() = "Explyt.ExternalView.EnableDiChanged"

    override fun isAlwaysLeaf() = true

}