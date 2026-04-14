/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.nodes

import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.view.EndpointViewWithContainerName
import com.intellij.icons.AllIcons
import com.intellij.ui.treeStructure.CachingSimpleNode
import com.intellij.ui.treeStructure.SimpleNode

class EndpointTypeNode(
    private val type: EndpointType,
    private val elements: List<EndpointViewWithContainerName>,
    rootNode: RootEndpointNode
) : CachingSimpleNode(rootNode) {

    init {
        presentation.setIcon(AllIcons.Nodes.ConfigFolder)
    }

    override fun getName() = type.readable

    override fun buildChildren(): Array<SimpleNode> {
        return if (type == EndpointType.SPRING_BOOT) {
            elements.map { SpringBootClassNode(it.classOrFileName, it.list.firstOrNull(), this) }
        } else {
            elements.map { EndpointFileNode(type, it.classOrFileName, it.list, this) }
        }.toTypedArray()
    }
}