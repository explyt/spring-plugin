/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view

import com.explyt.spring.web.view.nodes.RootEndpointNode
import com.intellij.ui.treeStructure.SimpleTreeStructure

class EndpointTreeStructure(private val rootEndpointNode: RootEndpointNode) : SimpleTreeStructure() {
    override fun getRootElement() = rootEndpointNode
}