/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.view.nodes

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.externalSystem.view.ExternalProjectsView
import com.intellij.openapi.externalSystem.view.ExternalSystemNode

@Order(20)
class LibraryBeanNodes(externalProjectsView: ExternalProjectsView, val beans: List<ExternalSystemNode<*>>) :
    ExternalSystemNode<SpringBeanData>(externalProjectsView, null, null) {

    override fun update(presentation: PresentationData) {
        super.update(presentation)
        presentation.setIcon(AllIcons.Nodes.ConfigFolder)
    }

    override fun getName() = SpringCoreBundle.message("explyt.external.view.node.library")

    override fun isVisible() = super.isVisible() && hasChildren()

    override fun doBuildChildren() = beans

}