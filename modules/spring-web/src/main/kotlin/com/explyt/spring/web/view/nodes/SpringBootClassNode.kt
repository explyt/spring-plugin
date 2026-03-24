/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.nodes

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.web.view.EndpointElementViewData
import com.intellij.pom.Navigatable
import com.intellij.ui.treeStructure.CachingSimpleNode
import com.intellij.ui.treeStructure.SimpleNode


class SpringBootClassNode(
    private val classOrFileName: String,
    val viewData: EndpointElementViewData?,
    rootNode: EndpointTypeNode
) : CachingSimpleNode(rootNode), EndpointNavigable {
    init {
        presentation.setIcon(SpringIcons.SpringBoot)
    }

    override fun getName() = classOrFileName

    override fun buildChildren() = emptyArray<SimpleNode>()

    override fun navigate() {
        (viewData?.psiPointer?.element?.navigationElement as? Navigatable)?.navigate(true)
    }
}