/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.nodes

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebIcons
import com.explyt.spring.web.loader.EndpointExposure
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.view.EndpointElementViewData
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.pom.Navigatable
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.CachingSimpleNode
import com.intellij.ui.treeStructure.SimpleNode

class HttpMethodNode(
    val httpElement: EndpointElementViewData, rootNode: CachingSimpleNode
) : CachingSimpleNode(rootNode), EndpointNavigable {

    init {
        when (httpElement.method.uppercase()) {
            "CONNECT" -> presentation.setIcon(SpringWebIcons.HttpConnect)
            "DELETE" -> presentation.setIcon(SpringWebIcons.HttpDelete)
            "GET" -> presentation.setIcon(SpringWebIcons.HttpGet)
            "HEAD" -> presentation.setIcon(SpringWebIcons.HttpHead)
            "OPTIONS" -> presentation.setIcon(SpringWebIcons.HttpOptions)
            "PATCH" -> presentation.setIcon(SpringWebIcons.HttpPatch)
            "PUT" -> presentation.setIcon(SpringWebIcons.HttpPut)
            "POST" -> presentation.setIcon(SpringWebIcons.HttpPost)
            "TRACE" -> presentation.setIcon(SpringWebIcons.HttpTrace)

            EndpointType.MESSAGE_BROKER.name -> presentation.setIcon(AllIcons.Webreferences.MessageQueue)
            EndpointType.EVENT_LISTENERS.name -> presentation.setIcon(SpringIcons.EventListener)

            else -> presentation.setIcon(AllIcons.General.Web)
        }
    }

    override fun getName() = httpElement.path

    /** An Actuator endpoint the configuration does not publish over HTTP stays listed, but reads as such. */
    override fun doUpdate(presentation: PresentationData) {
        val note = when (httpElement.exposure) {
            EndpointExposure.NOT_EXPOSED -> "explyt.web.endpoints.tool.actuator.not.exposed"
            EndpointExposure.UNKNOWN -> "explyt.web.endpoints.tool.actuator.exposure.unknown"
            EndpointExposure.EXPOSED, null -> return
        }
        presentation.clearText()
        presentation.addText(name, SimpleTextAttributes.GRAYED_ATTRIBUTES)
        presentation.addText("  ${SpringWebBundle.message(note)}", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
    }

    override fun buildChildren() = emptyArray<SimpleNode>()

    override fun navigate() {
        (httpElement.psiPointer.element?.navigationElement as? Navigatable)?.navigate(true)
    }
}
