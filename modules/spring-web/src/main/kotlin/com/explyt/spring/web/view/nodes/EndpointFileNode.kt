/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.nodes

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.web.SpringWebIcons
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.view.EndpointElementViewData
import com.intellij.ui.treeStructure.CachingSimpleNode
import com.intellij.ui.treeStructure.SimpleNode

class EndpointFileNode(
    type: EndpointType,
    private val classOrFileName: String,
    private val elements: List<EndpointElementViewData>,
    rootNode: EndpointTypeNode
) : CachingSimpleNode(rootNode) {
    init {
        if (type == EndpointType.OPENAPI) {
            if (classOrFileName.endsWith(".json", true)) {
                presentation.setIcon(SpringWebIcons.OpenApiJson)
            } else {
                presentation.setIcon(SpringWebIcons.OpenApiYaml)
            }
        } else {
            presentation.setIcon(SpringIcons.SpringBean)
        }
    }

    override fun getName() = classOrFileName

    override fun buildChildren(): Array<SimpleNode> {
        return elements.map { HttpMethodNode(it, this) }.toTypedArray()
    }
}