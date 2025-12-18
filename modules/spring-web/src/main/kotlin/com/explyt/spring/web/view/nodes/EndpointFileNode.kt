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