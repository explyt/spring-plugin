/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.nodes

import com.explyt.spring.web.view.EndpointViewByType
import com.intellij.ui.treeStructure.CachingSimpleNode
import com.intellij.ui.treeStructure.SimpleNode

class RootEndpointNode(val list: List<EndpointViewByType>) : CachingSimpleNode(null) {

    override fun getName() = "root node"

    override fun buildChildren(): Array<SimpleNode> {
        return list.map { EndpointTypeNode(it.type, it.list, this) }.toTypedArray()
    }
}